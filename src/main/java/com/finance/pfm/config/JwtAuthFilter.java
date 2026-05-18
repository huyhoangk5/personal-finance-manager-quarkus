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
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JwtAuthFilter — kiểm tra Bearer token trên các endpoint cần bảo vệ.
 * Kết nối với Redis để kiểm tra trạng thái token (Blacklist/Revocation).
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(JwtAuthFilter.class);

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

        String token = authHeader.substring(7);
        
        // Giải mã thủ công JWT Payload để lấy userId (sub) vì trong JAX-RS Filter 
        // của Quarkus RESTEasy Reactive, authentication context được tải lazy và 
        // chưa sẵn sàng ở giai đoạn filter này.
        String userId = getSubjectFromToken(token);

        if (userId != null) {
            String tokenSuffix = token.length() > 20 ? token.substring(token.length() - 20) : token;
            String redisKey = "jwt:access:" + userId + ":" + tokenSuffix;

            try {
                ValueCommands<String, String> commands = redisDataSource.value(String.class);
                String tokenStatus = commands.get(redisKey);

                if (tokenStatus == null) {
                    LOG.warnf("Token đã bị thu hồi hoặc đăng xuất khỏi Redis: userId=%s", userId);
                    requestContext.abortWith(
                            Response.status(Response.Status.UNAUTHORIZED)
                                    .entity("{\"error\": \"Unauthorized\", \"message\": \"Phiên đăng nhập đã hết hạn hoặc tài khoản của bạn bị khóa!\"}")
                                    .header("Content-Type", "application/json")
                                    .build()
                    );
                }
            } catch (Exception e) {
                LOG.error("Lỗi khi kết nối Redis để kiểm tra token: " + e.getMessage());
                // Fallback: Nếu Redis tạm thời sập, vẫn cho phép đi tiếp để không gây gián đoạn dịch vụ hệ thống
            }
        } else {
            // Token không hợp lệ hoặc không chứa claim "sub"
            LOG.warn("Token không chứa claim subject hoặc không đúng định dạng JWT");
            requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity("{\"error\": \"Unauthorized\", \"message\": \"Mã Token không đúng định dạng hoặc đã hết hạn!\"}")
                            .header("Content-Type", "application/json")
                            .build()
            );
        }
    }

    private boolean isPublicPath(String path) {
        if (PUBLIC_PATHS.contains(path) || PUBLIC_PATHS.contains("/" + path)) return true;
        if (path.startsWith("/q/") || path.startsWith("q/") || path.startsWith("/swagger-ui") || path.startsWith("/openapi")) return true;
        return false;
    }

    /**
     * Helper giải mã thủ công payload của JWT và trích xuất claim "sub" (userId)
     */
    private String getSubjectFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                
                // Trích xuất "sub" bằng Regex nhanh chóng, tránh dùng thư viện JSON bên ngoài
                Pattern pattern = Pattern.compile("\"sub\"\\s*:\\s*\"([^\"]+)\"");
                Matcher matcher = pattern.matcher(payloadJson);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        } catch (Exception e) {
            LOG.error("Lỗi giải mã payload JWT thủ công: " + e.getMessage());
        }
        return null;
    }
}
