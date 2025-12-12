package com.smartdelivery.notification.listener;

import com.smartdelivery.notification.model.Notification;
import com.smartdelivery.notification.model.NotificationEvent;
import com.smartdelivery.notification.service.EmailNotificationService;
import com.smartdelivery.notification.service.NotificationPreferenceService;
import com.smartdelivery.notification.service.PushNotificationService;
import com.smartdelivery.notification.service.SmsNotificationService;
import com.smartdelivery.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQEventListener {

    private final EmailNotificationService emailNotificationService;
    private final SmsNotificationService smsNotificationService;
    private final PushNotificationService pushNotificationService;
    private final NotificationPreferenceService preferenceService;
    private final NotificationRepository notificationRepository;

    @RabbitListener(queues = "${rabbitmq.queue.dispatched.name}")
    public void handleDeliveryDispatchedEvent(Map<String, Object> event) {
        log.info("Received delivery dispatched event: {}", event);

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(UUID.fromString(event.get("userId").toString()))
                .deliveryId(UUID.fromString(event.get("deliveryId").toString()))
                .type(NotificationEvent.EventType.DELIVERY_DISPATCHED)
                .data(event)
                .timestamp(LocalDateTime.now())
                .build();

        createNotificationsForEvent(notificationEvent);
    }

    @RabbitListener(queues = "${rabbitmq.queue.in_transit.name}")
    public void handleDeliveryInTransitEvent(Map<String, Object> event) {
        log.info("Received delivery in transit event: {}", event);

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(UUID.fromString(event.get("userId").toString()))
                .deliveryId(UUID.fromString(event.get("deliveryId").toString()))
                .type(NotificationEvent.EventType.DELIVERY_IN_TRANSIT)
                .data(event)
                .timestamp(LocalDateTime.now())
                .build();

        createNotificationsForEvent(notificationEvent);
    }

    @RabbitListener(queues = "${rabbitmq.queue.delivered.name}")
    public void handleDeliveryDeliveredEvent(Map<String, Object> event) {
        log.info("Received delivery delivered event: {}", event);

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(UUID.fromString(event.get("userId").toString()))
                .deliveryId(UUID.fromString(event.get("deliveryId").toString()))
                .type(NotificationEvent.EventType.DELIVERY_DELIVERED)
                .data(event)
                .timestamp(LocalDateTime.now())
                .build();

        createNotificationsForEvent(notificationEvent);
    }

    private void createNotificationsForEvent(NotificationEvent event) {
        // Vérifier si nous sommes dans les heures de silence
        boolean inQuietHours = preferenceService.isInQuietHours(event.getUserId());

        // Notification par email
        if (preferenceService.isNotificationEnabled(event.getUserId(), 
                mapEventTypeToPreferenceType(event.getType()), 
                Notification.NotificationChannel.EMAIL)) {

            Notification emailNotification = buildNotification(event, Notification.NotificationChannel.EMAIL);

            // Si nous sommes dans les heures de silence, mettre en attente
            if (inQuietHours) {
                emailNotification.setStatus(Notification.NotificationStatus.PENDING);
            }

            emailNotificationService.sendEmail(emailNotification);
        }

        // Notification SMS
        if (preferenceService.isNotificationEnabled(event.getUserId(), 
                mapEventTypeToPreferenceType(event.getType()), 
                Notification.NotificationChannel.SMS)) {

            Notification smsNotification = buildNotification(event, Notification.NotificationChannel.SMS);

            // Si nous sommes dans les heures de silence, mettre en attente
            if (inQuietHours) {
                smsNotification.setStatus(Notification.NotificationStatus.PENDING);
            }

            smsNotificationService.sendSms(smsNotification);
        }

        // Notification push
        if (preferenceService.isNotificationEnabled(event.getUserId(), 
                mapEventTypeToPreferenceType(event.getType()), 
                Notification.NotificationChannel.PUSH)) {

            Notification pushNotification = buildNotification(event, Notification.NotificationChannel.PUSH);

            // Les notifications push sont toujours envoyées, même pendant les heures de silence
            pushNotificationService.sendPushNotification(pushNotification);
        }
    }

    private Notification buildNotification(NotificationEvent event, Notification.NotificationChannel channel) {
        String subject = generateSubject(event.getType(), event.getData());
        String content = generateContent(event.getType(), event.getData());
        String recipient = generateRecipient(event.getUserId(), channel, event.getData());

        return Notification.builder()
                .id(UUID.randomUUID())
                .userId(event.getUserId())
                .deliveryId(event.getDeliveryId())
                .type(mapEventTypeToNotificationType(event.getType()))
                .channel(channel)
                .recipient(recipient)
                .subject(subject)
                .content(content)
                .status(Notification.NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Notification.NotificationType mapEventTypeToNotificationType(NotificationEvent.EventType eventType) {
        return switch (eventType) {
            case DELIVERY_CREATED -> Notification.NotificationType.DELIVERY_CREATED;
            case DELIVERY_DISPATCHED -> Notification.NotificationType.DELIVERY_DISPATCHED;
            case DELIVERY_PICKED_UP, DELIVERY_IN_TRANSIT -> Notification.NotificationType.DELIVERY_IN_TRANSIT;
            case DELIVERY_DELIVERED -> Notification.NotificationType.DELIVERY_DELIVERED;
            case DELIVERY_DELAYED -> Notification.NotificationType.DELIVERY_DELAYED;
            default -> Notification.NotificationType.DELIVERY_IN_TRANSIT; // Type par défaut
        };
    }

    private NotificationPreference.NotificationType mapEventTypeToPreferenceType(NotificationEvent.EventType eventType) {
        return switch (eventType) {
            case DELIVERY_CREATED -> NotificationPreference.NotificationType.DELIVERY_CREATED;
            case DELIVERY_DISPATCHED -> NotificationPreference.NotificationType.DELIVERY_DISPATCHED;
            case DELIVERY_PICKED_UP, DELIVERY_IN_TRANSIT -> NotificationPreference.NotificationType.DELIVERY_IN_TRANSIT;
            case DELIVERY_DELIVERED -> NotificationPreference.NotificationType.DELIVERY_DELIVERED;
            case DELIVERY_DELAYED -> NotificationPreference.NotificationType.DELIVERY_DELAYED;
            case COURIER_ARRIVAL -> NotificationPreference.NotificationType.COURIER_ARRIVAL;
            case PAYMENT_CONFIRMED -> NotificationPreference.NotificationType.PAYMENT_CONFIRMED;
            case PAYMENT_FAILED -> NotificationPreference.NotificationType.PAYMENT_FAILED;
            case PROMOTION -> NotificationPreference.NotificationType.PROMOTION;
            case SYSTEM_UPDATE -> NotificationPreference.NotificationType.SYSTEM_UPDATE;
        };
    }

    private String generateSubject(NotificationEvent.EventType eventType, Map<String, Object> data) {
        return switch (eventType) {
            case DELIVERY_CREATED -> "Votre livraison a été créée";
            case DELIVERY_DISPATCHED -> "Votre livraison a été expédiée";
            case DELIVERY_PICKED_UP -> "Votre colis a été pris en charge";
            case DELIVERY_IN_TRANSIT -> "Votre livraison est en cours";
            case DELIVERY_DELIVERED -> "Votre livraison a été livrée";
            case DELIVERY_DELAYED -> "Votre livraison est retardée";
            case COURIER_ARRIVAL -> "Le livreur est arrivé";
            case PAYMENT_CONFIRMED -> "Paiement confirmé";
            case PAYMENT_FAILED -> "Échec du paiement";
            case PROMOTION -> "Offre spéciale pour vous";
            case SYSTEM_UPDATE -> "Mise à jour du système";
        };
    }

    private String generateContent(NotificationEvent.EventType eventType, Map<String, Object> data) {
        String deliveryId = data.get("deliveryId") != null ? data.get("deliveryId").toString() : "";
        String trackingNumber = data.get("trackingNumber") != null ? data.get("trackingNumber").toString() : "";
        String estimatedDelivery = data.get("estimatedDelivery") != null ? data.get("estimatedDelivery").toString() : "";
        String courierName = data.get("courierName") != null ? data.get("courierName").toString() : "";

        return switch (eventType) {
            case DELIVERY_CREATED -> String.format(
                    "Votre livraison #%s a été créée avec succès. Numéro de suivi: %s. Date de livraison estimée: %s",
                    deliveryId, trackingNumber, estimatedDelivery);
            case DELIVERY_DISPATCHED -> String.format(
                    "Votre livraison #%s a été expédiée. Vous pouvez suivre son parcours avec le numéro: %s",
                    deliveryId, trackingNumber);
            case DELIVERY_PICKED_UP -> String.format(
                    "Votre colis #%s a été pris en charge par notre livreur. Préparez-vous pour sa réception!",
                    deliveryId);
            case DELIVERY_IN_TRANSIT -> String.format(
                    "Votre livraison #%s est en cours de transport. Elle arrivera bientôt à destination.",
                    deliveryId);
            case DELIVERY_DELIVERED -> String.format(
                    "Votre livraison #%s a été livrée avec succès. Merci d'avoir utilisé nos services!",
                    deliveryId);
            case DELIVERY_DELAYED -> String.format(
                    "Votre livraison #%s a rencontré un retard. Nouvelle date de livraison estimée: %s",
                    deliveryId, estimatedDelivery);
            case COURIER_ARRIVAL -> String.format(
                    "Notre livreur %s est arrivé à votre adresse. Veuillez vous présenter pour récupérer votre colis.",
                    courierName);
            case PAYMENT_CONFIRMED -> String.format(
                    "Votre paiement de %s€ pour la livraison #%s a été confirmé avec succès.",
                    data.get("amount"), deliveryId);
            case PAYMENT_FAILED -> String.format(
                    "Le paiement de %s€ pour la livraison #%s a échoué. Veuillez vérifier vos informations de paiement.",
                    data.get("amount"), deliveryId);
            case PROMOTION -> String.format(
                    "Profitez de notre offre spéciale: %s. Valable jusqu'au %s.",
                    data.get("offer"), data.get("validUntil"));
            case SYSTEM_UPDATE -> String.format(
                    "Notre système sera mis à jour le %s de %s à %s. Certains services pourraient être temporairement indisponibles.",
                    data.get("date"), data.get("startTime"), data.get("endTime"));
        };
    }

    private String generateRecipient(UUID userId, Notification.NotificationChannel channel, Map<String, Object> data) {
        return switch (channel) {
            case EMAIL -> data.get("email") != null ? data.get("email").toString() : "";
            case SMS -> data.get("phone") != null ? data.get("phone").toString() : "";
            case PUSH -> ""; // Pour les notifications push, le destinataire est géré par le service de notification push
        };
    }
}
