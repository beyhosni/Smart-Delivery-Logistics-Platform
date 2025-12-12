package com.smartdelivery.notification.service;

import com.smartdelivery.notification.model.NotificationPreference;
import com.smartdelivery.notification.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    public List<NotificationPreference> getUserPreferences(UUID userId) {
        return preferenceRepository.findByUserId(userId);
    }

    @Transactional
    public NotificationPreference updateUserPreference(UUID userId, 
                                                      NotificationPreference.NotificationType type,
                                                      Boolean emailEnabled,
                                                      Boolean smsEnabled,
                                                      Boolean pushEnabled,
                                                      Boolean inAppEnabled) {

        NotificationPreference preference = preferenceRepository
                .findByUserIdAndNotificationType(userId, type)
                .stream()
                .findFirst()
                .orElse(null);

        if (preference == null) {
            preference = NotificationPreference.builder()
                    .userId(userId)
                    .notificationType(type)
                    .build();
        }

        if (emailEnabled != null) {
            preference.setEmailEnabled(emailEnabled);
        }
        if (smsEnabled != null) {
            preference.setSmsEnabled(smsEnabled);
        }
        if (pushEnabled != null) {
            preference.setPushEnabled(pushEnabled);
        }
        if (inAppEnabled != null) {
            preference.setInAppEnabled(inAppEnabled);
        }

        preference = preferenceRepository.save(preference);
        log.info("Updated notification preference for user {} and type {}", userId, type);
        return preference;
    }

    @Transactional
    public NotificationPreference updateQuietHours(UUID userId, 
                                                    Boolean quietHoursEnabled,
                                                    Integer quietHoursStart,
                                                    Integer quietHoursEnd) {

        List<NotificationPreference> preferences = preferenceRepository.findByUserId(userId);

        if (preferences.isEmpty()) {
            log.warn("No preferences found for user {}", userId);
            return null;
        }

        // Mettre à jour toutes les préférences de l'utilisateur avec les heures de silence
        for (NotificationPreference preference : preferences) {
            if (quietHoursEnabled != null) {
                preference.setQuietHoursEnabled(quietHoursEnabled);
            }
            if (quietHoursStart != null) {
                preference.setQuietHoursStart(quietHoursStart);
            }
            if (quietHoursEnd != null) {
                preference.setQuietHoursEnd(quietHoursEnd);
            }
        }

        // Sauvegarder et retourner la première préférence mise à jour
        NotificationPreference firstPreference = preferences.get(0);
        preferenceRepository.saveAll(preferences);

        log.info("Updated quiet hours for user {}: enabled={}, start={}, end={}", 
                userId, quietHoursEnabled, quietHoursStart, quietHoursEnd);

        return firstPreference;
    }

    public boolean isNotificationEnabled(UUID userId, 
                                        NotificationPreference.NotificationType type,
                                        NotificationPreference.NotificationChannel channel) {
        NotificationPreference preference = preferenceRepository
                .findByUserIdAndNotificationType(userId, type)
                .stream()
                .findFirst()
                .orElse(null);

        if (preference == null) {
            // Par défaut, toutes les notifications sont activées
            return true;
        }

        return switch (channel) {
            case EMAIL -> preference.getEmailEnabled();
            case SMS -> preference.getSmsEnabled();
            case PUSH -> preference.getPushEnabled();
        };
    }

    public boolean isInQuietHours(UUID userId) {
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .stream()
                .findFirst()
                .orElse(null);

        if (preference == null || !preference.getQuietHoursEnabled()) {
            return false;
        }

        int currentHour = java.time.LocalDateTime.now().getHour();
        int start = preference.getQuietHoursStart();
        int end = preference.getQuietHoursEnd();

        // Gérer le cas où les heures de silence traversent minuit
        if (start > end) {
            return currentHour >= start || currentHour < end;
        } else {
            return currentHour >= start && currentHour < end;
        }
    }

    @Transactional
    public void createDefaultPreferences(UUID userId) {
        // Créer des préférences par défaut pour tous les types de notification
        for (NotificationPreference.NotificationType type : NotificationPreference.NotificationType.values()) {
            NotificationPreference preference = NotificationPreference.builder()
                    .userId(userId)
                    .notificationType(type)
                    .emailEnabled(true)
                    .smsEnabled(true)
                    .pushEnabled(true)
                    .inAppEnabled(true)
                    .quietHoursEnabled(false)
                    .quietHoursStart(22)
                    .quietHoursEnd(8)
                    .build();

            preferenceRepository.save(preference);
        }

        log.info("Created default notification preferences for user {}", userId);
    }
}
