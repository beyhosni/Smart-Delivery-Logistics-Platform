
package com.smartdelivery.tracking.service;

import com.smartdelivery.tracking.model.DeliveryTracking;
import com.smartdelivery.tracking.model.Location;
import com.smartdelivery.tracking.model.LocationUpdate;
import com.smartdelivery.tracking.model.TrackingEvent;
import com.smartdelivery.tracking.repository.DeliveryTrackingRepository;
import com.smartdelivery.tracking.repository.LocationUpdateRepository;
import com.smartdelivery.tracking.repository.TrackingEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {

    @Mock
    private DeliveryTrackingRepository deliveryTrackingRepository;

    @Mock
    private LocationUpdateRepository locationUpdateRepository;

    @Mock
    private TrackingEventRepository trackingEventRepository;

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private TrackingService trackingService;

    private DeliveryTracking testTracking;
    private LocationUpdate testLocationUpdate;
    private TrackingEvent testTrackingEvent;

    @BeforeEach
    void setUp() {
        // Configuration des valeurs pour les champs annotés avec @Value
        ReflectionTestUtils.setField(trackingService, "exchangeName", "delivery.exchange");
        ReflectionTestUtils.setField(trackingService, "inTransitRoutingKey", "delivery.in_transit");
        ReflectionTestUtils.setField(trackingService, "deliveredRoutingKey", "delivery.delivered");

        // Création d'objets de test
        UUID deliveryId = UUID.randomUUID();
        UUID courierId = UUID.randomUUID();

        testTracking = DeliveryTracking.builder()
                .id(UUID.randomUUID())
                .deliveryId(deliveryId)
                .courierId(courierId)
                .status(DeliveryTracking.TrackingStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testLocationUpdate = LocationUpdate.builder()
                .id(UUID.randomUUID())
                .deliveryId(deliveryId)
                .courierId(courierId)
                .location(new Location(48.8566, 2.3522))
                .timestamp(LocalDateTime.now())
                .build();

        testTrackingEvent = TrackingEvent.builder()
                .id(UUID.randomUUID())
                .deliveryId(deliveryId)
                .eventType(TrackingEvent.EventType.LOCATION_UPDATE)
                .description("Location updated")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    void createTrackingForDelivery_ShouldCreateTrackingAndPublishEvent() {
        // Given
        UUID deliveryId = UUID.randomUUID();
        UUID courierId = UUID.randomUUID();

        when(deliveryTrackingRepository.save(any(DeliveryTracking.class))).thenReturn(testTracking);

        // When
        DeliveryTracking result = trackingService.createTrackingForDelivery(deliveryId, courierId);

        // Then
        assertNotNull(result);
        assertEquals(deliveryId, result.getDeliveryId());
        assertEquals(courierId, result.getCourierId());
        assertEquals(DeliveryTracking.TrackingStatus.CREATED, result.getStatus());
        assertNotNull(result.getCreatedAt());

        verify(deliveryTrackingRepository, times(1)).save(any(DeliveryTracking.class));
    }

    @Test
    void getTrackingByDeliveryId_ShouldReturnTracking() {
        // Given
        UUID deliveryId = testTracking.getDeliveryId();
        when(deliveryTrackingRepository.findByDeliveryId(deliveryId)).thenReturn(Optional.of(testTracking));

        // When
        DeliveryTracking result = trackingService.getTrackingByDeliveryId(deliveryId);

        // Then
        assertNotNull(result);
        assertEquals(deliveryId, result.getDeliveryId());
        verify(deliveryTrackingRepository, times(1)).findByDeliveryId(deliveryId);
    }

    @Test
    void getTrackingByDeliveryId_WithNonExistentDelivery_ShouldThrowException() {
        // Given
        UUID deliveryId = UUID.randomUUID();
        when(deliveryTrackingRepository.findByDeliveryId(deliveryId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> trackingService.getTrackingByDeliveryId(deliveryId));
        verify(deliveryTrackingRepository, times(1)).findByDeliveryId(deliveryId);
    }

    @Test
    void updateLocation_ShouldSaveLocationUpdateAndNotify() {
        // Given
        UUID deliveryId = testLocationUpdate.getDeliveryId();
        Location newLocation = new Location(48.8570, 2.3400);

        when(locationUpdateRepository.save(any(LocationUpdate.class))).thenReturn(testLocationUpdate);
        when(deliveryTrackingRepository.findByDeliveryId(deliveryId)).thenReturn(Optional.of(testTracking));

        // When
        LocationUpdate result = trackingService.updateLocation(deliveryId, newLocation);

        // Then
        assertNotNull(result);
        assertEquals(deliveryId, result.getDeliveryId());
        assertEquals(newLocation, result.getLocation());

        verify(locationUpdateRepository, times(1)).save(any(LocationUpdate.class));
        verify(webSocketService, times(1)).sendLocationUpdate(eq(deliveryId), any(LocationUpdate.class));
    }

    @Test
    void getLocationHistory_ShouldReturnLocationUpdates() {
        // Given
        UUID deliveryId = testLocationUpdate.getDeliveryId();
        List<LocationUpdate> locationUpdates = Arrays.asList(testLocationUpdate);
        when(locationUpdateRepository.findByDeliveryIdOrderByTimestampDesc(deliveryId)).thenReturn(locationUpdates);

        // When
        List<LocationUpdate> result = trackingService.getLocationHistory(deliveryId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(deliveryId, result.get(0).getDeliveryId());
        verify(locationUpdateRepository, times(1)).findByDeliveryIdOrderByTimestampDesc(deliveryId);
    }

    @Test
    void updateTrackingStatus_ShouldUpdateStatusAndPublishEvent() {
        // Given
        UUID deliveryId = testTracking.getDeliveryId();
        DeliveryTracking.TrackingStatus newStatus = DeliveryTracking.TrackingStatus.IN_TRANSIT;

        DeliveryTracking updatedTracking = DeliveryTracking.builder()
                .id(testTracking.getId())
                .deliveryId(testTracking.getDeliveryId())
                .courierId(testTracking.getCourierId())
                .status(newStatus)
                .createdAt(testTracking.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(deliveryTrackingRepository.findByDeliveryId(deliveryId)).thenReturn(Optional.of(testTracking));
        when(deliveryTrackingRepository.save(any(DeliveryTracking.class))).thenReturn(updatedTracking);

        // When
        DeliveryTracking result = trackingService.updateTrackingStatus(deliveryId, newStatus);

        // Then
        assertNotNull(result);
        assertEquals(newStatus, result.getStatus());
        assertNotNull(result.getUpdatedAt());

        verify(deliveryTrackingRepository, times(1)).findByDeliveryId(deliveryId);
        verify(deliveryTrackingRepository, times(1)).save(any(DeliveryTracking.class));
        verify(rabbitTemplate, times(1)).convertAndSend(eq("delivery.exchange"), eq("delivery.in_transit"), any());
    }

    @Test
    void addTrackingEvent_ShouldSaveEvent() {
        // Given
        UUID deliveryId = testTrackingEvent.getDeliveryId();
        TrackingEvent.EventType eventType = TrackingEvent.EventType.LOCATION_UPDATE;
        String description = "Test event";

        when(trackingEventRepository.save(any(TrackingEvent.class))).thenReturn(testTrackingEvent);

        // When
        TrackingEvent result = trackingService.addTrackingEvent(deliveryId, eventType, description);

        // Then
        assertNotNull(result);
        assertEquals(deliveryId, result.getDeliveryId());
        assertEquals(eventType, result.getEventType());
        assertEquals(description, result.getDescription());

        verify(trackingEventRepository, times(1)).save(any(TrackingEvent.class));
    }

    @Test
    void getTrackingEvents_ShouldReturnEvents() {
        // Given
        UUID deliveryId = testTrackingEvent.getDeliveryId();
        List<TrackingEvent> trackingEvents = Arrays.asList(testTrackingEvent);
        when(trackingEventRepository.findByDeliveryIdOrderByTimestampDesc(deliveryId)).thenReturn(trackingEvents);

        // When
        List<TrackingEvent> result = trackingService.getTrackingEvents(deliveryId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(deliveryId, result.get(0).getDeliveryId());
        verify(trackingEventRepository, times(1)).findByDeliveryIdOrderByTimestampDesc(deliveryId);
    }

    @Test
    void markAsDelivered_ShouldUpdateStatusAndPublishEvent() {
        // Given
        UUID deliveryId = testTracking.getDeliveryId();
        Location deliveryLocation = new Location(48.8584, 2.2945);
        String recipientName = "John Doe";
        String signature = "base64-signature";
        String photo = "base64-photo";

        DeliveryTracking updatedTracking = DeliveryTracking.builder()
                .id(testTracking.getId())
                .deliveryId(testTracking.getDeliveryId())
                .courierId(testTracking.getCourierId())
                .status(DeliveryTracking.TrackingStatus.DELIVERED)
                .createdAt(testTracking.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .deliveryLocation(deliveryLocation)
                .deliveryTime(LocalDateTime.now())
                .recipientName(recipientName)
                .signature(signature)
                .photo(photo)
                .build();

        when(deliveryTrackingRepository.findByDeliveryId(deliveryId)).thenReturn(Optional.of(testTracking));
        when(deliveryTrackingRepository.save(any(DeliveryTracking.class))).thenReturn(updatedTracking);

        // When
        DeliveryTracking result = trackingService.markAsDelivered(deliveryId, deliveryLocation, recipientName, signature, photo);

        // Then
        assertNotNull(result);
        assertEquals(DeliveryTracking.TrackingStatus.DELIVERED, result.getStatus());
        assertEquals(deliveryLocation, result.getDeliveryLocation());
        assertEquals(recipientName, result.getRecipientName());
        assertEquals(signature, result.getSignature());
        assertEquals(photo, result.getPhoto());
        assertNotNull(result.getDeliveryTime());

        verify(deliveryTrackingRepository, times(1)).findByDeliveryId(deliveryId);
        verify(deliveryTrackingRepository, times(1)).save(any(DeliveryTracking.class));
        verify(rabbitTemplate, times(1)).convertAndSend(eq("delivery.exchange"), eq("delivery.delivered"), any());
    }
}
