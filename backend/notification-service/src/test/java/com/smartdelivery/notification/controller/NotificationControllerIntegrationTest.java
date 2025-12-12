package com.smartdelivery.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdelivery.notification.model.Notification;
import com.smartdelivery.notification.repository.NotificationRepository;
import com.smartdelivery.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class NotificationControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private NotificationRepository notificationRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testGetAllNotifications() throws Exception {
        // Given
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        Notification notification1 = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId1)
                .deliveryId(UUID.randomUUID())
                .type(Notification.NotificationType.DELIVERY_CREATED)
                .channel(Notification.NotificationChannel.EMAIL)
                .recipient("test1@example.com")
                .subject("Test Subject 1")
                .content("Test Content 1")
                .status(Notification.NotificationStatus.SENT)
                .createdAt(LocalDateTime.now())
                .build();

        Notification notification2 = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId2)
                .deliveryId(UUID.randomUUID())
                .type(Notification.NotificationType.DELIVERY_DISPATCHED)
                .channel(Notification.NotificationChannel.SMS)
                .recipient("+33612345678")
                .subject("Test Subject 2")
                .content("Test Content 2")
                .status(Notification.NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        List<Notification> notifications = Arrays.asList(notification1, notification2);
        when(notificationService.getAllNotifications()).thenReturn(notifications);

        // When & Then
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].userId").value(userId1.toString()))
                .andExpect(jsonPath("$[0].type").value("DELIVERY_CREATED"))
                .andExpect(jsonPath("$[0].channel").value("EMAIL"))
                .andExpect(jsonPath("$[1].userId").value(userId2.toString()))
                .andExpect(jsonPath("$[1].type").value("DELIVERY_DISPATCHED"))
                .andExpect(jsonPath("$[1].channel").value("SMS"));
    }

    @Test
    void testGetUserNotifications() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();

        Notification notification1 = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .deliveryId(UUID.randomUUID())
                .type(Notification.NotificationType.DELIVERY_CREATED)
                .channel(Notification.NotificationChannel.EMAIL)
                .recipient("test1@example.com")
                .subject("Test Subject 1")
                .content("Test Content 1")
                .status(Notification.NotificationStatus.SENT)
                .createdAt(LocalDateTime.now())
                .build();

        Notification notification2 = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .deliveryId(UUID.randomUUID())
                .type(Notification.NotificationType.DELIVERY_DISPATCHED)
                .channel(Notification.NotificationChannel.SMS)
                .recipient("+33612345678")
                .subject("Test Subject 2")
                .content("Test Content 2")
                .status(Notification.NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        List<Notification> notifications = Arrays.asList(notification1, notification2);
        when(notificationService.getNotificationsByUserId(userId)).thenReturn(notifications);

        // When & Then
        mockMvc.perform(get("/api/notifications/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].type").value("DELIVERY_CREATED"))
                .andExpect(jsonPath("$[0].channel").value("EMAIL"))
                .andExpect(jsonPath("$[1].userId").value(userId.toString()))
                .andExpect(jsonPath("$[1].type").value("DELIVERY_DISPATCHED"))
                .andExpect(jsonPath("$[1].channel").value("SMS"));
    }

    @Test
    void testGetUnreadNotifications() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();

        Notification notification1 = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .deliveryId(UUID.randomUUID())
                .type(Notification.NotificationType.DELIVERY_CREATED)
                .channel(Notification.NotificationChannel.EMAIL)
                .recipient("test1@example.com")
                .subject("Test Subject 1")
                .content("Test Content 1")
                .status(Notification.NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Notification notification2 = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .deliveryId(UUID.randomUUID())
                .type(Notification.NotificationType.DELIVERY_DISPATCHED)
                .channel(Notification.NotificationChannel.SMS)
                .recipient("+33612345678")
                .subject("Test Subject 2")
                .content("Test Content 2")
                .status(Notification.NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        List<Notification> notifications = Arrays.asList(notification1, notification2);
        when(notificationService.getUnreadNotifications(userId)).thenReturn(notifications);

        // When & Then
        mockMvc.perform(get("/api/notifications/unread/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].userId").value(userId.toString()))
                .andExpect(jsonPath("$[1].status").value("PENDING"));
    }

    @Test
    void testGetDeliveryNotifications() throws Exception {
        // Given
        UUID deliveryId = UUID.randomUUID();

        Notification notification1 = Notification.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .deliveryId(deliveryId)
                .type(Notification.NotificationType.DELIVERY_CREATED)
                .channel(Notification.NotificationChannel.EMAIL)
                .recipient("test1@example.com")
                .subject("Test Subject 1")
                .content("Test Content 1")
                .status(Notification.NotificationStatus.SENT)
                .createdAt(LocalDateTime.now())
                .build();

        Notification notification2 = Notification.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .deliveryId(deliveryId)
                .type(Notification.NotificationType.DELIVERY_DISPATCHED)
                .channel(Notification.NotificationChannel.SMS)
                .recipient("+33612345678")
                .subject("Test Subject 2")
                .content("Test Content 2")
                .status(Notification.NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        List<Notification> notifications = Arrays.asList(notification1, notification2);
        when(notificationService.getNotificationsByDeliveryId(deliveryId)).thenReturn(notifications);

        // When & Then
        mockMvc.perform(get("/api/notifications/delivery/{deliveryId}", deliveryId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].deliveryId").value(deliveryId.toString()))
                .andExpect(jsonPath("$[0].type").value("DELIVERY_CREATED"))
                .andExpect(jsonPath("$[1].deliveryId").value(deliveryId.toString()))
                .andExpect(jsonPath("$[1].type").value("DELIVERY_DISPATCHED"));
    }

    @Test
    void testCreateNotification() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        Notification notification = Notification.builder()
                .userId(userId)
                .deliveryId(deliveryId)
                .type(Notification.NotificationType.DELIVERY_CREATED)
                .channel(Notification.NotificationChannel.EMAIL)
                .recipient("test@example.com")
                .subject("Test Subject")
                .content("Test Content")
                .build();

        Notification createdNotification = Notification.builder()
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

        when(notificationService.createAndSendNotification(
                userId, deliveryId, 
                Notification.NotificationType.DELIVERY_CREATED,
                Notification.NotificationChannel.EMAIL,
                "test@example.com", "Test Subject", "Test Content"))
                .thenReturn(createdNotification);

        // When & Then
        mockMvc.perform(post("/api/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(notification)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.deliveryId").value(deliveryId.toString()))
                .andExpect(jsonPath("$.type").value("DELIVERY_CREATED"))
                .andExpect(jsonPath("$.channel").value("EMAIL"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testMarkAsRead() throws Exception {
        // Given
        UUID notificationId = UUID.randomUUID();

        Notification notification = Notification.builder()
                .id(notificationId)
                .userId(UUID.randomUUID())
                .deliveryId(UUID.randomUUID())
                .type(Notification.NotificationType.DELIVERY_CREATED)
                .channel(Notification.NotificationChannel.EMAIL)
                .recipient("test@example.com")
                .subject("Test Subject")
                .content("Test Content")
                .status(Notification.NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Notification updatedNotification = Notification.builder()
                .id(notificationId)
                .userId(UUID.randomUUID())
                .deliveryId(UUID.randomUUID())
                .type(Notification.NotificationType.DELIVERY_CREATED)
                .channel(Notification.NotificationChannel.EMAIL)
                .recipient("test@example.com")
                .subject("Test Subject")
                .content("Test Content")
                .status(Notification.NotificationStatus.READ)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationService.updateNotificationStatus(
                notificationId, 
                Notification.NotificationStatus.READ, 
                null))
                .thenReturn(updatedNotification);

        // When & Then
        mockMvc.perform(put("/api/notifications/{notificationId}/read", notificationId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(notificationId.toString()))
                .andExpect(jsonPath("$.status").value("READ"));
    }

    @Test
    void testDeleteNotification() throws Exception {
        // Given
        UUID notificationId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(delete("/api/notifications/{notificationId}", notificationId))
                .andExpect(status().isOk());
    }

    @Test
    void testProcessPendingNotifications() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/notifications/process/{channel}", Notification.NotificationChannel.EMAIL))
                .andExpect(status().isOk())
                .andExpect(content().string("Notifications processed successfully"));
    }
}
