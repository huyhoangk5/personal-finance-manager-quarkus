package com.finance.pfm.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long userId;

    @Column(nullable = false, unique = true)
    public String username;

    @Column(nullable = false)
    public String password;

    public String fullName;

    @Column(unique = true)
    public String email;
}
