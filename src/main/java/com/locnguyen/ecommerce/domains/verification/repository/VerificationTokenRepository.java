package com.locnguyen.ecommerce.domains.verification.repository;

import com.locnguyen.ecommerce.domains.verification.entity.VerificationToken;
import com.locnguyen.ecommerce.domains.verification.enums.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    /**
     * Find a still-active token (not used, not expired) for the given target / purpose.
     * Most recent first to handle concurrent issuance gracefully.
     */
    @Query("""
            select v from VerificationToken v
             where v.target = :target
               and v.purpose = :purpose
               and v.usedAt is null
               and v.expiresAt > :now
             order by v.createdAt desc
            """)
    List<VerificationToken> findActiveByTargetAndPurpose(
            @Param("target") String target,
            @Param("purpose") VerificationPurpose purpose,
            @Param("now") LocalDateTime now);

    Optional<VerificationToken> findByResetTokenHash(String resetTokenHash);

    /**
     * Mark all previous active tokens for the same target/purpose as used.
     * Called before issuing a new OTP to ensure only one active token at a time.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update VerificationToken v
               set v.usedAt = :now,
                   v.updatedAt = :now
             where v.target = :target
               and v.purpose = :purpose
               and v.usedAt is null
               and v.expiresAt > :now
            """)
    int markPreviousSuperseded(
            @Param("target") String target,
            @Param("purpose") VerificationPurpose purpose,
            @Param("now") LocalDateTime now);
}
