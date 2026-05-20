package com.finance.pfm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AdminChangeRoleRequest {
    @NotBlank(message = "Quyền hạn không được để trống")
    @Pattern(regexp = "^(USER|ADMIN)$", message = "Quyền hạn không hợp lệ! Chỉ chấp nhận USER hoặc ADMIN.")
    private String role;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
