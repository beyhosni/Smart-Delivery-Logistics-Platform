package com.smartdelivery.notification.controller;

import com.smartdelivery.notification.model.Notification;
import com.smartdelivery.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<Notification>> getAllNotifications() {
        log.info("Getting all notifications");
        List<Notification> notifications = notificationService.getAllNotifications();
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getUserNotifications(@PathVariable UUID userId) {
        log.info("Getting notifications for user ID: {}", userId);
        List<Notification> notifications = notificationService.getNotificationsByUserId(userId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread/{userId}")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@PathVariable UUID userId) {
        log.info("Getting unread notifications for user ID: {}", userId);
        List<Notification> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/delivery/{deliveryId}")
    public ResponseEntity<List<Notification>> getDeliveryNotifications(@PathVariable UUID deliveryId) {
        log.info("Getting notifications for delivery ID: {}", deliveryId);
        List<Notification> notifications = notificationService.getNotificationsByDeliveryId(deliveryId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<Notification> getNotificationById(@PathVariable UUID notificationId) {
        log.info("Getting notification with ID: {}", notificationId);
        // Note: Dans un vrai projet, nous aurions une méthode dédiée dans le service
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Notification> createNotification(@RequestBody Notification notification) {
        log.info("Creating notification");
        Notification createdNotification = notificationService.createAndSendNotification(
                notification.getUserId(),
                notification.getDeliveryId(),
                notification.getType(),
                notification.getChannel(),
                notification.getRecipient(),
                notification.getSubject(),
                notification.getContent()
        );
        return ResponseEntity.ok(createdNotification);
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable UUID notificationId) {
        log.info("Marking notification as read with ID: {}", notificationId);
        Notification updatedNotification = notificationService.updateNotificationStatus(
                notificationId, 
                Notification.NotificationStatus.READ, 
                null
        );
        return ResponseEntity.ok(updatedNotification);
    }

    @PutMapping("/read-all/{userId}")
    public ResponseEntity<List<Notification>> markAllAsRead(@PathVariable UUID userId) {
        log.info("Marking all notifications as read for user ID: {}", userId);

        List<Notification> unreadNotifications = notificationService.getUnreadNotifications(userId);
        unreadNotifications.forEach(notification -> {
            notificationService.updateNotificationStatus(
                    notification.getId(), 
                    Notification.NotificationStatus.READ, 
                    null
            );
        });

        return ResponseEntity.ok(unreadNotifications);
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable UUID notificationId) {
        log.info("Deleting notification with ID: {}", notificationId);
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/process/{channel}")
    public ResponseEntity<String> processPendingNotifications(@PathVariable Notification.NotificationChannel channel) {
        log.info("Processing pending notifications for channel: {}", channel);

        try {
            notificationService.processPendingNotifications(channel);
            return ResponseEntity.ok("Notifications processed successfully");
        } catch (Exception e) {
            log.error("Error processing notifications", e);
            return ResponseEntity.badRequest().body("Error processing notifications: " + e.getMessage());
        }
    }
}
