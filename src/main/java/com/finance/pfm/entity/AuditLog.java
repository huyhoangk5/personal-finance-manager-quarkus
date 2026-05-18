package com.finance.pfm.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    public Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public User user;

    @Column(name = "action", nullable = false, length = 100)
    public String action; // LOGIN, LOGOUT, CREATE_TRANSACTION, DELETE_TRANSACTION, etc.

    @Column(name = "entity_type", length = 50)
    public String entityType; // TRANSACTION, CATEGORY, BUDGET, USER

    @Column(name = "entity_id")
    public Long entityId;

    @Column(name = "description", length = 500)
    public String description;

    @Column(name = "ip_address", length = 50)
    public String ipAddress;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_suspicious", nullable = false)
    public boolean suspicious = false;

    @Column(name = "suspicious_reason", length = 255)
    public String suspiciousReason;
}
