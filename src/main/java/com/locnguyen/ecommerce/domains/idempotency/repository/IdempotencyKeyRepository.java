package com.locnguyen.ecommerce.domains.idempotency.repository;

import com.locnguyen.ecommerce.domains.idempotency.entity.IdempotencyKey;
import com.locnguyen.ecommerce.domains.idempotency.enums.IdempotencyActionType;
import com.locnguyen.ecommerce.domains.idempotency.enums.IdempotencyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByUserIdAndActionTypeAndIdempotencyKey(
            UUID userId, IdempotencyActionType actionType, String idempotencyKey);

    @Modifying
    @Query("UPDATE IdempotencyKey i SET i.status = :status, i.resourceType = :resourceType, " +
           "i.resourceId = :resourceId, i.responseStatus = :responseStatus, " +
           "i.responseBody = :responseBody, i.updatedAt = :now WHERE i.id = :id")
    void markCompleted(
            @Param("id") Long id,
            @Param("status") IdempotencyStatus status,
            @Param("resourceType") String resourceType,
            @Param("resourceId") String resourceId,
            @Param("responseStatus") Integer responseStatus,
            @Param("responseBody") String responseBody,
            @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE IdempotencyKey i SET i.status = :status, i.errorCode = :errorCode, " +
           "i.updatedAt = :now WHERE i.id = :id")
    void markFailed(
            @Param("id") Long id,
            @Param("status") IdempotencyStatus status,
            @Param("errorCode") String errorCode,
            @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM IdempotencyKey i WHERE i.expiresAt < :cutoff")
    int deleteExpired(@Param("cutoff") LocalDateTime cutoff);
}
