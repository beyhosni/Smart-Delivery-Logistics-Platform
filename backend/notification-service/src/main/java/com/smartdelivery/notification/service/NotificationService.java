package com.smartdelivery.notification.service;

import com.smartdelivery.notification.model.Notification;
import com.smartdelivery.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    /**
     * Crée et envoie une notification
     * @param userId ID de l'utilisateur
     * @param deliveryId ID de la livraison
     * @param type Type de notification
     * @param channel Canal de notification
     * @param recipient Destinataire (email ou numéro de téléphone)
     * @param subject Sujet de la notification
     * @param content Contenu de la notification
     * @return La notification créée
     */
    @Transactional
    public Notification createAndSendNotification(
            UUID userId, UUID deliveryId,
            Notification.NotificationType type,
            Notification.NotificationChannel channel,
            String recipient, String subject, String content) {

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

        // Envoyer la notification de manière asynchrone
        sendNotificationAsync(savedNotification);

        return savedNotification;
    }

    /**
     * Récupère toutes les notifications d'un utilisateur
     * @param userId ID de l'utilisateur
     * @return La liste des notifications
     */
    public List<Notification> getNotificationsByUserId(UUID userId) {
        return notificationRepository.findByUserId(userId);
    }

    /**
     * Récupère toutes les notifications d'une livraison
     * @param deliveryId ID de la livraison
     * @return La liste des notifications
     */
    public List<Notification> getNotificationsByDeliveryId(UUID deliveryId) {
        return notificationRepository.findByDeliveryId(deliveryId);
    }

    /**
     * Récupère toutes les notifications en attente pour un canal spécifique
     * @param channel Canal de notification
     * @return La liste des notifications en attente
     */
    public List<Notification> getPendingNotificationsByChannel(Notification.NotificationChannel channel) {
        return notificationRepository.findPendingNotificationsByChannel(channel);
    }

    /**
     * Met à jour le statut d'une notification
     * @param notificationId ID de la notification
     * @param status Nouveau statut
     * @param errorMessage Message d'erreur (en cas d'échec)
     * @return La notification mise à jour
     */
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

    /**
     * Envoie une notification de manière asynchrone
     * @param notification La notification à envoyer
     */
    @Async
    public void sendNotificationAsync(Notification notification) {
        try {
            switch (notification.getChannel()) {
                case EMAIL:
                    emailService.sendEmail(notification);
                    break;
                case SMS:
                    smsService.sendSms(notification);
                    break;
                case PUSH:
                    // Implémentation des notifications push
                    log.info("Sending PUSH notification to {}: {}", notification.getRecipient(), notification.getSubject());
                    updateNotificationStatus(notification.getId(), Notification.NotificationStatus.SENT, null);
                    break;
                default:
                    throw new RuntimeException("Canal de notification non supporté: " + notification.getChannel());
            }
        } catch (Exception e) {
            log.error("Error sending notification", e);
            updateNotificationStatus(notification.getId(), Notification.NotificationStatus.FAILED, e.getMessage());
        }
    }

    /**
     * Traite toutes les notifications en attente pour un canal spécifique
     * @param channel Canal de notification
     */
    @Transactional
    public void processPendingNotifications(Notification.NotificationChannel channel) {
        List<Notification> pendingNotifications = getPendingNotificationsByChannel(channel);

        log.info("Processing {} pending {} notifications", pendingNotifications.size(), channel);

        for (Notification notification : pendingNotifications) {
            sendNotificationAsync(notification);
        }
    }
}
