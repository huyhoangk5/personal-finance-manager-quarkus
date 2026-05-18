package com.finance.pfm.config;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.Set;

/**
 * JwtAuthFilter — kiểm tra Bearer token trên các endpoint cần bảo vệ.
 * Các endpoint public (login, register, forgot-password...) được bỏ qua.
 * Quarkus SmallRye JWT tự verify token — filter này chỉ kiểm tra sự tồn tại.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(JwtAuthFilter.class);

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
            "/api/users/qr-login/confirm",
            "/q/health",
            "/q/health/live",
            "/q/health/ready",
            "/q/openapi",
            "/q/swagger-ui"
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
        }
        // Quarkus SmallRye JWT sẽ tự verify token và inject @Context SecurityContext
    }

    private boolean isPublicPath(String path) {
        if (PUBLIC_PATHS.contains(path)) return true;
        // Swagger UI paths
        if (path.startsWith("/q/")) return true;
        return false;
    }
}
