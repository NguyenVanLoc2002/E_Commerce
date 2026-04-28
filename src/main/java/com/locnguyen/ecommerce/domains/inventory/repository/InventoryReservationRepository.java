package com.locnguyen.ecommerce.domains.inventory.repository;

import com.locnguyen.ecommerce.domains.inventory.entity.InventoryReservation;
import com.locnguyen.ecommerce.domains.inventory.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import java.util.UUID;
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {

    List<InventoryReservation> findByVariantIdAndStatus(UUID variantId, ReservationStatus status);

    List<InventoryReservation> findByReferenceTypeAndReferenceIdAndStatus(
            String referenceType, String referenceId, ReservationStatus status);

    Optional<InventoryReservation> findByReferenceTypeAndReferenceIdAndVariantIdAndStatus(
            String referenceType, String referenceId, UUID variantId, ReservationStatus status);

    /**
     * Find all PENDING reservations that have passed their expiration time.
     */
    @Query("SELECT r FROM InventoryReservation r " +
            "WHERE r.status = 'PENDING' AND r.expiresAt IS NOT NULL AND r.expiresAt < :now")
    List<InventoryReservation> findExpiredReservations(@Param("now") LocalDateTime now);

    /**
     * Mark expired reservations as EXPIRED.
     */
    @Modifying
    @Query("UPDATE InventoryReservation r SET r.status = 'EXPIRED' " +
            "WHERE r.status = 'PENDING' AND r.expiresAt IS NOT NULL AND r.expiresAt < :now")
    int expirePendingReservations(@Param("now") LocalDateTime now);
}
