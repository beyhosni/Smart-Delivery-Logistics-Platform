package com.smartdelivery.notification.service;

import com.smartdelivery.notification.model.Notification;
import com.smartdelivery.notification.model.NotificationPreference;
import com.smartdelivery.notification.repository.NotificationPreferenceRepository;
import com.smartdelivery.notification.repository.NotificationRepository;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsNotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;

    @Value("${app.notification.sms.accountSid}")
    private String accountSid;

    @Value("${app.notification.sms.authToken}")
    private String authToken;

    @Value("${app.notification.sms.fromNumber}")
    private String fromNumber;

    @Value("${app.notification.sms.enabled}")
    private boolean smsEnabled;

    @Async
    public void sendSms(Notification notification) {
        if (!smsEnabled) {
            log.warn("SMS notifications are disabled");
            return;
        }

        // Vérifier les préférences de l'utilisateur
        NotificationPreference preference = preferenceRepository
                .findByUserIdAndNotificationType(notification.getUserId(), notification.getType())
                .stream()
                .findFirst()
                .orElse(null);

        if (preference != null && !preference.getSmsEnabled()) {
            log.info("SMS notification disabled for user {} and type {}", 
                    notification.getUserId(), notification.getType());
            return;
        }

        try {
            Twilio.init(accountSid, authToken);

            Message message = Message.creator(
                    new PhoneNumber(notification.getRecipient()),
                    new PhoneNumber(fromNumber),
                    notification.getContent())
                    .create();

            log.info("SMS sent successfully to {} with SID: {}", 
                    notification.getRecipient(), message.getSid());

            // Mettre à jour le statut de la notification
            notification.setStatus(Notification.NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

        } catch (Exception e) {
            log.error("Failed to send SMS to {}", notification.getRecipient(), e);
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notificationRepository.save(notification);
        }
    }
}
