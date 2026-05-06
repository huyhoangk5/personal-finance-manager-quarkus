package com.finance.pfm.resource;

import com.finance.pfm.entity.User;
import com.finance.pfm.repository.UserRepository;
import com.finance.pfm.service.UserService;
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
        Optional<User> optionalUser = userService.findById(userId);
        if (optionalUser.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        User user = optionalUser.get();
        if (userDetails.fullName != null) user.fullName = userDetails.fullName;
        if (userDetails.email != null) {
            Optional<User> existingEmail = userRepository.findByEmail(userDetails.email);
            if (existingEmail.isPresent() && !existingEmail.get().userId.equals(userId)) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Email đã tồn tại").build();
            }
            user.email = userDetails.email;
        }
        User updatedUser = userService.updateUser(user);
        return Response.ok(updatedUser).build();
    }

    @POST
    @Path("/google-login")
    public Response googleLogin(Map<String, String> payload) {
        String token = payload.get("token");
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
        String otp = userService.generateAndSendOtp(phoneNumber);
        return Response.ok("OTP đã được gửi (giả lập: " + otp + ")").build();
    }

    @POST
    @Path("/verify-otp")
    public Response verifyOtp(Map<String, String> payload) {
        String phoneNumber = payload.get("phoneNumber");
        String otp = payload.get("otp");
        Optional<User> userOpt = userService.verifyOtpAndCreateUser(phoneNumber, otp);
        if (userOpt.isPresent()) {
            return Response.ok(userOpt.get()).build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).entity("OTP không hợp lệ").build();
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
        Long userId = Long.parseLong(payload.get("userId"));
        String oldPassword = payload.get("oldPassword");
        String newPassword = payload.get("newPassword");
        boolean changed = userService.changePassword(userId, oldPassword, newPassword);
        if (changed) {
            return Response.ok("Đổi mật khẩu thành công").build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Mật khẩu cũ không đúng hoặc user không tồn tại").build();
        }
    }

    @GET
    @Path("/qr-code")
    public Response getUserQrCode(@QueryParam("userId") Long userId) {
        String qrToken = userService.generateUserQrCode(userId);
        return Response.ok(qrToken).build();
    }

    @POST
    @Path("/qr-login/confirm")
    public Response confirmQrLogin(Map<String, String> payload) {
        String qrToken = payload.get("qrToken");
        Long userId = Long.parseLong(payload.get("userId"));
        boolean confirmed = userService.confirmQrLogin(qrToken, userId);
        if (confirmed) {
            return Response.ok("Xác nhận thành công").build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Xác nhận thất bại").build();
        }
    }

    @GET
    @Path("/qr-login/status")
    public Response getQrLoginStatus(@QueryParam("token") String token) {
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
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).entity("Email không tồn tại").build();
        }
        String qrToken = userService.generateUserQrCode(userOpt.get().userId);
        return Response.ok(qrToken).build();
    }

    @POST
    @Path("/forgot-password")
    public Response forgotPassword(Map<String, String> payload) {
        String email = payload.get("email");
        String token = userService.createPasswordResetToken(email);
        if (token == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Email không tồn tại").build();
        }
        userService.sendPasswordResetEmail(email, token);
        return Response.ok("Email đặt lại mật khẩu đã được gửi (kiểm tra console)").build();
    }

    @POST
    @Path("/reset-password")
    public Response resetPassword(Map<String, String> payload) {
        String token = payload.get("token");
        String newPassword = payload.get("newPassword");
        boolean success = userService.resetPassword(token, newPassword);
        if (success) {
            return Response.ok("Đặt lại mật khẩu thành công").build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Token không hợp lệ hoặc đã hết hạn").build();
        }
    }

    @POST
    @Path("/qr-register")
    public Response qrRegister(Map<String, String> payload) {
        String token = payload.get("token");
        String email = payload.get("email");
        String password = payload.get("password");

        if (userRepository.findByEmail(email).isPresent()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Email đã tồn tại").build();
        }

        Optional<User> userOpt = userService.registerWithQrToken(token, email, password);
        if (userOpt.isPresent()) {
            return Response.ok(userOpt.get()).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Token không hợp lệ hoặc đã hết hạn").build();
        }
    }
}
