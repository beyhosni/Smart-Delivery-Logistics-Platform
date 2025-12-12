package com.smartdelivery.notification.service;

import com.smartdelivery.notification.model.Notification;
import com.smartdelivery.notification.model.NotificationPreference;
import com.smartdelivery.notification.repository.NotificationPreferenceRepository;
import com.smartdelivery.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;

    @Value("${app.notification.email.from}")
    private String fromEmail;

    @Value("${app.notification.email.enabled}")
    private boolean emailEnabled;

    @Async
    public void sendEmail(Notification notification) {
        if (!emailEnabled) {
            log.warn("Email notifications are disabled");
            return;
        }

        // Vérifier les préférences de l'utilisateur
        NotificationPreference preference = preferenceRepository
                .findByUserIdAndNotificationType(notification.getUserId(), notification.getType())
                .stream()
                .findFirst()
                .orElse(null);

        if (preference != null && !preference.getEmailEnabled()) {
            log.info("Email notification disabled for user {} and type {}", 
                    notification.getUserId(), notification.getType());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(notification.getRecipient());
            helper.setSubject(notification.getSubject());

            // Utiliser le template Thymeleaf si disponible
            if (notification.getContent() != null && notification.getContent().contains("template")) {
                Context context = new Context();
                context.setVariables(Map.of(
                        "notification", notification,
                        "deliveryId", notification.getDeliveryId()
                ));

                String htmlContent = templateEngine.process(notification.getContent(), context);
                helper.setText(htmlContent, true);
            } else {
                helper.setText(notification.getContent(), true);
            }

            mailSender.send(message);

            // Mettre à jour le statut de la notification
            notification.setStatus(Notification.NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("Email sent successfully to {} for notification {}", 
                    notification.getRecipient(), notification.getId());
        } catch (MessagingException e) {
            log.error("Failed to send email to {}", notification.getRecipient(), e);
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notificationRepository.save(notification);
        }
    }

    public boolean isQuietHours(UUID userId) {
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .stream()
                .findFirst()
                .orElse(null);

        if (preference == null || !preference.getQuietHoursEnabled()) {
            return false;
        }

        int currentHour = LocalDateTime.now().getHour();
        int start = preference.getQuietHoursStart();
        int end = preference.getQuietHoursEnd();

        // Gérer le cas où les heures de silence traversent minuit
        if (start > end) {
            return currentHour >= start || currentHour < end;
        } else {
            return currentHour >= start && currentHour < end;
        }
    }
}
