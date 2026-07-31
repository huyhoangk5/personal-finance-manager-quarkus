package com.finance.pfm.repository;

import com.finance.pfm.entity.AiChatMessage;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class AiChatMessageRepository implements PanacheRepository<AiChatMessage> {

    /**
     * Lấy N tin nhắn gần nhất của 1 sessionId, sắp xếp theo thứ tự thời gian.
     */
    public List<AiChatMessage> findRecentBySessionId(String sessionId, int limit) {
        return find("sessionId = ?1 order by createdAt asc", sessionId)
                .page(0, limit)
                .list();
    }

    /**
     * Lấy toàn bộ lịch sử của 1 session.
     */
    public List<AiChatMessage> findBySessionId(String sessionId) {
        return list("sessionId = ?1 order by createdAt asc", sessionId);
    }

    /**
     * Lấy danh sách session_id duy nhất của một user.
     */
    public List<String> findDistinctSessionsByUserId(Long userId) {
        return find("SELECT DISTINCT m.sessionId FROM AiChatMessage m WHERE m.user.userId = ?1 ORDER BY m.sessionId DESC", userId)
                .project(String.class)
                .list();
    }
}
