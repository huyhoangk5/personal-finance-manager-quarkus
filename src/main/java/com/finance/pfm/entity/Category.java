package com.finance.pfm.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    public Long categoryId;

    @Column(nullable = false, name = "category_name")
    public String categoryName;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    public TransactionType type; // THU hoặc CHI

    public enum TransactionType {
        THU, CHI
    }
}
