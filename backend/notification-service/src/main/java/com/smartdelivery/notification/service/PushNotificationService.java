package com.smartdelivery.notification.service;

import com.google.firebase.messaging.*;
import com.smartdelivery.notification.model.Notification;
import com.smartdelivery.notification.model.NotificationPreference;
import com.smartdelivery.notification.model.PushNotificationMessage;
import com.smartdelivery.notification.model.PushNotificationToken;
import com.smartdelivery.notification.repository.NotificationPreferenceRepository;
import com.smartdelivery.notification.repository.NotificationRepository;
import com.smartdelivery.notification.repository.PushNotificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final PushNotificationTokenRepository tokenRepository;
    private final FirebaseMessaging firebaseMessaging;

    @Value("${app.notification.push.enabled}")
    private boolean pushEnabled;

    @Async
    public void sendPushNotification(Notification notification) {
        if (!pushEnabled) {
            log.warn("Push notifications are disabled");
            return;
        }

        // Vérifier les préférences de l'utilisateur
        NotificationPreference preference = preferenceRepository
                .findByUserIdAndNotificationType(notification.getUserId(), notification.getType())
                .stream()
                .findFirst()
                .orElse(null);

        if (preference != null && !preference.getPushEnabled()) {
            log.info("Push notification disabled for user {} and type {}", 
                    notification.getUserId(), notification.getType());
            return;
        }

        // Récupérer les tokens actifs pour l'utilisateur
        List<PushNotificationToken> tokens = tokenRepository.findByUserIdAndIsActive(
                notification.getUserId(), true);

        if (tokens.isEmpty()) {
            log.warn("No active push tokens found for user {}", notification.getUserId());
            return;
        }

        // Préparer le message push
        PushNotificationMessage pushMessage = PushNotificationMessage.builder()
                .title(notification.getSubject())
                .body(notification.getContent())
                .data(Map.of(
                        "notificationId", notification.getId().toString(),
                        "deliveryId", notification.getDeliveryId().toString(),
                        "type", notification.getType().name()
                ))
                .build();

        // Envoyer aux tokens par lots (max 1000 tokens par requête)
        List<String> tokenStrings = tokens.stream()
                .map(PushNotificationToken::getDeviceToken)
                .collect(Collectors.toList());

        sendBatchPushNotification(notification, pushMessage, tokenStrings);
    }

    private void sendBatchPushNotification(Notification notification, PushNotificationMessage pushMessage, List<String> tokens) {
        try {
            // Diviser les tokens en lots de 1000
            List<List<String>> batches = partition(tokens, 1000);

            for (List<String> batch : batches) {
                MulticastMessage message = MulticastMessage.builder()
                        .setNotification(Notification.builder()
                                .setTitle(pushMessage.getTitle())
                                .setBody(pushMessage.getBody())
                                .build())
                        .putAllData(pushMessage.getData())
                        .addAllTokens(batch)
                        .setApnsConfig(ApnsConfig.builder()
                                .setAps(Aps.builder()
                                        .setSound(pushMessage.getSound())
                                        .setBadge(1)
                                        .build())
                                .build())
                        .setAndroidConfig(AndroidConfig.builder()
                                .setNotification(AndroidNotification.builder()
                                        .setSound(pushMessage.getSound())
                                        .setPriority(AndroidConfig.Priority.HIGH)
                                        .build())
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .setTtl(pushMessage.getTtl() * 1000)
                                .build())
                        .build();

                BatchResponse response = firebaseMessaging.sendMulticast(message);

                // Traiter les résultats
                handleBatchResponse(notification, batch, response);
            }

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send push notification", e);
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notificationRepository.save(notification);
        }
    }

    private void handleBatchResponse(Notification notification, List<String> tokens, BatchResponse response) {
        int successCount = response.getSuccessCount();
        int failureCount = response.getFailureCount();

        log.info("Push notification batch results: {} successful, {} failed", successCount, failureCount);

        if (failureCount > 0) {
            // Traiter les tokens qui ont échoué
            List<String> failedTokens = new ArrayList<>();
            List<String> errorMessageTokens = new ArrayList<>();

            for (int i = 0; i < response.getResponses().size(); i++) {
                if (!response.getResponses().get(i).isSuccessful()) {
                    String token = tokens.get(i);
                    failedTokens.add(token);

                    // Si le token n'est plus valide, le désactiver
                    if (isUnrecoverableError(response.getResponses().get(i).getException())) {
                        errorMessageTokens.add(token);
                    }
                }
            }

            // Désactiver les tokens invalides
            if (!errorMessageTokens.isEmpty()) {
                tokenRepository.findByDeviceTokenIn(errorMessageTokens)
                        .forEach(token -> {
                            token.setIsActive(false);
                            tokenRepository.save(token);
                        });
            }
        }

        // Mettre à jour le statut de la notification
        if (successCount > 0) {
            notification.setStatus(Notification.NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
        } else {
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setErrorMessage("All push notifications failed");
        }
        notificationRepository.save(notification);
    }

    private boolean isUnrecoverableError(Exception exception) {
        if (exception instanceof FirebaseMessagingException) {
            FirebaseMessagingException fme = (FirebaseMessagingException) exception;
            return fme.getErrorCode() == FirebaseMessagingException.UNREGISTERED ||
                   fme.getErrorCode() == FirebaseMessagingException.INVALID_ARGUMENT;
        }
        return false;
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}
