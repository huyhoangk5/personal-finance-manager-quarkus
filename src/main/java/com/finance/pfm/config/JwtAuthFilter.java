package com.finance.pfm.config;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.Set;

/**
 * JwtAuthFilter — kiểm tra Bearer token trên các endpoint cần bảo vệ.
 * Kết nối với Redis để kiểm tra trạng thái token (Blacklist/Revocation).
 */
@Provider
@Priority(Priorities.AUTHENTICATION) // Chạy ngay sau khi SmallRye JWT verify signature
public class JwtAuthFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(JwtAuthFilter.class);

    @Inject
    JsonWebToken jwt;

    @Inject
    RedisDataSource redisDataSource;

    // Các path không cần token
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/users/login",
            "/api/users/register",
            "/api/users/google-login",
            "/api/users/facebook-login",
            "/api/users/forgot-password",
            "/api/users/reset-password",
            "/api/users/send-otp",
            "/api/users/verify-otp",
            "/api/users/refresh-token",
            "/api/users/qr-login/generate",
            "/api/users/qr-login/verify",
            "/api/users/qr-login/status",
            "/api/users/qr-code-by-email",
            "/api/users/qr-register",
            "/q/health",
            "/q/health/live",
            "/q/health/ready",
            "/q/openapi",
            "/q/swagger-ui",
            "/swagger-ui",
            "/openapi"
    );

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        String method = requestContext.getMethod();

        // Bỏ qua OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return;
        }

        // Bỏ qua public paths
        if (isPublicPath(path)) {
            return;
        }

        // Kiểm tra Authorization header
        String authHeader = requestContext.getHeaderString("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            LOG.debugf("Thiếu Bearer token: path=%s", path);
            requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity("{\"error\": \"Unauthorized\", \"message\": \"Token không hợp lệ hoặc đã hết hạn\"}")
                            .header("Content-Type", "application/json")
                            .build()
            );
            return;
        }

        // Xác thực bổ sung bằng Redis (Chống token hack/revoked/bị khóa tài khoản)
        if (jwt != null && jwt.getSubject() != null) {
            String token = authHeader.substring(7);
            String tokenSuffix = token.length() > 20 ? token.substring(token.length() - 20) : token;
            String redisKey = "jwt:access:" + jwt.getSubject() + ":" + tokenSuffix;

            try {
                ValueCommands<String, String> commands = redisDataSource.value(String.class);
                String tokenStatus = commands.get(redisKey);

                if (tokenStatus == null) {
                    LOG.warnf("Token đã bị thu hồi hoặc đăng xuất khỏi Redis: userId=%s", jwt.getSubject());
                    requestContext.abortWith(
                            Response.status(Response.Status.UNAUTHORIZED)
                                    .entity("{\"error\": \"Unauthorized\", \"message\": \"Token đã bị thu hồi, tài khoản bị khóa hoặc đã đăng xuất!\"}")
                                    .header("Content-Type", "application/json")
                                    .build()
                    );
                }
            } catch (Exception e) {
                LOG.error("Lỗi khi kết nối Redis để kiểm tra token: " + e.getMessage());
                // Fallback: Trong môi trường dev/production, nếu Redis tạm thời down, cho qua để không ngắt quãng dịch vụ
            }
        } else {
            // SmallRye JWT không thể giải mã token hợp lệ
            requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity("{\"error\": \"Unauthorized\", \"message\": \"Mã Token không hợp lệ hoặc đã hết hạn signature!\"}")
                            .header("Content-Type", "application/json")
                            .build()
            );
        }
    }

    private boolean isPublicPath(String path) {
        if (PUBLIC_PATHS.contains(path) || PUBLIC_PATHS.contains("/" + path)) return true;
        // Swagger UI paths
        if (path.startsWith("/q/") || path.startsWith("q/") || path.startsWith("/swagger-ui") || path.startsWith("/openapi")) return true;
        return false;
    }
}
