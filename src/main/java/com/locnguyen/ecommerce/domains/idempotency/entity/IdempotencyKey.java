package com.locnguyen.ecommerce.domains.idempotency.entity;

import com.locnguyen.ecommerce.domains.idempotency.enums.IdempotencyActionType;
import com.locnguyen.ecommerce.domains.idempotency.enums.IdempotencyStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks one idempotent operation identified by (userId, actionType, idempotencyKey).
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>PROCESSING — inserted at the start of the business action in its own transaction.</li>
 *   <li>COMPLETED — updated after the business action succeeds; stores resource reference.</li>
 *   <li>FAILED    — updated if the business action throws; stores error code for diagnosis.</li>
 * </ol>
 *
 * <p>PROCESSING records older than {@code app.idempotency.stale-processing-minutes} are
 * treated as stale (crashed request) and a new PROCESSING record is allowed.
 */
@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
@NoArgsConstructor
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "idempotency_key", length = 100, nullable = false)
    private String idempotencyKey;

    @Column(name = "user_id", length = 36)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", length = 50, nullable = false)
    private IdempotencyActionType actionType;

    /** SHA-256 hex of the stable request payload — used to detect same-key/different-body conflicts. */
    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "LONGTEXT")
    private String responseBody;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private IdempotencyStatus status;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isStaleProcessing(long staleMinutes) {
        return status == IdempotencyStatus.PROCESSING
                && createdAt != null
                && createdAt.isBefore(LocalDateTime.now().minusMinutes(staleMinutes));
    }
}
