package com.smartdelivery.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    private String eventId;
    private UUID userId;
    private UUID deliveryId;
    private Notification.NotificationType type;
    private Map<String, Object> data;
    private LocalDateTime timestamp;

    public static NotificationEvent fromDeliveryStatusChange(UUID userId, UUID deliveryId, 
                                                            Notification.NotificationType type, 
                                                            Map<String, Object> data) {
        return NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(userId)
                .deliveryId(deliveryId)
                .type(type)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
