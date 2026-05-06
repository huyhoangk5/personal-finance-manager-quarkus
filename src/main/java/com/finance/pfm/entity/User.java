package com.finance.pfm.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    public Long userId;

    @Column(nullable = false, unique = true, name = "username")
    public String username;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(nullable = false, name = "password")
    public String password;

    @Column(name = "full_name")
    public String fullName;

    @Column(unique = true, name = "email")
    public String email;
}
