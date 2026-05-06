package com.finance.pfm.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
public class Transaction extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    public Long transactionId;

    @Column(name = "amount")
    public Double amount;

    @Column(name = "date")
    public LocalDate date;

    @Column(name = "note")
    public String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    public Category.TransactionType type;

    @ManyToOne
    @JoinColumn(name = "user_id")
    public User user;

    @ManyToOne
    @JoinColumn(name = "category_id")
    public Category category;
}
