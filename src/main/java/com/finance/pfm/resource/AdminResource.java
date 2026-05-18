package com.finance.pfm.resource;

import com.finance.pfm.dto.AuditLogDTO;
import com.finance.pfm.dto.UserDTO;
import com.finance.pfm.entity.User;
import com.finance.pfm.repository.UserRepository;
import com.finance.pfm.service.AdminService;
import com.finance.pfm.service.AuditLogService;
import com.finance.pfm.service.JwtService;
import com.finance.pfm.service.UserService;
import com.finance.pfm.util.ValidationUtil;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Path("/api/admin")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin", description = "Các tính năng dành riêng cho quản trị viên")
public class AdminResource {

    @Inject
    AdminService adminService;

    @Inject
    UserService userService;

    @Inject
    UserRepository userRepository;

    @Inject
    JwtService jwtService;

    @Inject
    AuditLogService auditLogService;

    // ─── 1. Quản lý người dùng ───────────────────────────────────────────────

    @GET
    @Path("/users")
    @Operation(summary = "Xem danh sách người dùng toàn hệ thống")
    public List<UserDTO> getAllUsers() {
        List<User> users = userRepository.listAll();
        return users.stream()
                .map(UserDTO::from)
                .collect(Collectors.toList());
    }

    @PUT
    @Path("/users/{id}/lock")
    @Transactional
    @Operation(summary = "Khóa hoặc mở khóa tài khoản người dùng")
    public Response toggleLockUser(@PathParam("id") Long id, @Context SecurityContext ctx) {
        Long adminId = Long.parseLong(ctx.getUserPrincipal().getName());
        if (adminId.equals(id)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Bạn không thể tự khóa tài khoản của chính mình!")
                    .build();
        }

        User user = userRepository.findById(id);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Không tìm thấy người dùng")
                    .build();
        }

        user.locked = !user.locked;
        userService.updateUser(user);

        // Nếu bị khóa, thu hồi ngay lập tức tất cả JWT token
        if (user.locked) {
            jwtService.revokeAllTokens(user.userId);
        }

        String action = user.locked ? "LOCK_USER" : "UNLOCK_USER";
        auditLogService.log(adminId, action, "USER", user.userId,
                "Admin " + ctx.getUserPrincipal().getName() + (user.locked ? " đã khóa " : " đã mở khóa ") + "tài khoản " + user.username,
                "ADMIN_CONSOLE");

