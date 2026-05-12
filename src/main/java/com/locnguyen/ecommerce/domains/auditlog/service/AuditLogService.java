package com.locnguyen.ecommerce.domains.auditlog.service;

import com.locnguyen.ecommerce.common.response.PagedResponse;
import com.locnguyen.ecommerce.domains.auditlog.dto.AuditLogFilter;
import com.locnguyen.ecommerce.domains.auditlog.dto.AuditLogResponse;
import com.locnguyen.ecommerce.domains.auditlog.enums.AuditAction;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AuditLogService {

    void log(AuditAction action, String entityType, String entityId, String details);

    void log(AuditAction action, String entityType, String entityId);

    void logWithActor(AuditAction action, String entityType, String entityId,
                      String actor, String details);

    PagedResponse<AuditLogResponse> getAuditLogs(AuditLogFilter filter, Pageable pageable);

    AuditLogResponse getAuditLogById(UUID id);
}
