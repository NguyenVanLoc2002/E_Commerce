package com.locnguyen.ecommerce.domains.auditlog.service.impl;

import com.locnguyen.ecommerce.common.constants.AppConstants;
import com.locnguyen.ecommerce.common.exception.AppException;
import com.locnguyen.ecommerce.common.exception.ErrorCode;
import com.locnguyen.ecommerce.common.response.PagedResponse;
import com.locnguyen.ecommerce.common.utils.SecurityUtils;
import com.locnguyen.ecommerce.domains.auditlog.dto.AuditLogFilter;
import com.locnguyen.ecommerce.domains.auditlog.dto.AuditLogResponse;
import com.locnguyen.ecommerce.domains.auditlog.entity.AuditLog;
import com.locnguyen.ecommerce.domains.auditlog.enums.AuditAction;
import com.locnguyen.ecommerce.domains.auditlog.repository.AuditLogRepository;
import com.locnguyen.ecommerce.domains.auditlog.service.AuditLogService;
import com.locnguyen.ecommerce.domains.auditlog.specification.AuditLogSpecification;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Async("auditLogExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditAction action, String entityType, String entityId, String details) {
        try {
            AuditLog entry = new AuditLog();
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId != null ? entityId : "unknown");
            entry.setActor(SecurityUtils.getCurrentUsernameOrSystem());
            entry.setIpAddress(resolveClientIp());
            entry.setRequestId(MDC.get(AppConstants.MDC_REQUEST_ID));
            entry.setDetails(details);
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            log.error("Failed to persist audit log: action={} entityType={} entityId={} — {}",
                    action, entityType, entityId, ex.getMessage());
        }
    }

    @Override
    @Async("auditLogExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditAction action, String entityType, String entityId) {
        log(action, entityType, entityId, null);
    }

    @Override
    @Async("auditLogExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logWithActor(AuditAction action, String entityType, String entityId,
                             String actor, String details) {
        try {
            AuditLog entry = new AuditLog();
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId != null ? entityId : "unknown");
            entry.setActor(actor != null ? actor : AppConstants.SYSTEM_USER);
            entry.setIpAddress(resolveClientIp());
            entry.setRequestId(MDC.get(AppConstants.MDC_REQUEST_ID));
            entry.setDetails(details);
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            log.error("Failed to persist audit log (explicit actor): action={} actor={} — {}",
                    action, actor, ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> getAuditLogs(AuditLogFilter filter, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findAll(
                AuditLogSpecification.withFilter(filter), pageable);
        return PagedResponse.of(page.map(AuditLogResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLogById(UUID id) {
        return auditLogRepository.findById(id)
                .map(AuditLogResponse::from)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Audit log not found: id=" + id));
    }

    private String resolveClientIp() {
        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (!(attrs instanceof ServletRequestAttributes servletAttrs)) {
                return null;
            }
            HttpServletRequest req = servletAttrs.getRequest();

            String[] headers = {
                    "X-Forwarded-For",
                    "X-Real-IP",
                    "Proxy-Client-IP",
                    "WL-Proxy-Client-IP"
            };
            for (String header : headers) {
                String value = req.getHeader(header);
                if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                    return value.split(",")[0].trim();
                }
            }
            return req.getRemoteAddr();
        } catch (Exception ex) {
            return null;
        }
    }
}
