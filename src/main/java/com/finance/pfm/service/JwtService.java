package com.finance.pfm.service;

import com.finance.pfm.entity.RefreshToken;
import com.finance.pfm.entity.User;
import com.finance.pfm.repository.RefreshTokenRepository;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class JwtService {

    private static final Logger LOG = Logger.getLogger(JwtService.class);

    // Access token: 15 phút
    private static final long ACCESS_TOKEN_EXPIRY_SECONDS = 15 * 60;
    // Refresh token: 7 ngày
    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 7;

    @Inject
    RefreshTokenRepository refreshTokenRepository;

    @Inject
    RedisDataSource redisDataSource;

    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "finance-manager")
    String issuer;

    @ConfigProperty(name = "smallrye.jwt.sign.key", defaultValue = "")
    String privateKeyContent;

    // Cache parsed PrivateKey để tránh parse lại mỗi request
    private volatile PrivateKey cachedPrivateKey;

    /**
     * Parse nội dung PEM thành PrivateKey object.
     * Hỗ trợ cả nội dung có xuống dòng thực (\n) lẫn escaped (\\n) từ env var.
     */
    private PrivateKey getPrivateKeyFromContent(String pemContent) {
        if (cachedPrivateKey != null) {
            return cachedPrivateKey;
        }
        try {
            String normalized = pemContent
                    .replace("\\n", "\n")          // env var thường escape \n thành \\n
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");          // xóa hết whitespace/newline

            byte[] decoded = Base64.getDecoder().decode(normalized);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            cachedPrivateKey = kf.generatePrivate(spec);
            LOG.info("Đã parse private key từ biến môi trường JWT_PRIVATE_KEY thành công");
            return cachedPrivateKey;
        } catch (Exception e) {
            LOG.errorf("Không thể parse private key từ env var: %s", e.getMessage());
            return null;
        }
    }

    /**
     * Tạo access token JWT, lưu vào Redis với TTL = 15 phút.
     */
    public String generateAccessToken(User user) {
        String role = user.role != null ? user.role.name() : "USER";

        io.smallrye.jwt.build.JwtClaimsBuilder builder = Jwt.issuer(issuer)
                .subject(String.valueOf(user.userId))
                .groups(Set.of(role))
                .claim("username", user.username)
                .claim("email", user.email != null ? user.email : "")
                .claim("fullName", user.fullName != null ? user.fullName : "")
                .claim("role", role)
                .expiresIn(ACCESS_TOKEN_EXPIRY_SECONDS);

        String token;

        // Ưu tiên 1: Parse private key từ env var JWT_PRIVATE_KEY
        if (privateKeyContent != null && !privateKeyContent.trim().isEmpty()) {
            PrivateKey pk = getPrivateKeyFromContent(privateKeyContent);
            if (pk != null) {
                token = builder.sign(pk);
            } else {
                // Fallback nếu parse env var thất bại: thử sign() mặc định
                token = builder.sign();
            }
        } else {
            // Ưu tiên 2: Dùng key location file trên classpath (dev mode)
            token = builder.sign();
        }

        // Lưu token vào Redis để có thể revoke
        try {
            ValueCommands<String, String> commands = redisDataSource.value(String.class);
            String redisKey = "jwt:access:" + user.userId + ":" + token.substring(token.length() - 20);
            commands.setex(redisKey, ACCESS_TOKEN_EXPIRY_SECONDS, "valid");
        } catch (Exception e) {
            LOG.warnf("Không thể lưu token vào Redis: %s", e.getMessage());
        }

        return token;
    }

    /**
     * Tạo refresh token ngẫu nhiên, lưu vào DB.
     */
    @Transactional
    public String generateRefreshToken(User user) {
        // Revoke tất cả refresh token cũ của user
        refreshTokenRepository.revokeAllByUserId(user.userId);

        String tokenValue = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.token = tokenValue;
        refreshToken.user = user;
        refreshToken.expiresAt = LocalDateTime.now().plusDays(REFRESH_TOKEN_EXPIRY_DAYS);
        refreshTokenRepository.persist(refreshToken);

        return tokenValue;
    }

    /**
     * Xác thực refresh token và trả về User nếu hợp lệ.
     */
    public Optional<User> validateRefreshToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .filter(RefreshToken::isValid)
                .map(rt -> rt.user);
    }

    /**
     * Revoke refresh token (logout).
     */
    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.revoked = true;
        });
    }

    /**
     * Revoke tất cả token của user (logout all devices).
     */
    @Transactional
    public void revokeAllTokens(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        // Xóa access token khỏi Redis
        try {
            redisDataSource.key().del("jwt:access:" + userId + ":*");
        } catch (Exception e) {
            LOG.warnf("Không thể xóa access token khỏi Redis: %s", e.getMessage());
        }
    }

    /**
     * Lấy userId từ JWT token đã được verify bởi Quarkus.
     */
    public Long getUserIdFromToken(JsonWebToken jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    /**
     * Lấy role từ JWT token.
     */
    public String getRoleFromToken(JsonWebToken jwt) {
        return jwt.getClaim("role");
    }
}
