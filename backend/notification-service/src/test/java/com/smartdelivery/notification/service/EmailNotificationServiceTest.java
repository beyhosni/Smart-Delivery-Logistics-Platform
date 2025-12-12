package com.smartdelivery.notification.service;

import com.smartdelivery.notification.model.Notification;
import com.smartdelivery.notification.model.NotificationPreference;
import com.smartdelivery.notification.repository.NotificationPreferenceRepository;
import com.smartdelivery.notification.repository.NotificationRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private MimeMessage mimeMessage;

    @Mock
    private MimeMessageHelper mimeMessageHelper;

    @InjectMocks
    private EmailNotificationService emailNotificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailNotificationService, "fromEmail", "test@smartdelivery.com");
        ReflectionTestUtils.setField(emailNotificationService, "emailEnabled", true);
    }

    @Test
    void testSendEmailSuccess() throws MessagingException {
        // Given
        UUID userId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .deliveryId(deliveryId)
                .type(Notification.NotificationType.DELIVERY_CREATED)
                .channel(Notification.NotificationChannel.EMAIL)
                .recipient("test@example.com")
                .subject("Test Subject")
                .content("Test Content")
                .status(Notification.NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // When
        emailNotificationService.sendEmail(notification);

        // Then
        verify(mailSender).send(any(MimeMessage.class));
        verify(notificationRepository).save(notification);
        assertEquals(Notification.NotificationStatus.SENT, notification.getStatus());
        assertNotNull(notification.getSentAt());
    }

    @Test
    void testSendEmailDisabled() {
        // Given
        ReflectionTestUtils.setField(emailNotificationService, "emailEnabled", false);

        UUID userId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .deliveryId(deliveryId)
                .type(Notification.NotificationType.DELIVERY_CREATED)
                .channel(Notification.NotificationChannel.EMAIL)
                .recipient("test@example.com")
                .subject("Test Subject")
                .content("Test Content")
                .status(Notification.NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        // When
        emailNotificationService.sendEmail(notification);

        // Then
        verify(mailSender, never()).send(any(MimeMessage.class));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void testSendEmailWithDisabledPreference() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .deliveryId(deliveryId)
                .type(Notification.NotificationType.DELIVERY_CREATED)
                .channel(Notification.NotificationChannel.EMAIL)
                .recipient("test@example.com")
                .subject("Test Subject")
                .content("Test Content")
                .status(Notification.NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        NotificationPreference preference = NotificationPreference.builder()
                .userId(userId)
                .notificationType(NotificationPreference.NotificationType.DELIVERY_CREATED)
                .emailEnabled(false)
                .build();

        when(preferenceRepository.findByUserIdAndNotificationType(userId, NotificationPreference.NotificationType.DELIVERY_CREATED))
                .thenReturn(Optional.of(preference));

        // When
        emailNotificationService.sendEmail(notification);

        // Then
        verify(mailSender, never()).send(any(MimeMessage.class));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void testSendEmailFailure() throws MessagingException {
        // Given
        UUID userId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .deliveryId(deliveryId)
                .type(Notification.NotificationType.DELIVERY_CREATED)
                .channel(Notification.NotificationChannel.EMAIL)
                .recipient("test@example.com")
                .subject("Test Subject")
                .content("Test Content")
                .status(Notification.NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MessagingException("Test exception")).when(mailSender).send(any(MimeMessage.class));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // When
        emailNotificationService.sendEmail(notification);

        // Then
        verify(mailSender).send(any(MimeMessage.class));
        verify(notificationRepository).save(notification);
        assertEquals(Notification.NotificationStatus.FAILED, notification.getStatus());
        assertEquals("Test exception", notification.getErrorMessage());
    }

    @Test
    void testIsQuietHours() {
        // Given
        UUID userId = UUID.randomUUID();

        NotificationPreference preference = NotificationPreference.builder()
                .userId(userId)
                .quietHoursEnabled(true)
                .quietHoursStart(22)
                .quietHoursEnd(8)
                .build();

        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));

        // When & Then
        // Test pendant les heures de silence (23h)
        LocalDateTime testTime = LocalDateTime.now().withHour(23);
        assertTrue(emailNotificationService.isQuietHours(userId));

        // Test en dehors des heures de silence (10h)
        testTime = LocalDateTime.now().withHour(10);
        assertFalse(emailNotificationService.isQuietHours(userId));
    }

    @Test
    void testIsQuietHoursDisabled() {
        // Given
        UUID userId = UUID.randomUUID();

        NotificationPreference preference = NotificationPreference.builder()
                .userId(userId)
                .quietHoursEnabled(false)
                .build();

        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));

        // When & Then
        assertFalse(emailNotificationService.isQuietHours(userId));
    }

    @Test
    void testIsQuietHoursNoPreference() {
        // Given
        UUID userId = UUID.randomUUID();

        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When & Then
        assertFalse(emailNotificationService.isQuietHours(userId));
    }
}
