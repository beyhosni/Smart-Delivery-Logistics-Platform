package com.smartdelivery.notification.controller;

import com.smartdelivery.notification.model.NotificationPreference;
import com.smartdelivery.notification.service.NotificationPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notification-preferences")
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationPreference>> getUserPreferences(@PathVariable UUID userId) {
        log.info("Getting notification preferences for user ID: {}", userId);
        List<NotificationPreference> preferences = preferenceService.getUserPreferences(userId);
        return ResponseEntity.ok(preferences);
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<NotificationPreference> updateNotificationPreference(
            @PathVariable UUID userId,
            @RequestParam NotificationPreference.NotificationType type,
            @RequestParam(required = false) Boolean emailEnabled,
            @RequestParam(required = false) Boolean smsEnabled,
            @RequestParam(required = false) Boolean pushEnabled,
            @RequestParam(required = false) Boolean inAppEnabled) {

        log.info("Updating notification preference for user ID: {} and type: {}", userId, type);

        NotificationPreference preference = preferenceService.updateUserPreference(
                userId, type, emailEnabled, smsEnabled, pushEnabled, inAppEnabled);

        return ResponseEntity.ok(preference);
    }

    @PostMapping("/user/{userId}/quiet-hours")
    public ResponseEntity<NotificationPreference> updateQuietHours(
            @PathVariable UUID userId,
            @RequestParam(required = false) Boolean quietHoursEnabled,
            @RequestParam(required = false) Integer quietHoursStart,
            @RequestParam(required = false) Integer quietHoursEnd) {

        log.info("Updating quiet hours for user ID: {}", userId);

        NotificationPreference preference = preferenceService.updateQuietHours(
                userId, quietHoursEnabled, quietHoursStart, quietHoursEnd);

        return ResponseEntity.ok(preference);
    }

    @PostMapping("/user/{userId}/defaults")
    public ResponseEntity<String> createDefaultPreferences(@PathVariable UUID userId) {
        log.info("Creating default notification preferences for user ID: {}", userId);

        preferenceService.createDefaultPreferences(userId);

        return ResponseEntity.ok("Default preferences created successfully");
    }

    @GetMapping("/user/{userId}/is-enabled")
    public ResponseEntity<Boolean> isNotificationEnabled(
            @PathVariable UUID userId,
            @RequestParam NotificationPreference.NotificationType type,
            @RequestParam NotificationPreference.NotificationChannel channel) {

        log.info("Checking if notification is enabled for user ID: {}, type: {}, channel: {}", 
                userId, type, channel);

        boolean enabled = preferenceService.isNotificationEnabled(userId, type, channel);

        return ResponseEntity.ok(enabled);
    }

    @GetMapping("/user/{userId}/is-quiet-hours")
    public ResponseEntity<Boolean> isInQuietHours(@PathVariable UUID userId) {
        log.info("Checking if user ID: {} is in quiet hours", userId);

        boolean inQuietHours = preferenceService.isInQuietHours(userId);

        return ResponseEntity.ok(inQuietHours);
    }
}
