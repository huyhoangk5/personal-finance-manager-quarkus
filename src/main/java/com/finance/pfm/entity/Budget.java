package com.finance.pfm.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "budgets")
public class Budget extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "budget_id")
    public Long budgetId;

    @Column(name = "month")
    public String month; // Định dạng YYYY-MM

    @Column(name = "total_limit")
    public Double totalLimit;

    @Column(name = "category_limit")
    public Double categoryLimit;

    @ManyToOne
    @JoinColumn(name = "user_id")
    public User user;

    @ManyToOne
    @JoinColumn(name = "category_id")
    public Category category;
}
