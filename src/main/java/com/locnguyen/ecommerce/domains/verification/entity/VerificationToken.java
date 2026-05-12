package com.locnguyen.ecommerce.domains.verification.entity;

import com.locnguyen.ecommerce.domains.user.entity.User;
import com.locnguyen.ecommerce.domains.verification.enums.VerificationPurpose;
import com.locnguyen.ecommerce.domains.verification.enums.VerificationTargetType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One row per outstanding OTP / reset-token attempt.
 *
 * <p>The OTP value is stored as a SHA-256 hash in {@code tokenHash} (never plain).
 * After a successful OTP verification, an opaque reset token may be issued —
 * its SHA-256 hash is stored on the same row in {@code resetTokenHash}.
 *
 * <p>This entity is intentionally not a {@code SoftDeleteEntity}: verification
 * tokens have a short, predictable lifetime and are physically retained for
 * audit only until they are pruned by an out-of-band cleanup job.
 */
@Entity
@Table(name = "verification_tokens")
@Getter
@Setter
@NoArgsConstructor
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private VerificationTargetType targetType;

    @Column(name = "target", nullable = false, length = 255)
    private String target;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    private VerificationPurpose purpose;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "reset_token_hash", length = 255)
    private String resetTokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 5;

    @Column(name = "resend_count", nullable = false)
    private int resendCount = 0;

    @Column(name = "last_sent_at")
    private LocalDateTime lastSentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public boolean hasReachedMaxAttempts() {
        return attemptCount >= maxAttempts;
    }
}
