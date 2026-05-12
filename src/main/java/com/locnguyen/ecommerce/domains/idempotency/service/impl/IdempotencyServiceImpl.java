package com.locnguyen.ecommerce.domains.idempotency.service.impl;

import com.locnguyen.ecommerce.common.config.AppProperties;
import com.locnguyen.ecommerce.common.exception.AppException;
import com.locnguyen.ecommerce.common.exception.ErrorCode;
import com.locnguyen.ecommerce.domains.idempotency.entity.IdempotencyKey;
import com.locnguyen.ecommerce.domains.idempotency.enums.IdempotencyActionType;
import com.locnguyen.ecommerce.domains.idempotency.enums.IdempotencyStatus;
import com.locnguyen.ecommerce.domains.idempotency.repository.IdempotencyKeyRepository;
import com.locnguyen.ecommerce.domains.idempotency.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private static final int MAX_KEY_LENGTH = 100;

    /** Actions where a FAILED record allows an automatic retry with the same key. */
    private static final Set<IdempotencyActionType> RETRYABLE_ACTIONS = Set.of(
            IdempotencyActionType.CHECKOUT,
            IdempotencyActionType.PAYMENT_INITIATE
    );

    private final IdempotencyKeyRepository repository;
    private final AppProperties appProperties;

    // ─── Public API ──────────────────────────────────────────────────────────

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyKey findOrCreateProcessing(UUID userId, IdempotencyActionType actionType,
                                                 String key, String requestHash) {
        try {
            return insertProcessing(userId, actionType, key, requestHash);
        } catch (DataIntegrityViolationException dive) {
            // Concurrent request already inserted — reload and evaluate
            log.debug("Idempotency DIVE for userId={} action={} key={} — loading existing",
                    userId, actionType, key);
            return loadAndEvaluate(userId, actionType, key, requestHash);
        }
    }

    @Override
    @Transactional
    public void markComplete(Long id, String resourceType, String resourceId, int responseStatus) {
        repository.markCompleted(id, IdempotencyStatus.COMPLETED,
                resourceType, resourceId, responseStatus, null, LocalDateTime.now());
        log.debug("Idempotency COMPLETED: id={} resource={}/{}", id, resourceType, resourceId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long id, String errorCode) {
        try {
            repository.markFailed(id, IdempotencyStatus.FAILED, errorCode, LocalDateTime.now());
            log.debug("Idempotency FAILED: id={} errorCode={}", id, errorCode);
        } catch (Exception e) {
            // Best-effort — never mask the original business exception
            log.warn("Failed to mark idempotency record {} as FAILED: {}", id, e.getMessage());
        }
    }

    @Override
    public String validateKey(String rawHeader) {
        if (!StringUtils.hasText(rawHeader)) {
            throw new AppException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }
        String key = rawHeader.trim();
        if (key.length() > MAX_KEY_LENGTH) {
            throw new AppException(ErrorCode.IDEMPOTENCY_KEY_TOO_LONG);
        }
        return key;
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private IdempotencyKey insertProcessing(UUID userId, IdempotencyActionType actionType,
                                            String key, String requestHash) {
        IdempotencyKey record = new IdempotencyKey();
        record.setUserId(userId);
        record.setActionType(actionType);
        record.setIdempotencyKey(key);
        record.setRequestHash(requestHash);
        record.setStatus(IdempotencyStatus.PROCESSING);
        record.setExpiresAt(LocalDateTime.now().plusHours(
                appProperties.getIdempotency().getTtlHours()));
        return repository.save(record);
    }

    private IdempotencyKey loadAndEvaluate(UUID userId, IdempotencyActionType actionType,
                                           String key, String requestHash) {
        IdempotencyKey existing = repository
                .findByUserIdAndActionTypeAndIdempotencyKey(userId, actionType, key)
                .orElseThrow(() -> new AppException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT,
                        "Idempotency key conflict — concurrent requests with conflicting context"));

        // Hash check first — different payload always rejected regardless of status
        if (existing.getRequestHash() != null && !existing.getRequestHash().equals(requestHash)) {
            throw new AppException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT,
                    "Idempotency-Key was submitted with a different request payload");
        }

        return switch (existing.getStatus()) {
            case COMPLETED -> existing; // caller uses resourceId to replay

            case PROCESSING -> {
                long staleMinutes = appProperties.getIdempotency().getStaleProcessingMinutes();
                if (existing.isStaleProcessing(staleMinutes)) {
                    // Previous request crashed — replace stale record with fresh PROCESSING
                    log.warn("Replacing stale PROCESSING idempotency record: id={} key={}",
                            existing.getId(), key);
                    repository.delete(existing);
                    repository.flush();
                    yield insertProcessing(userId, actionType, key, requestHash);
                }
                throw new AppException(ErrorCode.IDEMPOTENCY_REQUEST_IN_PROGRESS,
                        "A request with this Idempotency-Key is already being processed");
            }

            case FAILED -> {
                if (RETRYABLE_ACTIONS.contains(actionType)) {
                    // Allow retry: replace the failed record with fresh PROCESSING
                    log.info("Retrying FAILED idempotency action={} key={}", actionType, key);
                    repository.delete(existing);
                    repository.flush();
                    yield insertProcessing(userId, actionType, key, requestHash);
                }
                throw new AppException(ErrorCode.IDEMPOTENCY_REPLAY_NOT_AVAILABLE,
                        "Previous attempt failed with: " + existing.getErrorCode());
            }
        };
    }
}
