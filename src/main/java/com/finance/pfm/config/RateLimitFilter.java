package com.finance.pfm.config;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * RateLimitFilter — JAX-RS ContainerRequestFilter áp dụng rate limiting
 * dựa trên IP address của client và đường dẫn endpoint.
 *
 * Các giới hạn:
 *   - Login/Register/Google-Login: 10 req / 5 phút (chống brute-force)
 *   - Transactions (POST/PUT/DELETE):  60 req / phút
 *   - Dashboard APIs:                 120 req / phút
 */
@Provider
public class RateLimitFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(RateLimitFilter.class);

    // ── Giới hạn auth endpoints (chống brute-force) ──────────────────────────
    private static final int AUTH_LIMIT = 10;
    private static final long AUTH_WINDOW_SECONDS = 300; // 5 phút

    // ── Giới hạn transaction write endpoints ─────────────────────────────────
    private static final int TRANSACTION_WRITE_LIMIT = 60;
    private static final long TRANSACTION_WRITE_WINDOW_SECONDS = 60; // 1 phút

    // ── Giới hạn dashboard endpoints ─────────────────────────────────────────
    private static final int DASHBOARD_LIMIT = 120;
    private static final long DASHBOARD_WINDOW_SECONDS = 60; // 1 phút

    @Inject
    RateLimitService rateLimitService;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        String method = requestContext.getMethod();
        String clientIp = getClientIp(requestContext);

        // ── 1. Auth endpoints — giới hạn nghiêm ngặt ─────────────────────────
        if (isAuthEndpoint(path)) {
            String key = "rate:auth:" + clientIp;
            if (!rateLimitService.isAllowed(key, AUTH_LIMIT, AUTH_WINDOW_SECONDS)) {
                LOG.warnf("Rate limit AUTH vượt mức: ip=%s path=%s", clientIp, path);
                abortWithTooManyRequests(requestContext,
                        "Quá nhiều lần thử đăng nhập. Vui lòng thử lại sau 5 phút.");
                return;
            }
        }

        // ── 2. Transaction write (POST/PUT/DELETE) ────────────────────────────
        else if (path.startsWith("/api/transactions") && isWriteMethod(method)) {
            String key = "rate:txn-write:" + clientIp;
            if (!rateLimitService.isAllowed(key, TRANSACTION_WRITE_LIMIT, TRANSACTION_WRITE_WINDOW_SECONDS)) {
                LOG.warnf("Rate limit TRANSACTION WRITE vượt mức: ip=%s", clientIp);
                abortWithTooManyRequests(requestContext,
                        "Quá nhiều yêu cầu. Vui lòng thử lại sau.");
                return;
            }
        }

        // ── 3. Dashboard endpoints ────────────────────────────────────────────
        else if (path.startsWith("/api/dashboard")) {
            String key = "rate:dashboard:" + clientIp;
            if (!rateLimitService.isAllowed(key, DASHBOARD_LIMIT, DASHBOARD_WINDOW_SECONDS)) {
                LOG.warnf("Rate limit DASHBOARD vượt mức: ip=%s", clientIp);
                abortWithTooManyRequests(requestContext,
                        "Quá nhiều yêu cầu. Vui lòng thử lại sau.");
                return;
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Trả về true nếu path là auth endpoint cần rate limit nghiêm ngặt.
     */
    private boolean isAuthEndpoint(String path) {
        return path.equals("/api/users/login")
                || path.equals("/api/users/register")
                || path.equals("/api/users/google-login")
                || path.equals("/api/users/facebook-login")
                || path.equals("/api/users/forgot-password")
                || path.equals("/api/users/reset-password")
                || path.equals("/api/users/send-otp")
                || path.equals("/api/users/verify-otp");
    }

    /**
     * Trả về true nếu HTTP method là write operation.
     */
    private boolean isWriteMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method);
    }

    /**
     * Lấy IP thực của client, xử lý trường hợp đứng sau reverse proxy (Render/CloudFlare).
     */
    private String getClientIp(ContainerRequestContext requestContext) {
        // Kiểm tra X-Forwarded-For header (Render dùng reverse proxy)
        String forwarded = requestContext.getHeaderString("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            // X-Forwarded-For có thể chứa nhiều IP: "client, proxy1, proxy2"
            return forwarded.split(",")[0].trim();
        }

        String realIp = requestContext.getHeaderString("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp.trim();
        }

        // Fallback — không xác định được IP (hiếm gặp)
        return "unknown";
    }

    /**
     * Abort request với HTTP 429 Too Many Requests.
     */
    private void abortWithTooManyRequests(ContainerRequestContext ctx, String message) {
        ctx.abortWith(
            Response.status(429)
                    .header("Content-Type", "application/json")
                    .header("Retry-After", "60")
                    .entity("{\"error\": \"Too Many Requests\", \"message\": \"" + message + "\"}")
                    .build()
        );
    }
}
