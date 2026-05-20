package com.finance.pfm.dto;

import com.finance.pfm.entity.User;
import java.time.LocalDateTime;

/**
 * DTO trả về thông tin user — không bao gồm password.
 */
public class UserDTO {
    public Long userId;
    public String username;
    public String fullName;
    public String email;
    public String role;
    public boolean locked;
    public LocalDateTime createdAt;
    public LocalDateTime lastLoginAt;
    public String authType;
    public String otpStatus;

    public static UserDTO from(User user) {
        UserDTO dto = new UserDTO();
        dto.userId = user.userId;
        dto.username = user.username;
        dto.fullName = user.fullName;
        dto.email = user.email;
        dto.role = user.role != null ? user.role.name() : "USER";
        dto.locked = user.isLocked();
        dto.createdAt = user.createdAt;
        dto.lastLoginAt = user.lastLoginAt;

        // Xử lý loại xác thực và trạng thái OTP dựa trên thông tin sẵn có
        if (user.username != null) {
            if (user.username.startsWith("qr_")) {
                dto.authType = "QR Code";
                dto.otpStatus = "Không dùng";
            } else if (user.username.contains("facebook") || (user.fullName != null && user.fullName.contains("Facebook"))) {
                dto.authType = "Facebook OAuth";
                dto.otpStatus = "Đã liên kết";
            } else if (user.username.contains("google") || (user.fullName != null && user.fullName.contains("Google"))) {
                dto.authType = "Google OAuth";
                dto.otpStatus = "Đã liên kết";
            } else if (user.username.matches("^\\+?[0-9]{8,15}$")) {
                dto.authType = "Số điện thoại (OTP)";
                dto.otpStatus = "Đang kích hoạt";
            } else {
                dto.authType = "Mật khẩu truyền thống";
                dto.otpStatus = (user.email != null && !user.email.isEmpty()) ? "Đã xác thực Email" : "Chưa xác thực OTP";
            }
        } else {
            dto.authType = "Không xác định";
            dto.otpStatus = "Không dùng";
        }

        return dto;
    }
}
