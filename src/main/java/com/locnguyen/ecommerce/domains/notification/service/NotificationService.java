package com.locnguyen.ecommerce.domains.notification.service;

import com.locnguyen.ecommerce.common.exception.AppException;
import com.locnguyen.ecommerce.common.exception.ErrorCode;
import com.locnguyen.ecommerce.common.response.PagedResponse;
import com.locnguyen.ecommerce.domains.customer.entity.Customer;
import com.locnguyen.ecommerce.domains.customer.repository.CustomerRepository;
import com.locnguyen.ecommerce.domains.notification.dto.BroadcastNotificationRequest;
import com.locnguyen.ecommerce.domains.notification.dto.NotificationResponse;
import com.locnguyen.ecommerce.domains.notification.entity.Notification;
import com.locnguyen.ecommerce.domains.notification.enums.NotificationType;
import com.locnguyen.ecommerce.domains.notification.mapper.NotificationMapper;
import com.locnguyen.ecommerce.domains.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CustomerRepository customerRepository;
    private final NotificationMapper notificationMapper;

    // ─── Internal creation ────────────────────────────────────────────────────

    /**
     * Create an in-app notification for a single customer.
     * Called internally by other services (ReviewService, OrderService, etc.).
     * Never throws — failures are logged and swallowed so they don't break the caller's transaction.
     */
    @Transactional
    public void send(Long customerId, NotificationType type, String title, String message,
                     String referenceType, String referenceId) {
        try {
            Customer customer = customerRepository.findById(customerId).orElse(null);
            if (customer == null) {
                log.warn("Notification skipped — customer not found: id={}", customerId);
                return;
            }
            Notification notification = buildNotification(
                    customer, type, title, message, referenceType, referenceId);
            notificationRepository.save(notification);
            log.debug("Notification sent: customerId={} type={}", customerId, type);
        } catch (Exception e) {
            log.error("Failed to send notification: customerId={} type={} error={}",
                    customerId, type, e.getMessage());
        }
    }

    /** Convenience overload without a reference entity. */
    @Transactional
    public void send(Long customerId, NotificationType type, String title, String message) {
        send(customerId, type, title, message, null, null);
    }

    // ─── Admin broadcast ──────────────────────────────────────────────────────

    /**
     * Send a notification to a targeted list of customers, or to all customers
     * when {@code request.customerIds} is null or empty.
     *
     * <p>Each customer gets an individual {@link Notification} row so they can
     * mark it read independently.
     *
     * @return number of notifications created
     */
    @Transactional
    public int broadcast(BroadcastNotificationRequest request) {
        List<Customer> targets;

        if (request.getCustomerIds() == null || request.getCustomerIds().isEmpty()) {
            targets = customerRepository.findAll();
            log.info("Broadcasting notification to all {} customers", targets.size());
        } else {
            targets = customerRepository.findAllById(request.getCustomerIds());
            log.info("Broadcasting notification to {} targeted customers", targets.size());
        }

        List<Notification> notifications = targets.stream()
                .map(c -> buildNotification(c, request.getType(), request.getTitle(),
                        request.getMessage(), request.getReferenceType(),
                        request.getReferenceId()))
                .toList();

        notificationRepository.saveAll(notifications);
        return notifications.size();
    }

    // ─── Customer read operations ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getMyNotifications(Customer customer,
                                                                   Pageable pageable) {
        Page<Notification> page = notificationRepository
                .findByCustomerIdOrderByCreatedAtDesc(customer.getId(), pageable);
        return PagedResponse.of(page.map(notificationMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public long countUnread(Customer customer) {
        return notificationRepository.countByCustomerIdAndReadFalse(customer.getId());
    }

    /**
     * Mark a single notification as read.
     * Ownership check: throws {@code NOTIFICATION_NOT_FOUND} if the notification
     * belongs to another customer (don't reveal it exists).
     */
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Customer customer) {
        Notification notification = findByIdOrThrow(notificationId);

        if (!notification.getCustomer().getId().equals(customer.getId())) {
            throw new AppException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
        return notificationMapper.toResponse(notification);
    }

    /**
     * Mark all unread notifications as read for the current customer.
     *
     * @return number of notifications updated
     */
    @Transactional
    public int markAllAsRead(Customer customer) {
        int updated = notificationRepository.markAllReadByCustomerId(customer.getId());
        log.debug("Marked {} notifications as read for customerId={}", updated, customer.getId());
        return updated;
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private Notification findByIdOrThrow(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));
    }

    private Notification buildNotification(Customer customer, NotificationType type,
                                            String title, String message,
                                            String referenceType, String referenceId) {
        Notification n = new Notification();
        n.setCustomer(customer);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setReferenceType(referenceType);
        n.setReferenceId(referenceId);
        return n;
    }
}
