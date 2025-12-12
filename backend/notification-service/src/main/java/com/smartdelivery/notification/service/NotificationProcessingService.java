package com.smartdelivery.notification.service;

import com.smartdelivery.notification.model.Notification;
import com.smartdelivery.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProcessingService {

    private final NotificationRepository notificationRepository;
    private final EmailNotificationService emailNotificationService;
    private final SmsNotificationService smsNotificationService;
    private final PushNotificationService pushNotificationService;
    private final NotificationPreferenceService preferenceService;

    @Scheduled(fixedDelay = 300000) // Exécuter toutes les 5 minutes
    public void processPendingNotifications() {
        log.info("Processing pending notifications");

        // Traiter les notifications en attente par canal
        processPendingEmailNotifications();
        processPendingSmsNotifications();
        processPendingPushNotifications();
    }

    private void processPendingEmailNotifications() {
        List<Notification> pendingEmailNotifications = notificationRepository
                .findPendingNotificationsByChannel(Notification.NotificationChannel.EMAIL);

        for (Notification notification : pendingEmailNotifications) {
            // Vérifier si nous sommes en dehors des heures de silence
            if (!preferenceService.isInQuietHours(notification.getUserId())) {
                emailNotificationService.sendEmail(notification);
            }
        }
    }

    private void processPendingSmsNotifications() {
        List<Notification> pendingSmsNotifications = notificationRepository
                .findPendingNotificationsByChannel(Notification.NotificationChannel.SMS);

        for (Notification notification : pendingSmsNotifications) {
            // Vérifier si nous sommes en dehors des heures de silence
            if (!preferenceService.isInQuietHours(notification.getUserId())) {
                smsNotificationService.sendSms(notification);
            }
        }
    }

    private void processPendingPushNotifications() {
        List<Notification> pendingPushNotifications = notificationRepository
                .findPendingNotificationsByChannel(Notification.NotificationChannel.PUSH);

        // Les notifications push sont toujours envoyées, même pendant les heures de silence
        for (Notification notification : pendingPushNotifications) {
            pushNotificationService.sendPushNotification(notification);
        }
    }

    @Scheduled(fixedDelay = 3600000) // Exécuter toutes les heures
    @Transactional
    public void cleanupOldNotifications() {
        log.info("Cleaning up old notifications");

        // Supprimer les notifications lues de plus de 30 jours
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Notification> oldNotifications = notificationRepository.findByStatusAndCreatedAtBefore(
                Notification.NotificationStatus.READ, thirtyDaysAgo);

        if (!oldNotifications.isEmpty()) {
            notificationRepository.deleteAll(oldNotifications);
            log.info("Deleted {} old notifications", oldNotifications.size());
        }
    }

    @Scheduled(fixedDelay = 86400000) // Exécuter une fois par jour
    public void retryFailedNotifications() {
        log.info("Retrying failed notifications");

        // Réessayer les notifications échouées des dernières 24 heures
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        List<Notification> failedNotifications = notificationRepository.findByStatusAndCreatedAtAfter(
                Notification.NotificationStatus.FAILED, oneDayAgo);

        for (Notification notification : failedNotifications) {
            // Réinitialiser le statut à PENDING et réessayer
            notification.setStatus(Notification.NotificationStatus.PENDING);
            notification.setErrorMessage(null);
            notificationRepository.save(notification);

            // Envoyer selon le canal
            switch (notification.getChannel()) {
                case EMAIL -> emailNotificationService.sendEmail(notification);
                case SMS -> smsNotificationService.sendSms(notification);
                case PUSH -> pushNotificationService.sendPushNotification(notification);
            }
        }
    }
}
