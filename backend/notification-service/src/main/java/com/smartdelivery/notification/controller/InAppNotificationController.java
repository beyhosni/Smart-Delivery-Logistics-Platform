package com.smartdelivery.notification.controller;

import com.smartdelivery.notification.model.Notification;
import com.smartdelivery.notification.service.InAppNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/in-app-notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "In-App Notifications API", description = "API pour la gestion des notifications in-app")
public class InAppNotificationController {

    private final InAppNotificationService inAppNotificationService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Récupérer les notifications d'un utilisateur", 
               description = "Récupère les notifications paginées pour un utilisateur spécifique")
    public ResponseEntity<Page<Notification>> getUserNotifications(
            @Parameter(description = "ID de l'utilisateur") @PathVariable UUID userId,
            @Parameter(description = "Numéro de page (par défaut: 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page (par défaut: 20)") @RequestParam(defaultValue = "20") int size) {

        log.info("Getting in-app notifications for user ID: {}, page: {}, size: {}", userId, page, size);
        Page<Notification> notifications = inAppNotificationService.getUserNotifications(userId, page, size);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/user/{userId}/unread")
    @Operation(summary = "Récupérer les notifications non lues", 
               description = "Récupère toutes les notifications non lues pour un utilisateur spécifique")
    public ResponseEntity<List<Notification>> getUnreadNotifications(
            @Parameter(description = "ID de l'utilisateur") @PathVariable UUID userId) {

        log.info("Getting unread in-app notifications for user ID: {}", userId);
        List<Notification> notifications = inAppNotificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/user/{userId}/unread/count")
    @Operation(summary = "Compter les notifications non lues", 
               description = "Compte le nombre de notifications non lues pour un utilisateur spécifique")
    public ResponseEntity<Long> getUnreadNotificationsCount(
            @Parameter(description = "ID de l'utilisateur") @PathVariable UUID userId) {

        log.info("Getting unread in-app notifications count for user ID: {}", userId);
        long count = inAppNotificationService.getUnreadCount(userId);
        return ResponseEntity.ok(count);
    }

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Marquer une notification comme lue", 
               description = "Marque une notification spécifique comme lue")
    public ResponseEntity<Notification> markNotificationAsRead(
            @Parameter(description = "ID de la notification") @PathVariable UUID notificationId) {

        log.info("Marking in-app notification as read with ID: {}", notificationId);
        Notification notification = inAppNotificationService.markAsRead(notificationId);
        return ResponseEntity.ok(notification);
    }

    @PutMapping("/user/{userId}/read-all")
    @Operation(summary = "Marquer toutes les notifications comme lues", 
               description = "Marque toutes les notifications d'un utilisateur comme lues")
    public ResponseEntity<Void> markAllNotificationsAsRead(
            @Parameter(description = "ID de l'utilisateur") @PathVariable UUID userId) {

        log.info("Marking all in-app notifications as read for user ID: {}", userId);
        inAppNotificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }
}
