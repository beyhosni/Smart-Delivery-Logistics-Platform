package com.smartdelivery.notification.service;

import com.smartdelivery.notification.model.Notification;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final NotificationService notificationService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Envoie un email basé sur une notification
     * @param notification La notification contenant les détails de l'email
     */
    @Transactional
    public void sendEmail(Notification notification) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(notification.getRecipient());
            helper.setSubject(notification.getSubject());

            // Utiliser un template Thymeleaf pour le contenu de l'email
            Context context = new Context();
            context.setVariable("notification", notification);

            String htmlContent = templateEngine.process("email-template", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            // Mettre à jour le statut de la notification
            notificationService.updateNotificationStatus(
                    notification.getId(), 
                    Notification.NotificationStatus.SENT, 
                    null);

            log.info("Email sent successfully to {}", notification.getRecipient());
        } catch (Exception e) {
            log.error("Error sending email to {}", notification.getRecipient(), e);

            // Mettre à jour le statut de la notification avec l'erreur
            notificationService.updateNotificationStatus(
                    notification.getId(), 
                    Notification.NotificationStatus.FAILED, 
                    e.getMessage());

            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }

    /**
     * Envoie un email avec un template personnalisé
     * @param to Destinataire
     * @param subject Sujet
     * @param templateName Nom du template
     * @param variables Variables du template
     */
    @Transactional
    public void sendEmailWithTemplate(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);

            Context context = new Context();
            context.setVariables(variables);

            String htmlContent = templateEngine.process(templateName, context);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Error sending email to {}", to, e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }
}
