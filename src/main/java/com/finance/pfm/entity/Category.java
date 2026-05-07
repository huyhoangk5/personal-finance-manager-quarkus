package com.finance.pfm.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "categories")
public class Category extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    public Long categoryId;

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(min = 1, max = 50, message = "Tên danh mục phải từ 1-50 ký tự")
    @Column(nullable = false, name = "category_name", length = 50)
    public String categoryName;

    @NotNull(message = "Loại danh mục không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    public TransactionType type; // THU hoặc CHI

    @ManyToOne
    @JoinColumn(name = "user_id")
    public User user;

    public enum TransactionType {
        THU, CHI
    }
}