        return Response.ok(UserDTO.from(user)).build();
    }

    @PUT
    @Path("/users/{id}/temp-lock")
    @Transactional
    @Operation(summary = "Khóa tạm thời người dùng vì hoạt động đáng ngờ")
    public Response tempLockUser(@PathParam("id") Long id, @Context SecurityContext ctx) {
        Long adminId = Long.parseLong(ctx.getUserPrincipal().getName());
        if (adminId.equals(id)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Không thể tự khóa tài khoản của chính mình!")
                    .build();
        }

        User user = userRepository.findById(id);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Không tìm thấy người dùng")
                    .build();
        }

        user.locked = true;
        userService.updateUser(user);

        // Thu hồi toàn bộ token
        jwtService.revokeAllTokens(user.userId);

        auditLogService.log(adminId, "TEMP_LOCK_USER", "USER", user.userId,
                "Khóa tạm thời do hoạt động nghi vấn: " + user.username, "ADMIN_CONSOLE");

        return Response.ok(UserDTO.from(user)).build();
    }

    @PUT
    @Path("/users/{id}/reset-password")
    @Transactional
    @Operation(summary = "Đặt lại mật khẩu cho người dùng")
    public Response resetPassword(@PathParam("id") Long id, Map<String, String> payload, @Context SecurityContext ctx) {
        Long adminId = Long.parseLong(ctx.getUserPrincipal().getName());
        User user = userRepository.findById(id);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Không tìm thấy người dùng")
                    .build();
        }

        String newPassword = payload != null ? payload.get("newPassword") : null;
        if (newPassword == null || newPassword.trim().isEmpty()) {
            // Tự động sinh mật khẩu tạm thời
            newPassword = "Temp@" + String.format("%06d", new Random().nextInt(999999));
        } else {
            ValidationUtil.ValidationResult passwordValidation = ValidationUtil.validatePassword(newPassword);
            if (!passwordValidation.isValid()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(passwordValidation.getFirstError())
                        .build();
            }
        }

        user.password = BcryptUtil.bcryptHash(newPassword);
        userService.updateUser(user);

        // Thu hồi token để bắt buộc đăng nhập lại
        jwtService.revokeAllTokens(user.userId);

        auditLogService.log(adminId, "RESET_PASSWORD_BY_ADMIN", "USER", user.userId,
                "Admin đặt lại mật khẩu cho user: " + user.username, "ADMIN_CONSOLE");

        return Response.ok(Map.of(
                "message", "Đặt lại mật khẩu thành công!",
                "newPassword", newPassword
        )).build();
    }

    @PUT
    @Path("/users/{id}/role")
    @Transactional
    @Operation(summary = "Phân quyền (USER hoặc ADMIN)")
    public Response changeRole(@PathParam("id") Long id, Map<String, String> payload, @Context SecurityContext ctx) {
        Long adminId = Long.parseLong(ctx.getUserPrincipal().getName());
        if (adminId.equals(id)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Bạn không thể tự thay đổi quyền của chính mình!")
                    .build();
        }

        User user = userRepository.findById(id);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Không tìm thấy người dùng")
                    .build();
        }

        String roleStr = payload != null ? payload.get("role") : null;
        if (roleStr == null || (!roleStr.equals("USER") && !roleStr.equals("ADMIN"))) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Quyền hạn không hợp lệ! Chỉ chấp nhận USER hoặc ADMIN.")
                    .build();
        }

        user.role = User.Role.valueOf(roleStr);
        userService.updateUser(user);

        // Thu hồi toàn bộ token của user để nạp lại role mới trong JWT
        jwtService.revokeAllTokens(user.userId);

        auditLogService.log(adminId, "CHANGE_ROLE", "USER", user.userId,
                "Admin thay đổi quyền của " + user.username + " thành " + roleStr, "ADMIN_CONSOLE");

        return Response.ok(UserDTO.from(user)).build();
    }

    // ─── 2. Xem thống kê toàn hệ thống ───────────────────────────────────────

    @GET
    @Path("/statistics")
    @Operation(summary = "Xem thống kê toàn hệ thống (Dashboard Admin)")
    public Response getSystemStatistics() {
        Map<String, Object> stats = adminService.getSystemStatistics();
        return Response.ok(stats).build();
    }

    // ─── 4. Xem audit log ───────────────────────────────────────────────────

    @GET
    @Path("/audit-logs")
    @Operation(summary = "Xem lịch sử hoạt động toàn hệ thống")
    public List<AuditLogDTO> getAuditLogs(@QueryParam("limit") @DefaultValue("100") int limit) {
        return adminService.getAuditLogs(limit);
    }

    // ─── 5. Quản lý cảnh báo/fraud ──────────────────────────────────────────

    @GET
    @Path("/alerts")
    @Operation(summary = "Xem danh sách cảnh báo nghi vấn và gian lận")
    public List<AuditLogDTO> getAlerts() {
        return adminService.getAlerts();
    }

    @PUT
    @Path("/alerts/{logId}/safe")
    @Operation(summary = "Đánh dấu giao dịch/hoạt động là an toàn")
    public Response markAlertSafe(@PathParam("logId") Long logId, @Context SecurityContext ctx) {
        Long adminId = Long.parseLong(ctx.getUserPrincipal().getName());
        adminService.markAlertSafe(logId);

        auditLogService.log(adminId, "MARK_ALERT_SAFE", "AUDIT_LOG", logId,
                "Admin đánh dấu cảnh báo #" + logId + " là an toàn", "ADMIN_CONSOLE");

        return Response.ok("Đã đánh dấu hoạt động này an toàn").build();
    }
}
