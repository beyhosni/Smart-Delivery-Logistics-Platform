package com.smartdelivery.notification.service;

import com.smartdelivery.notification.model.Notification;
import com.smartdelivery.notification.model.NotificationPreference;
import com.smartdelivery.notification.repository.NotificationPreferenceRepository;
import com.smartdelivery.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceWithMonitoring {

    private final NotificationRepository notificationRepository;
    private final EmailNotificationService emailNotificationService;
    private final SmsNotificationService smsNotificationService;
    private final PushNotificationService pushNotificationService;
    private final InAppNotificationService inAppNotificationService;
    private final NotificationPreferenceService preferenceService;
    private final MonitoringService monitoringService;

    @Transactional
    public Notification createAndSendNotification(
            UUID userId,
            UUID deliveryId,
            Notification.NotificationType type,
            Notification.NotificationChannel channel,
            String recipient,
            String subject,
            String content) {

        // Créer la notification
        Notification notification = Notification.builder()
                .userId(userId)
                .deliveryId(deliveryId)
                .type(type)
                .channel(channel)
                .recipient(recipient)
                .subject(subject)
                .content(content)
                .status(Notification.NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        // Envoyer la notification de manière asynchrone avec monitoring
        sendNotificationAsyncWithMonitoring(savedNotification);

        return savedNotification;
    }

    @Cacheable(value = "notifications", key = "#userId")
    public List<Notification> getNotificationsByUserId(UUID userId) {
        log.info("Getting notifications for user ID: {} from database", userId);
        return notificationRepository.findByUserId(userId);
    }

    @Cacheable(value = "notifications", key = "#deliveryId")
    public List<Notification> getNotificationsByDeliveryId(UUID deliveryId) {
        log.info("Getting notifications for delivery ID: {} from database", deliveryId);
        return notificationRepository.findByDeliveryId(deliveryId);
    }

    @Cacheable(value = "unreadNotifications", key = "#userId")
    public List<Notification> getUnreadNotifications(UUID userId) {
        log.info("Getting unread notifications for user ID: {} from database", userId);
        return notificationRepository.findByUserIdAndStatus(userId, Notification.NotificationStatus.PENDING);
    }

    @CacheEvict(value = {"notifications", "unreadNotifications"}, key = "#notificationId")
    @Transactional
    public Notification updateNotificationStatus(
            UUID notificationId,
            Notification.NotificationStatus status,
            String errorMessage) {

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée avec l'ID: " + notificationId));

        notification.setStatus(status);

        if (status == Notification.NotificationStatus.SENT) {
            notification.setSentAt(LocalDateTime.now());
        }

        if (errorMessage != null) {
            notification.setErrorMessage(errorMessage);
        }

        return notificationRepository.save(notification);
    }

    @CacheEvict(value = {"notifications", "unreadNotifications"}, key = "#notificationId")
    @Transactional
    public void deleteNotification(UUID notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    @Async
    public void sendNotificationAsyncWithMonitoring(Notification notification) {
        long startTime = System.currentTimeMillis();

        try {
            switch (notification.getChannel()) {
                case EMAIL:
                    monitoringService.recordEmailNotificationDuration(() -> {
                        emailNotificationService.sendEmail(notification);
                        monitoringService.incrementEmailCounter();
                    });
                    break;
                case SMS:
                    monitoringService.recordSmsNotificationDuration(() -> {
                        smsNotificationService.sendSms(notification);
                        monitoringService.incrementSmsCounter();
                    });
                    break;
                case PUSH:
                    monitoringService.recordPushNotificationDuration(() -> {
                        pushNotificationService.sendPushNotification(notification);
                        monitoringService.incrementPushCounter();
                    });
                    break;
                case IN_APP:
                    monitoringService.recordInAppNotificationDuration(() -> {
                        inAppNotificationService.sendInAppNotification(notification);
                        monitoringService.incrementInAppCounter();
                    });
                    break;
                default:
                    throw new RuntimeException("Canal de notification non supporté: " + notification.getChannel());
            }

            monitoringService.incrementSuccessCounter();
        } catch (Exception e) {
            log.error("Error sending notification", e);
            monitoringService.incrementFailureCounter();
            updateNotificationStatus(notification.getId(), Notification.NotificationStatus.FAILED, e.getMessage());
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("Notification {} sent in {} ms", notification.getId(), duration);
        }
    }

    @Transactional
    @CacheEvict(value = {"notifications", "unreadNotifications"}, allEntries = true)
    public void processPendingNotifications(Notification.NotificationChannel channel) {
        List<Notification> pendingNotifications = notificationRepository
                .findPendingNotificationsByChannel(channel);

        log.info("Processing {} pending {} notifications", pendingNotifications.size(), channel);

        for (Notification notification : pendingNotifications) {
            sendNotificationAsyncWithMonitoring(notification);
        }
    }
}
