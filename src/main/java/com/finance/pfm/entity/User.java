package com.finance.pfm.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "users")
public class User extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    public Long userId;

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 20, message = "Tên đăng nhập phải từ 3-20 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9]{3,20}$", message = "Tên đăng nhập chỉ được chứa chữ cái và số")
    @Column(nullable = false, unique = true, name = "username", length = 20)
    public String username;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @NotBlank(message = "Mật khẩu không được để trống")
    @Column(nullable = false, name = "password")
    public String password;

    @Size(max = 100, message = "Họ và tên không được vượt quá 100 ký tự")
    @Pattern(regexp = "^[a-zA-ZÀ-ỹ\\s]*$", message = "Họ và tên chỉ được chứa chữ cái và khoảng trắng")
    @Column(name = "full_name", length = 100)
    public String fullName;

    @Email(message = "Email không đúng định dạng")
    @Column(unique = true, name = "email")
    public String email;
}
