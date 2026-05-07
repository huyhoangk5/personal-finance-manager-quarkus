package com.finance.pfm.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
public class Transaction extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    public Long transactionId;

    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
    @DecimalMax(value = "1000000000000", message = "Số tiền không được vượt quá 1,000,000,000,000")
    @Column(name = "amount", nullable = false)
    public Double amount;

    @NotNull(message = "Ngày giao dịch không được để trống")
    @Column(name = "date", nullable = false)
    public LocalDate date;

    @Size(max = 255, message = "Ghi chú không được vượt quá 255 ký tự")
    @Column(name = "note", length = 255)
    public String note;

    @NotNull(message = "Loại giao dịch không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    public Category.TransactionType type;

    @NotNull(message = "Người dùng không được để trống")
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @NotNull(message = "Danh mục không được để trống")
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    public Category category;
}
