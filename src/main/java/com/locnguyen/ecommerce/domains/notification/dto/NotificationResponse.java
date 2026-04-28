package com.locnguyen.ecommerce.domains.notification.dto;

import lombok.*;

import java.time.LocalDateTime;

import java.util.UUID;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private UUID id;
    private String type;
    private String title;
    private String body;
    private UUID referenceId;
    private String referenceType;
    private boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
