package com.locnguyen.ecommerce.domains.notification.entity;

import com.locnguyen.ecommerce.common.auditing.BaseEntity;
import com.locnguyen.ecommerce.domains.customer.entity.Customer;
import com.locnguyen.ecommerce.domains.notification.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * In-app notification for a customer.
 *
 * <p>{@code referenceType} + {@code referenceId} form a soft polymorphic link
 * to the triggering entity (e.g. "ORDER"/"42") without a hard FK, keeping this
 * table decoupled from domain tables.
 *
 * <p>Extends {@link BaseEntity} — permanent record, never deleted.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 100, nullable = false)
    private NotificationType type;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    /** Entity type that triggered this notification, e.g. "ORDER", "REVIEW". */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    /** Entity identifier (ID or code) of the triggering entity. */
    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
