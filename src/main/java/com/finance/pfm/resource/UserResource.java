package com.finance.pfm.resource;

import com.finance.pfm.entity.User;
import com.finance.pfm.repository.UserRepository;
import com.finance.pfm.service.UserService;
import com.finance.pfm.util.ValidationUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.Optional;

@Path("/api/users")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserService userService;

    @Inject
    UserRepository userRepository;

    @POST
    @Path("/register")
    public Response register(User user) {
        String result = userService.registerUser(user);
        if (result.contains("Lỗi")) {
            return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
        }
        return Response.ok(result).build();
    }

    @POST
    @Path("/login")
    public Response login(Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        
        // Basic validation
        if (username == null || username.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Tên đăng nhập không được để trống").build();
        }
        if (password == null || password.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Mật khẩu không được để trống").build();
        }
        
        Optional<User> user = userService.login(username, password);
        if (user.isPresent()) {
            return Response.ok(user.get()).build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Sai tên đăng nhập hoặc mật khẩu!").build();
        }
    }

    @PUT
    @Path("/{userId}")
    public Response updateUser(@PathParam("userId") Long userId, User userDetails) {
        String result = userService.updateUserProfile(userId, userDetails.fullName, userDetails.email);
        if (result.contains("Lỗi")) {
            return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
        }
        
        Optional<User> optionalUser = userService.findById(userId);
        if (optionalUser.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        User updatedUser = userService.updateUser(optionalUser.get());
        return Response.ok(updatedUser).build();
    }

    @POST
    @Path("/google-login")
    public Response googleLogin(Map<String, String> payload) {
        String token = payload.get("token");
        if (token == null || token.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Token Google không được để trống").build();
        }
        
        Optional<User> userOpt = userService.authenticateGoogle(token);
        if (userOpt.isPresent()) {
            return Response.ok(userOpt.get()).build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Xác thực Google thất bại").build();
        }
    }

    @POST
    @Path("/facebook-login")
    public Response facebookLogin(Map<String, String> payload) {
        String token = payload.get("token");
        if (token == null || token.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Token Facebook không được để trống").build();
        }
        
        Optional<User> userOpt = userService.authenticateFacebook(token);
        if (userOpt.isPresent()) {
            return Response.ok(userOpt.get()).build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Xác thực Facebook thất bại").build();
        }
    }

    @POST
    @Path("/send-otp")
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
            return Response.ok(userOpt.get()).build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).entity("OTP không hợp lệ hoặc đã hết hạn").build();
        }
    }

    @GET
    @Path("/qr-login/generate")
    public Response generateQrToken() {
        String token = userService.generateQrToken();
        return Response.ok(token).build();
    }

    @GET
    @Path("/qr-login/verify")
    public Response verifyQrToken(@QueryParam("token") String token) {
        if (token == null || token.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Token không được để trống").build();
        }
        
        Optional<User> userOpt = userService.verifyQrToken(token);
        if (userOpt.isPresent()) {
            return Response.ok(userOpt.get()).build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Token không hợp lệ hoặc đã hết hạn").build();
        }
    }

    @PUT
    @Path("/change-password")
    public Response changePassword(Map<String, String> payload) {
        try {
            Long userId = Long.parseLong(payload.get("userId"));
            String oldPassword = payload.get("oldPassword");
            String newPassword = payload.get("newPassword");
            
            String result = userService.changePassword(userId, oldPassword, newPassword);
            if (result.contains("Lỗi")) {
                return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
            }
            return Response.ok(result).build();
        } catch (NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("User ID không hợp lệ").build();
        }
    }

    @GET
    @Path("/qr-code")
    public Response getUserQrCode(@QueryParam("userId") Long userId) {
        if (userId == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("User ID không được để trống").build();
        }
        
        try {
            String qrToken = userService.generateUserQrCode(userId);
            return Response.ok(qrToken).build();
        } catch (RuntimeException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("Không tìm thấy người dùng").build();
        }
    }

    @POST
    @Path("/qr-login/confirm")
    public Response confirmQrLogin(Map<String, String> payload) {
        String qrToken = payload.get("qrToken");
        String userIdStr = payload.get("userId");
        
        if (qrToken == null || qrToken.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("QR Token không được để trống").build();
        }
        if (userIdStr == null || userIdStr.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("User ID không được để trống").build();
        }
        
        try {
            Long userId = Long.parseLong(userIdStr);
            boolean confirmed = userService.confirmQrLogin(qrToken, userId);
            if (confirmed) {
                return Response.ok("Xác nhận thành công").build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Xác nhận thất bại").build();
            }
        } catch (NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("User ID không hợp lệ").build();
        }
    }

    @GET
    @Path("/qr-login/status")
    public Response getQrLoginStatus(@QueryParam("token") String token) {
        if (token == null || token.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Token không được để trống").build();
        }
        
        Optional<User> userOpt = userService.getQrLoginUser(token);
        if (userOpt.isPresent()) {
            return Response.ok(userOpt.get()).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity("Chưa có đăng nhập").build();
        }
    }

    @GET
    @Path("/qr-code-by-email")
    public Response getQrCodeByEmail(@QueryParam("email") String email) {
        // Validate email
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
    @Path("/forgot-password")
    public Response forgotPassword(Map<String, String> payload) {
        String email = payload.get("email");
        String result = userService.createPasswordResetToken(email);
        if (result.contains("Lỗi")) {
            return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
        }
        userService.sendPasswordResetEmail(email, result);
        return Response.ok("Email đặt lại mật khẩu đã được gửi (kiểm tra console)").build();
    }

    @POST
    @Path("/reset-password")
    public Response resetPassword(Map<String, String> payload) {
        String token = payload.get("token");
        String newPassword = payload.get("newPassword");
        
        String result = userService.resetPassword(token, newPassword);
        if (result.contains("Lỗi")) {
            return Response.status(Response.Status.BAD_REQUEST).entity(result).build();
        }
        return Response.ok(result).build();
    }

    @POST
    @Path("/qr-register")
    public Response qrRegister(Map<String, String> payload) {
        String token = payload.get("token");
        String email = payload.get("email");
        String password = payload.get("password");

        if (token == null || token.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Token không được để trống").build();
        }

        // Validate email
        ValidationUtil.ValidationResult emailValidation = ValidationUtil.validateEmail(email);
        if (!emailValidation.isValid()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(emailValidation.getFirstError()).build();
        }

        // Validate password
        ValidationUtil.ValidationResult passwordValidation = ValidationUtil.validatePassword(password);
        if (!passwordValidation.isValid()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(passwordValidation.getFirstError()).build();
        }

        if (userRepository.findByEmail(email.trim().toLowerCase()).isPresent()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Email đã tồn tại trong hệ thống").build();
        }

        Optional<User> userOpt = userService.registerWithQrToken(token, email, password);
        if (userOpt.isPresent()) {
            return Response.ok(userOpt.get()).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Token không hợp lệ hoặc đã hết hạn").build();
        }
    }
}
