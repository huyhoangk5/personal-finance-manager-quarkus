package com.finance.pfm.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long categoryId;

    @Column(nullable = false)
    public String categoryName;

    @Enumerated(EnumType.STRING)
    public TransactionType type; // THU hoặc CHI

    public enum TransactionType {
        THU, CHI
    }
}
