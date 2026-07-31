package com.finance.pfm.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_chat_messages")
public class AiChatMessage extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "session_id", nullable = false)
    public String sessionId;

    /**
     * "USER" hoặc "AI" — phân loại tin nhắn
     */
    @Column(name = "role", nullable = false, length = 10)
    public String role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    public String content;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;
}
