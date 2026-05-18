package com.finance.pfm.dto;

import com.finance.pfm.entity.AuditLog;
import java.time.LocalDateTime;

public class AuditLogDTO {
    public Long logId;
    public Long userId;
    public String username;
    public String action;
    public String entityType;
    public Long entityId;
    public String description;
    public String ipAddress;
    public LocalDateTime createdAt;
    public boolean suspicious;
    public String suspiciousReason;

    public static AuditLogDTO from(AuditLog log) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.logId = log.logId;
        dto.userId = log.user != null ? log.user.userId : null;
        dto.username = log.user != null ? log.user.username : "unknown";
        dto.action = log.action;
        dto.entityType = log.entityType;
        dto.entityId = log.entityId;
        dto.description = log.description;
        dto.ipAddress = log.ipAddress;
        dto.createdAt = log.createdAt;
        dto.suspicious = log.suspicious;
        dto.suspiciousReason = log.suspiciousReason;
        return dto;
    }
}
