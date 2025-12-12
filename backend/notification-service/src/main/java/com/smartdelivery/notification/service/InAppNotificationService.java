package com.smartdelivery.notification.service;

import com.smartdelivery.notification.model.Notification;
import com.smartdelivery.notification.model.NotificationPreference;
import com.smartdelivery.notification.repository.NotificationPreferenceRepository;
import com.smartdelivery.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InAppNotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${app.notification.in-app.enabled}")
    private boolean inAppEnabled;

    @Async
    public void sendInAppNotification(Notification notification) {
        if (!inAppEnabled) {
            log.warn("In-app notifications are disabled");
            return;
        }

        // Vérifier les préférences de l'utilisateur
        NotificationPreference preference = preferenceRepository
                .findByUserIdAndNotificationType(notification.getUserId(), notification.getType())
                .stream()
                .findFirst()
                .orElse(null);

        if (preference != null && !preference.getInAppEnabled()) {
            log.info("In-app notification disabled for user {} and type {}", 
                    notification.getUserId(), notification.getType());
            return;
        }

        try {
            // Mettre à jour le statut de la notification
            notification.setStatus(Notification.NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            // Envoyer la notification via WebSocket
            messagingTemplate.convertAndSendToUser(
                    notification.getUserId().toString(),
                    "/queue/notifications",
                    notification);

            log.info("In-app notification sent successfully to user {} for notification {}", 
                    notification.getUserId(), notification.getId());
        } catch (Exception e) {
            log.error("Failed to send in-app notification to user {}", notification.getUserId(), e);
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notificationRepository.save(notification);
        }
    }

    public Page<Notification> getUserNotifications(UUID userId, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);
        return notificationRepository.findByUserId(userId, pageable);
    }

    public List<Notification> getUnreadNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndStatus(userId, Notification.NotificationStatus.PENDING);
    }

    @Transactional
    public Notification markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));

        notification.setStatus(Notification.NotificationStatus.READ);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Notification> unreadNotifications = getUnreadNotifications(userId);
        unreadNotifications.forEach(notification -> {
            notification.setStatus(Notification.NotificationStatus.READ);
            notificationRepository.save(notification);
        });
    }

    public long getUnreadCount(UUID userId) {
        return notificationRepository.countPendingNotificationsByUserId(userId);
    }
}
