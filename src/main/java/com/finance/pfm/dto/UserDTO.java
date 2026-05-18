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

    public static UserDTO from(User user) {
        UserDTO dto = new UserDTO();
        dto.userId = user.userId;
        dto.username = user.username;
        dto.fullName = user.fullName;
        dto.email = user.email;
        dto.role = user.role != null ? user.role.name() : "USER";
        dto.locked = user.locked;
        dto.createdAt = user.createdAt;
        dto.lastLoginAt = user.lastLoginAt;
        return dto;
    }
}
