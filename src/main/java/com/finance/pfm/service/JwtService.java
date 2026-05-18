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

import java.time.Duration;
import java.time.LocalDateTime;
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

    @ConfigProperty(name = "smallrye.jwt.sign.key.location", defaultValue = "privateKey.pem")
    String privateKeyLocation;

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
        if (privateKeyContent != null && !privateKeyContent.trim().isEmpty()) {
            token = builder.sign(privateKeyContent);
        } else {
            token = builder.sign(privateKeyLocation);
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
