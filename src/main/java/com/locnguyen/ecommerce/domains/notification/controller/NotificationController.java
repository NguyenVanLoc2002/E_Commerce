package com.locnguyen.ecommerce.domains.notification.controller;

import com.locnguyen.ecommerce.common.constants.AppConstants;
import com.locnguyen.ecommerce.common.response.ApiResponse;
import com.locnguyen.ecommerce.common.response.PagedResponse;
import com.locnguyen.ecommerce.domains.notification.dto.NotificationResponse;
import com.locnguyen.ecommerce.domains.notification.service.NotificationService;
import com.locnguyen.ecommerce.domains.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Notifications", description = "In-app notification inbox for customers")
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@RequestMapping(AppConstants.API_V1 + "/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @Operation(summary = "Get my notifications (newest first)")
    @GetMapping
    public ApiResponse<PagedResponse<NotificationResponse>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(
                notificationService.getMyNotifications(userService.getCurrentCustomer(), pageable));
    }

    @Operation(summary = "Get unread notification count (for badge)")
    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> getUnreadCount() {
        long count = notificationService.countUnread(userService.getCurrentCustomer());
        return ApiResponse.success(Map.of("unreadCount", count));
    }

    @Operation(summary = "Mark a notification as read")
    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markAsRead(@PathVariable Long id) {
        return ApiResponse.success(
                notificationService.markAsRead(id, userService.getCurrentCustomer()));
    }

    @Operation(summary = "Mark all notifications as read")
    @PatchMapping("/read-all")
    public ApiResponse<Map<String, Integer>> markAllAsRead() {
        int updated = notificationService.markAllAsRead(userService.getCurrentCustomer());
        return ApiResponse.success(Map.of("updated", updated));
    }
}
