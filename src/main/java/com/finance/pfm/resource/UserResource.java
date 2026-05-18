package com.finance.pfm.resource;

import com.finance.pfm.dto.AuthResponse;
import com.finance.pfm.dto.UserDTO;
import com.finance.pfm.entity.User;
import com.finance.pfm.repository.UserRepository;
import com.finance.pfm.service.JwtService;
import com.finance.pfm.service.UserService;
import com.finance.pfm.util.ValidationUtil;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;
import java.util.Optional;

@Path("/api/users")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Users", description = "Đăng ký, đăng nhập, quản lý tài khoản")
@SecurityScheme(
    securitySchemeName = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class UserResource {

    @Inject
    UserService userService;

    @Inject
    JwtService jwtService;

    @Inject
    UserRepository userRepository;

    @Inject
    JsonWebToken jwt;

    // ─── Public endpoints (không cần token) ──────────────────────────────────

    @POST
    @Path("/register")
    @Operation(summary = "Đăng ký tài khoản mới")
    public Response register(User user) {
        String result = userService.registerUser(user);
        if (result.contains("Lỗi")) {
            return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
        }
        return Response.ok(result).build();
    }

    @POST
    @Path("/login")
    @Operation(summary = "Đăng nhập, trả về JWT access token + refresh token")
    public Response login(Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username == null || username.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Tên đăng nhập không được để trống").build();
        }
        if (password == null || password.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Mật khẩu không được để trống").build();
        }

        Optional<User> userOpt = userService.login(username, password);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            AuthResponse response = new AuthResponse(accessToken, refreshToken, 900, UserDTO.from(user));
            return Response.ok(response).build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Sai tên đăng nhập hoặc mật khẩu!").build();
        }
    }

    @POST
    @Path("/refresh-token")
    @Operation(summary = "Làm mới access token bằng refresh token")
    public Response refreshToken(Map<String, String> payload) {
        String refreshToken = payload.get("refreshToken");
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Refresh token không được để trống").build();
        }

        Optional<User> userOpt = jwtService.validateRefreshToken(refreshToken);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String newAccessToken = jwtService.generateAccessToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);
            AuthResponse response = new AuthResponse(newAccessToken, newRefreshToken, 900, UserDTO.from(user));
            return Response.ok(response).build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Refresh token không hợp lệ hoặc đã hết hạn").build();
        }
    }

    @POST
    @Path("/logout")
    @Operation(summary = "Đăng xuất, revoke refresh token")
    public Response logout(Map<String, String> payload) {
        String refreshToken = payload.get("refreshToken");
        if (refreshToken != null && !refreshToken.trim().isEmpty()) {
            jwtService.revokeRefreshToken(refreshToken);
        }
        return Response.ok("Đăng xuất thành công").build();
    }

    @POST
    @Path("/google-login")
    @Operation(summary = "Đăng nhập bằng Google OAuth")
    public Response googleLogin(Map<String, String> payload) {
        String token = payload.get("token");
        if (token == null || token.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Token Google không được để trống").build();
        }

        Optional<User> userOpt = userService.authenticateGoogle(token);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            AuthResponse response = new AuthResponse(accessToken, refreshToken, 900, UserDTO.from(user));
            return Response.ok(response).build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Xác thực Google thất bại").build();
        }
    }

    @POST
    @Path("/facebook-login")
    @Operation(summary = "Đăng nhập bằng Facebook (chưa hỗ trợ)")
    public Response facebookLogin(Map<String, String> payload) {
        String token = payload.get("token");
        if (token == null || token.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Token Facebook không được để trống").build();
        }

        Optional<User> userOpt = userService.authenticateFacebook(token);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            AuthResponse response = new AuthResponse(accessToken, refreshToken, 900, UserDTO.from(user));
            return Response.ok(response).build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Xác thực Facebook thất bại").build();
        }
    }

    @POST
    @Path("/forgot-password")
    @Operation(summary = "Gửi email đặt lại mật khẩu")
    public Response forgotPassword(Map<String, String> payload) {
        String email = payload.get("email");
        String result = userService.createPasswordResetToken(email);
        if (result == null || result.contains("Lỗi")) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Email không tồn tại trong hệ thống").build();
        }
        userService.sendPasswordResetEmail(email, result);
        return Response.ok("Email đặt lại mật khẩu đã được gửi (kiểm tra console)").build();
    }

    @POST
    @Path("/reset-password")
    @Operation(summary = "Đặt lại mật khẩu bằng token")
    public Response resetPassword(Map<String, String> payload) {
        String token = payload.get("token");
        String newPassword = payload.get("newPassword");

        String result = userService.resetPassword(token, newPassword);
        if (result.contains("Lỗi")) {
            return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
        }
        return Response.ok(result).build();
    }

    // ─── QR endpoints (public) ────────────────────────────────────────────────

    @GET
    @Path("/qr-login/generate")
    @Operation(summary = "Tạo QR token để đăng nhập")
    public Response generateQrToken() {
        String token = userService.generateQrToken();
        return Response.ok(token).build();
    }

    @GET
    @Path("/qr-login/verify")
    @Operation(summary = "Xác thực QR token")
    public Response verifyQrToken(@QueryParam("token") String token) {
        if (token == null || token.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Token không được để trống").build();
        }

        Optional<User> userOpt = userService.verifyQrToken(token);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            return Response.ok(new AuthResponse(accessToken, refreshToken, 900, UserDTO.from(user))).build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Token không hợp lệ hoặc đã hết hạn").build();
        }
    }

    @GET
    @Path("/qr-login/status")
    @Operation(summary = "Kiểm tra trạng thái đăng nhập QR")
    public Response getQrLoginStatus(@QueryParam("token") String token) {
        if (token == null || token.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Token không được để trống").build();
        }

        Optional<User> userOpt = userService.getQrLoginUser(token);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            return Response.ok(new AuthResponse(accessToken, refreshToken, 900, UserDTO.from(user))).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity("Chưa có đăng nhập").build();
        }
    }

    @GET
    @Path("/qr-code-by-email")
    @Operation(summary = "Lấy QR code theo email")
    public Response getQrCodeByEmail(@QueryParam("email") String email) {
        ValidationUtil.ValidationResult emailValidation = ValidationUtil.validateEmail(email);
        if (!emailValidation.isValid()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(emailValidation.getFirstError()).build();
        }

        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).entity("Email không tồn tại trong hệ thống").build();
        }
        String qrToken = userService.generateUserQrCode(userOpt.get().userId);
        return Response.ok(qrToken).build();
    }

    @POST
    @Path("/qr-login/confirm")
    @RolesAllowed({"USER", "ADMIN"})
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Xác nhận đăng nhập QR từ thiết bị đã đăng nhập")
    public Response confirmQrLogin(Map<String, String> payload, @Context SecurityContext ctx) {
        String qrToken = payload.get("qrToken");

        if (qrToken == null || qrToken.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("QR Token không được để trống").build();
        }

        try {
            Long userId = Long.parseLong(ctx.getUserPrincipal().getName());
            boolean confirmed = userService.confirmQrLogin(qrToken, userId);
            if (confirmed) {
                return Response.ok("Xác nhận thành công").build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Xác nhận thất bại").build();
            }
        } catch (NumberFormatException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Token không hợp lệ").build();
        }
    }

    @POST
    @Path("/qr-register")
    @Operation(summary = "Đăng ký tài khoản qua QR")
    public Response qrRegister(Map<String, String> payload) {
        String token = payload.get("token");
        String email = payload.get("email");
        String password = payload.get("password");

        if (token == null || token.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Token không được để trống").build();
        }

        ValidationUtil.ValidationResult emailValidation = ValidationUtil.validateEmail(email);
        if (!emailValidation.isValid()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(emailValidation.getFirstError()).build();
        }

        ValidationUtil.ValidationResult passwordValidation = ValidationUtil.validatePassword(password);
        if (!passwordValidation.isValid()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(passwordValidation.getFirstError()).build();
        }

        if (userRepository.findByEmail(email.trim().toLowerCase()).isPresent()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Email đã tồn tại trong hệ thống").build();
        }

        Optional<User> userOpt = userService.registerWithQrToken(token, email, password);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            return Response.ok(new AuthResponse(accessToken, refreshToken, 900, UserDTO.from(user))).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Token không hợp lệ hoặc đã hết hạn").build();
        }
    }

    // ─── Protected endpoints (cần JWT token) ─────────────────────────────────

    @PUT
    @Path("/profile")
    @RolesAllowed({"USER", "ADMIN"})
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cập nhật thông tin cá nhân")
    public Response updateProfile(User userDetails, @Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());

        Optional<User> optionalUser = userService.findById(userId);
        if (optionalUser.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        User user = optionalUser.get();

        if (userDetails.fullName != null && !userDetails.fullName.trim().isEmpty()) {
            ValidationUtil.ValidationResult fullNameValidation = ValidationUtil.validateFullName(userDetails.fullName);
            if (!fullNameValidation.isValid()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Lỗi: " + fullNameValidation.getFirstError()).build();
            }
            user.fullName = ValidationUtil.normalizeString(userDetails.fullName);
        }

        if (userDetails.email != null && !userDetails.email.trim().isEmpty()) {
            ValidationUtil.ValidationResult emailValidation = ValidationUtil.validateEmail(userDetails.email);
            if (!emailValidation.isValid()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Lỗi: " + emailValidation.getFirstError()).build();
            }

            String normalizedEmail = ValidationUtil.normalizeString(userDetails.email).toLowerCase();
            Optional<User> existingEmail = userRepository.findByEmail(normalizedEmail);
            if (existingEmail.isPresent() && !existingEmail.get().userId.equals(userId)) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Email đã tồn tại").build();
            }
            user.email = normalizedEmail;
        }

        User updatedUser = userService.updateUser(user);
        return Response.ok(UserDTO.from(updatedUser)).build();
    }

    @PUT
    @Path("/{userId}")
    @RolesAllowed({"USER", "ADMIN"})
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cập nhật thông tin user theo ID (legacy, dùng /profile thay thế)")
    public Response updateUser(@PathParam("userId") Long userId, User userDetails, @Context SecurityContext ctx) {
        Long tokenUserId = Long.parseLong(ctx.getUserPrincipal().getName());
        // Chỉ cho phép sửa chính mình (trừ ADMIN)
        if (!tokenUserId.equals(userId) && !ctx.isUserInRole("ADMIN")) {
            return Response.status(Response.Status.FORBIDDEN).entity("Không có quyền sửa tài khoản người khác").build();
        }

        Optional<User> optionalUser = userService.findById(userId);
        if (optionalUser.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        User user = optionalUser.get();

        if (userDetails.fullName != null && !userDetails.fullName.trim().isEmpty()) {
            ValidationUtil.ValidationResult fullNameValidation = ValidationUtil.validateFullName(userDetails.fullName);
            if (!fullNameValidation.isValid()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Lỗi: " + fullNameValidation.getFirstError()).build();
            }
            user.fullName = ValidationUtil.normalizeString(userDetails.fullName);
        }

        if (userDetails.email != null && !userDetails.email.trim().isEmpty()) {
            ValidationUtil.ValidationResult emailValidation = ValidationUtil.validateEmail(userDetails.email);
            if (!emailValidation.isValid()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Lỗi: " + emailValidation.getFirstError()).build();
            }

            String normalizedEmail = ValidationUtil.normalizeString(userDetails.email).toLowerCase();
            Optional<User> existingEmail = userRepository.findByEmail(normalizedEmail);
            if (existingEmail.isPresent() && !existingEmail.get().userId.equals(userId)) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Email đã tồn tại").build();
            }
            user.email = normalizedEmail;
        }

        User updatedUser = userService.updateUser(user);
        return Response.ok(UserDTO.from(updatedUser)).build();
    }

    @PUT
    @Path("/change-password")
    @RolesAllowed({"USER", "ADMIN"})
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Đổi mật khẩu")
    public Response changePassword(Map<String, String> payload, @Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());
        String oldPassword = payload.get("oldPassword");
        String newPassword = payload.get("newPassword");

        String result = userService.changePassword(userId, oldPassword, newPassword);
        if (result.contains("Lỗi")) {
            return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
        }
        return Response.ok(result).build();
    }

    @GET
    @Path("/qr-code")
    @RolesAllowed({"USER", "ADMIN"})
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Lấy QR code của user đang đăng nhập")
    public Response getUserQrCode(@Context SecurityContext ctx) {
        Long userId = Long.parseLong(ctx.getUserPrincipal().getName());
        try {
            String qrToken = userService.generateUserQrCode(userId);
            return Response.ok(qrToken).build();
        } catch (RuntimeException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("Không tìm thấy người dùng").build();
        }
    }

    @POST
    @Path("/send-otp")
    @Operation(summary = "Gửi OTP (chưa hỗ trợ)")
    public Response sendOtp(Map<String, String> payload) {
        String phoneNumber = payload.get("phoneNumber");
        String result = userService.generateAndSendOtp(phoneNumber);
        if (result.contains("Lỗi")) {
            return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
        }
        return Response.ok(result).build();
    }

    @POST
    @Path("/verify-otp")
    @Operation(summary = "Xác thực OTP (chưa hỗ trợ)")
    public Response verifyOtp(Map<String, String> payload) {
        String phoneNumber = payload.get("phoneNumber");
        String otp = payload.get("otp");

        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Số điện thoại không được để trống").build();
        }
        if (otp == null || otp.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Mã OTP không được để trống").build();
        }

        Optional<User> userOpt = userService.verifyOtpAndCreateUser(phoneNumber, otp);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            return Response.ok(new AuthResponse(accessToken, refreshToken, 900, UserDTO.from(user))).build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).entity("OTP không hợp lệ hoặc đã hết hạn").build();
        }
    }
}
