package com.finance.pfm.repository;

import com.finance.pfm.entity.AuditLog;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class AuditLogRepository implements PanacheRepository<AuditLog> {

    public List<AuditLog> findByUserId(Long userId) {
        return list("user.userId = ?1 order by createdAt desc", userId);
    }

    public List<AuditLog> findRecent(int limit) {
        return find("order by createdAt desc").page(0, limit).list();
    }

    public List<AuditLog> findSuspicious() {
        return list("suspicious = true order by createdAt desc");
    }

    public List<AuditLog> findByAction(String action) {
        return list("action = ?1 order by createdAt desc", action);
    }

    public List<AuditLog> findByDateRange(LocalDateTime from, LocalDateTime to) {
        return list("createdAt >= ?1 and createdAt <= ?2 order by createdAt desc", from, to);
    }

    public long countByAction(String action) {
        return count("action", action);
    }

    public long countSuspicious() {
        return count("suspicious", true);
    }
}
