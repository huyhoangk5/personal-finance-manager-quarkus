package com.finance.pfm.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    public Long userId;

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập phải từ 3-50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9.@_-]{3,50}$", message = "Tên đăng nhập không hợp lệ")
    @Column(nullable = false, unique = true, name = "username", length = 50)
    public String username;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @NotBlank(message = "Mật khẩu không được để trống")
    @Column(nullable = false, name = "password")
    public String password;

    @Size(max = 100, message = "Họ và tên không được vượt quá 100 ký tự")
    @Pattern(regexp = "^[a-zA-ZÀ-ỹ\\s\\.]*$", message = "Họ và tên chỉ được chứa chữ cái, khoảng trắng và dấu chấm")
    @Column(name = "full_name", length = 100)
    public String fullName;

    @Email(message = "Email không đúng định dạng")
    @Column(unique = true, name = "email")
    public String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = true, length = 20)
    public Role role = Role.USER;

    @Column(name = "is_locked", nullable = true)
    public boolean locked = false;

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_login_at")
    public LocalDateTime lastLoginAt;

    public enum Role {
        USER, ADMIN
    }
}
