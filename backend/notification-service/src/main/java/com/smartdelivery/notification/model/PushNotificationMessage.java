package com.smartdelivery.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushNotificationMessage {
    private String title;
    private String body;
    private String icon;
    private String clickAction;
    private Object data;
    private String to; // Token FCM ou topic
    private String sound = "default";
    private int priority = 10; // Priorité haute
    private int ttl = 3600; // Durée de vie en secondes (1 heure)
}
