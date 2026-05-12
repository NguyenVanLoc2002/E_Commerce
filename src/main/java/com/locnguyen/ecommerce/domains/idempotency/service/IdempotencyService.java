package com.locnguyen.ecommerce.domains.idempotency.service;

import com.locnguyen.ecommerce.domains.idempotency.entity.IdempotencyKey;
import com.locnguyen.ecommerce.domains.idempotency.enums.IdempotencyActionType;

import java.util.UUID;

public interface IdempotencyService {

    /**
     * Find an existing idempotency record or create a new PROCESSING one.
     *
     * <p>Runs in its own {@code REQUIRES_NEW} transaction so the PROCESSING
     * record is immediately visible to concurrent requests before the caller's
     * business transaction completes.
     *
     * <p>Behavior by existing record status:
     * <ul>
     *   <li>No record → insert PROCESSING, return it</li>
     *   <li>COMPLETED + same hash → return for replay (caller must use resourceId)</li>
     *   <li>COMPLETED + different hash → throw {@code IDEMPOTENCY_KEY_CONFLICT}</li>
     *   <li>PROCESSING + same hash + recent → throw {@code IDEMPOTENCY_REQUEST_IN_PROGRESS}</li>
     *   <li>PROCESSING + same hash + stale → delete old, insert new PROCESSING</li>
     *   <li>PROCESSING + different hash → throw {@code IDEMPOTENCY_KEY_CONFLICT}</li>
     *   <li>FAILED + same hash + retryable action → insert new PROCESSING</li>
     *   <li>FAILED + same hash + non-retryable → throw {@code IDEMPOTENCY_REPLAY_NOT_AVAILABLE}</li>
     *   <li>FAILED + different hash → throw {@code IDEMPOTENCY_KEY_CONFLICT}</li>
     *   <li>Concurrent insert race (DIVE) → reloads and applies same rules</li>
     * </ul>
     *
     * @param userId      nullable for gateway callbacks; required for user-facing actions
     * @param actionType  the business action being protected
     * @param key         normalized (trimmed) idempotency key from the client header
     * @param requestHash SHA-256 of the stable request payload (from {@code RequestHashUtils})
     * @return the IdempotencyKey record (status may be PROCESSING or COMPLETED)
     */
    IdempotencyKey findOrCreateProcessing(UUID userId, IdempotencyActionType actionType,
                                          String key, String requestHash);

    /**
     * Mark the record COMPLETED with a resource reference.
     *
     * <p>Called inside the same transaction as the business action — committed atomically.
     */
    void markComplete(Long id, String resourceType, String resourceId, int responseStatus);

    /**
     * Mark the record FAILED with the business error code.
     *
     * <p>Runs in {@code REQUIRES_NEW} so it commits even if the caller's transaction
     * is rolling back. Best-effort — a failure here is logged but not re-thrown.
     */
    void markFailed(Long id, String errorCode);

    /**
     * Validate and normalize a required idempotency key from an HTTP header value.
     *
     * <p>Throws {@code IDEMPOTENCY_KEY_REQUIRED} when the header is blank and
     * {@code IDEMPOTENCY_KEY_TOO_LONG} when it exceeds 100 characters.
     */
    String validateKey(String rawHeader);
}
