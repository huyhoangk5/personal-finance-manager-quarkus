package com.finance.pfm.service;

import com.finance.pfm.dto.AuditLogDTO;
import com.finance.pfm.entity.AuditLog;
import com.finance.pfm.entity.User;
import com.finance.pfm.repository.AuditLogRepository;
import com.finance.pfm.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class AuditLogService {

    private static final Logger LOG = Logger.getLogger(AuditLogService.class);

    // Ngưỡng phát hiện giao dịch bất thường (100 triệu VND)
    private static final double SUSPICIOUS_AMOUNT_THRESHOLD = 100_000_000.0;
    // Số lần login thất bại tối đa trước khi đánh dấu suspicious
    private static final int MAX_FAILED_LOGINS = 5;

    @Inject
    AuditLogRepository auditLogRepository;

    @Inject
    UserRepository userRepository;

    @Transactional
    public void log(Long userId, String action, String entityType, Long entityId,
                    String description, String ipAddress) {
        try {
            AuditLog log = new AuditLog();
            if (userId != null) {
                log.user = userRepository.findById(userId);
            }
            log.action = action;
            log.entityType = entityType;
            log.entityId = entityId;
            log.description = description;
            log.ipAddress = ipAddress;
            log.createdAt = LocalDateTime.now();
            auditLogRepository.persist(log);
        } catch (Exception e) {
            LOG.warnf("Không thể ghi audit log: %s", e.getMessage());
        }
    }

    @Transactional
    public void logSuspicious(Long userId, String action, String description,
                               String ipAddress, String reason) {
        try {
            AuditLog log = new AuditLog();
            if (userId != null) {
                log.user = userRepository.findById(userId);
            }
            log.action = action;
            log.description = description;
            log.ipAddress = ipAddress;
            log.suspicious = true;
            log.suspiciousReason = reason;
            log.createdAt = LocalDateTime.now();
            auditLogRepository.persist(log);
        } catch (Exception e) {
            LOG.warnf("Không thể ghi suspicious log: %s", e.getMessage());
        }
    }

    /**
     * Kiểm tra giao dịch có bất thường không (số tiền quá lớn).
     */
    @Transactional
    public void checkAndLogTransaction(Long userId, Double amount, String ipAddress) {
        if (amount != null && amount > SUSPICIOUS_AMOUNT_THRESHOLD) {
            logSuspicious(userId, "LARGE_TRANSACTION",
                    "Giao dịch lớn bất thường: " + String.format("%.0f", amount) + " VND",
                    ipAddress,
                    "Số tiền vượt ngưỡng " + String.format("%.0f", SUSPICIOUS_AMOUNT_THRESHOLD) + " VND");
        }
    }

    /**
     * Kiểm tra login thất bại nhiều lần từ cùng IP.
     */
    @Transactional
    public void checkFailedLogins(String ipAddress) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(30);
        List<AuditLog> recentFails = auditLogRepository.findByDateRange(since, LocalDateTime.now())
                .stream()
                .filter(l -> "LOGIN_FAILED".equals(l.action)
                        && ipAddress.equals(l.ipAddress))
                .collect(Collectors.toList());

        if (recentFails.size() >= MAX_FAILED_LOGINS) {
            logSuspicious(null, "BRUTE_FORCE_DETECTED",
                    "Phát hiện brute force từ IP: " + ipAddress,
                    ipAddress,
                    recentFails.size() + " lần đăng nhập thất bại trong 30 phút");
        }
    }

    public List<AuditLogDTO> getRecentLogs(int limit) {
        return auditLogRepository.findRecent(limit)
                .stream().map(AuditLogDTO::from).collect(Collectors.toList());
    }

    public List<AuditLogDTO> getSuspiciousLogs() {
        return auditLogRepository.findSuspicious()
                .stream().map(AuditLogDTO::from).collect(Collectors.toList());
    }

    public List<AuditLogDTO> getLogsByUser(Long userId) {
        return auditLogRepository.findByUserId(userId)
                .stream().map(AuditLogDTO::from).collect(Collectors.toList());
    }

    @Transactional
    public void markSafe(Long logId) {
        AuditLog log = auditLogRepository.findById(logId);
        if (log != null) {
            log.suspicious = false;
            log.suspiciousReason = "Đã xác nhận an toàn bởi Admin";
        }
    }
}
