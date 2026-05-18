package com.finance.pfm.service;

import com.finance.pfm.dto.AuditLogDTO;
import com.finance.pfm.entity.AuditLog;
import com.finance.pfm.entity.Category;
import com.finance.pfm.entity.Transaction;
import com.finance.pfm.entity.User;
import com.finance.pfm.repository.AuditLogRepository;
import com.finance.pfm.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.*;

@ApplicationScoped
public class AdminService {

    @Inject
    EntityManager em;

    @Inject
    AuditLogRepository auditLogRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    AuditLogService auditLogService;

    /**
     * Lấy thống kê toàn hệ thống cho Dashboard Admin.
     */
    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 1. Tổng số user
        long totalUsers = User.count();
        stats.put("totalUsers", totalUsers);

        // 2. Tổng số transactions
        long totalTransactions = Transaction.count();
        stats.put("totalTransactions", totalTransactions);

        // 3. Tổng chi tiêu hệ thống (CHI)
        Double totalSystemSpend = em.createQuery(
                "SELECT SUM(t.amount) FROM Transaction t WHERE t.type = :type", Double.class)
                .setParameter("type", Category.TransactionType.CHI)
                .getSingleResult();
        stats.put("totalSystemSpend", totalSystemSpend != null ? totalSystemSpend : 0.0);

        // 4. Top categories được dùng nhiều nhất (theo số lượng giao dịch)
        List<Object[]> topCatsRaw = em.createQuery(
                "SELECT t.category.categoryName, COUNT(t) FROM Transaction t GROUP BY t.category.categoryName ORDER BY COUNT(t) DESC", Object[].class)
                .setMaxResults(5)
                .getResultList();
        List<Map<String, Object>> topCategories = new ArrayList<>();
        for (Object[] row : topCatsRaw) {
            Map<String, Object> item = new HashMap<>();
            item.put("categoryName", row[0]);
            item.put("count", row[1]);
            topCategories.add(item);
        }
        stats.put("topCategories", topCategories);

        // 5. User hoạt động tích cực nhất (theo số lượng giao dịch)
        List<Object[]> activeUsersRaw = em.createQuery(
                "SELECT t.user.username, COUNT(t) FROM Transaction t GROUP BY t.user.username ORDER BY COUNT(t) DESC", Object[].class)
                .setMaxResults(5)
                .getResultList();
        List<Map<String, Object>> mostActiveUsers = new ArrayList<>();
        for (Object[] row : activeUsersRaw) {
            Map<String, Object> item = new HashMap<>();
            item.put("username", row[0]);
            item.put("transactionCount", row[1]);
            mostActiveUsers.add(item);
        }
        stats.put("mostActiveUsers", mostActiveUsers);

        return stats;
    }

    /**
     * Lấy danh sách audit logs (mặc định lấy 100 log gần nhất).
     */
    public List<AuditLogDTO> getAuditLogs(int limit) {
        return auditLogService.getRecentLogs(limit);
    }

    /**
     * Lấy danh sách cảnh báo nghi vấn/fraud (suspicious = true).
     */
    public List<AuditLogDTO> getAlerts() {
        return auditLogService.getSuspiciousLogs();
    }

    /**
     * Đánh dấu cảnh báo là an toàn.
     */
    @Transactional
    public void markAlertSafe(Long logId) {
        auditLogService.markSafe(logId);
    }
}
