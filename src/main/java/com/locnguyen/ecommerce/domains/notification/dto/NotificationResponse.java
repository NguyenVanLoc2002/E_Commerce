package com.locnguyen.ecommerce.domains.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "In-app notification")
public class NotificationResponse {

    private final Long id;
    private final String type;
    private final String title;
    private final String message;
    private final String referenceType;
    private final String referenceId;
    private final boolean read;
    private final LocalDateTime readAt;
    private final LocalDateTime createdAt;
}
