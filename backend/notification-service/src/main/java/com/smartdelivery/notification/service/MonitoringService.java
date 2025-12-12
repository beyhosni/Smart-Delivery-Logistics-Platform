package com.smartdelivery.notification.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringService {

    private final MeterRegistry meterRegistry;

    // Compteurs pour les notifications
    private Counter emailNotificationCounter;
    private Counter smsNotificationCounter;
    private Counter pushNotificationCounter;
    private Counter inAppNotificationCounter;

    // Compteurs pour les succès et échecs
    private Counter notificationSuccessCounter;
    private Counter notificationFailureCounter;

    // Timers pour mesurer la durée d'envoi
    private Timer emailNotificationTimer;
    private Timer smsNotificationTimer;
    private Timer pushNotificationTimer;
    private Timer inAppNotificationTimer;

    public void initializeCounters() {
        // Initialiser les compteurs pour les notifications par canal
        emailNotificationCounter = Counter.builder("notifications.email.total")
                .description("Total number of email notifications sent")
                .register(meterRegistry);

        smsNotificationCounter = Counter.builder("notifications.sms.total")
                .description("Total number of SMS notifications sent")
                .register(meterRegistry);

        pushNotificationCounter = Counter.builder("notifications.push.total")
                .description("Total number of push notifications sent")
                .register(meterRegistry);

        inAppNotificationCounter = Counter.builder("notifications.in_app.total")
                .description("Total number of in-app notifications sent")
                .register(meterRegistry);

        // Initialiser les compteurs pour les succès et échecs
        notificationSuccessCounter = Counter.builder("notifications.success.total")
                .description("Total number of successful notifications")
                .register(meterRegistry);

        notificationFailureCounter = Counter.builder("notifications.failure.total")
                .description("Total number of failed notifications")
                .register(meterRegistry);

        // Initialiser les timers pour mesurer la durée d'envoi
        emailNotificationTimer = Timer.builder("notifications.email.duration")
                .description("Time taken to send email notifications")
                .register(meterRegistry);

        smsNotificationTimer = Timer.builder("notifications.sms.duration")
                .description("Time taken to send SMS notifications")
                .register(meterRegistry);

        pushNotificationTimer = Timer.builder("notifications.push.duration")
                .description("Time taken to send push notifications")
                .register(meterRegistry);

        inAppNotificationTimer = Timer.builder("notifications.in_app.duration")
                .description("Time taken to send in-app notifications")
                .register(meterRegistry);
    }

    public void incrementEmailCounter() {
        if (emailNotificationCounter == null) {
            initializeCounters();
        }
        emailNotificationCounter.increment();
    }

    public void incrementSmsCounter() {
        if (smsNotificationCounter == null) {
            initializeCounters();
        }
        smsNotificationCounter.increment();
    }

    public void incrementPushCounter() {
        if (pushNotificationCounter == null) {
            initializeCounters();
        }
        pushNotificationCounter.increment();
    }

    public void incrementInAppCounter() {
        if (inAppNotificationCounter == null) {
            initializeCounters();
        }
        inAppNotificationCounter.increment();
    }

    public void incrementSuccessCounter() {
        if (notificationSuccessCounter == null) {
            initializeCounters();
        }
        notificationSuccessCounter.increment();
    }

    public void incrementFailureCounter() {
        if (notificationFailureCounter == null) {
            initializeCounters();
        }
        notificationFailureCounter.increment();
    }

    public void recordEmailNotificationDuration(Duration duration) {
        if (emailNotificationTimer == null) {
            initializeCounters();
        }
        emailNotificationTimer.record(duration);
    }

    public void recordSmsNotificationDuration(Duration duration) {
        if (smsNotificationTimer == null) {
            initializeCounters();
        }
        smsNotificationTimer.record(duration);
    }

    public void recordPushNotificationDuration(Duration duration) {
        if (pushNotificationTimer == null) {
            initializeCounters();
        }
        pushNotificationTimer.record(duration);
    }

    public void recordInAppNotificationDuration(Duration duration) {
        if (inAppNotificationTimer == null) {
            initializeCounters();
        }
        inAppNotificationTimer.record(duration);
    }

    public void recordEmailNotificationDuration(long millis) {
        recordEmailNotificationDuration(Duration.ofMillis(millis));
    }

    public void recordSmsNotificationDuration(long millis) {
        recordSmsNotificationDuration(Duration.ofMillis(millis));
    }

    public void recordPushNotificationDuration(long millis) {
        recordPushNotificationDuration(Duration.ofMillis(millis));
    }

    public void recordInAppNotificationDuration(long millis) {
        recordInAppNotificationDuration(Duration.ofMillis(millis));
    }

    public void recordEmailNotificationDuration(Runnable task) {
        if (emailNotificationTimer == null) {
            initializeCounters();
        }
        emailNotificationTimer.record(task);
    }

    public void recordSmsNotificationDuration(Runnable task) {
        if (smsNotificationTimer == null) {
            initializeCounters();
        }
        smsNotificationTimer.record(task);
    }

    public void recordPushNotificationDuration(Runnable task) {
        if (pushNotificationTimer == null) {
            initializeCounters();
        }
        pushNotificationTimer.record(task);
    }

    public void recordInAppNotificationDuration(Runnable task) {
        if (inAppNotificationTimer == null) {
            initializeCounters();
        }
        inAppNotificationTimer.record(task);
    }
}
