
package com.smartdelivery.delivery.service;

import com.smartdelivery.delivery.model.Delivery;
import com.smartdelivery.delivery.model.Address;
import com.smartdelivery.delivery.model.PackageDetails;
import com.smartdelivery.delivery.repository.DeliveryRepository;
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
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private DeliveryService deliveryService;

    private Delivery testDelivery;
    private Address pickupAddress;
    private Address deliveryAddress;
    private PackageDetails packageDetails;

    @BeforeEach
    void setUp() {
        // Configuration des valeurs pour les champs annotés avec @Value
        ReflectionTestUtils.setField(deliveryService, "exchangeName", "delivery.exchange");
        ReflectionTestUtils.setField(deliveryService, "createdRoutingKey", "delivery.created");

        // Création d'objets de test
        pickupAddress = Address.builder()
                .street("123 Pickup St")
                .city("City")
                .postalCode("12345")
                .country("Country")
                .coordinates(new com.smartdelivery.delivery.model.Coordinates(48.8566, 2.3522))
                .build();

        deliveryAddress = Address.builder()
                .street("456 Delivery Ave")
                .city("City")
                .postalCode("54321")
                .country("Country")
                .coordinates(new com.smartdelivery.delivery.model.Coordinates(48.8584, 2.2945))
                .build();

        packageDetails = PackageDetails.builder()
                .weight(2.5)
                .dimensions(new com.smartdelivery.delivery.model.Dimensions(30, 20, 10))
                .build();

        testDelivery = Delivery.builder()
                .senderId(UUID.randomUUID())
                .recipientId(UUID.randomUUID())
                .pickupAddress(pickupAddress)
                .deliveryAddress(deliveryAddress)
                .packageDetails(packageDetails)
                .priority(Delivery.DeliveryPriority.NORMAL)
                .requestedDeliveryTime(LocalDateTime.now().plusDays(1))
                .status(Delivery.DeliveryStatus.CREATED)
                .build();
    }

    @Test
    void createDelivery_ShouldSaveDeliveryAndPublishEvent() {
        // Given
        Delivery savedDelivery = Delivery.builder()
                .id(UUID.randomUUID())
                .senderId(testDelivery.getSenderId())
                .recipientId(testDelivery.getRecipientId())
                .pickupAddress(testDelivery.getPickupAddress())
                .deliveryAddress(testDelivery.getDeliveryAddress())
                .packageDetails(testDelivery.getPackageDetails())
                .priority(testDelivery.getPriority())
                .requestedDeliveryTime(testDelivery.getRequestedDeliveryTime())
                .status(Delivery.DeliveryStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();

        when(deliveryRepository.save(any(Delivery.class))).thenReturn(savedDelivery);

        // When
        Delivery result = deliveryService.createDelivery(testDelivery);

        // Then
        assertNotNull(result.getId());
        assertEquals(Delivery.DeliveryStatus.CREATED, result.getStatus());
        assertNotNull(result.getCreatedAt());
        verify(deliveryRepository, times(1)).save(any(Delivery.class));
        verify(rabbitTemplate, times(1)).convertAndSend(eq("delivery.exchange"), eq("delivery.created"), any(Delivery.class));
    }

    @Test
    void getDeliveryById_WithValidId_ShouldReturnDelivery() {
        // Given
        UUID deliveryId = UUID.randomUUID();
        testDelivery.setId(deliveryId);
        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(testDelivery));

        // When
        Delivery result = deliveryService.getDeliveryById(deliveryId);

        // Then
        assertNotNull(result);
        assertEquals(deliveryId, result.getId());
        verify(deliveryRepository, times(1)).findById(deliveryId);
    }

    @Test
    void getDeliveryById_WithInvalidId_ShouldThrowException() {
        // Given
        UUID deliveryId = UUID.randomUUID();
        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> deliveryService.getDeliveryById(deliveryId));
        verify(deliveryRepository, times(1)).findById(deliveryId);
    }

    @Test
    void getAllDeliveries_ShouldReturnAllDeliveries() {
        // Given
        List<Delivery> deliveries = Arrays.asList(testDelivery);
        when(deliveryRepository.findAll()).thenReturn(deliveries);

        // When
        List<Delivery> result = deliveryService.getAllDeliveries();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(deliveryRepository, times(1)).findAll();
    }

    @Test
    void getDeliveriesBySenderId_ShouldReturnDeliveriesForSender() {
        // Given
        UUID senderId = UUID.randomUUID();
        List<Delivery> deliveries = Arrays.asList(testDelivery);
        when(deliveryRepository.findBySenderId(senderId)).thenReturn(deliveries);

        // When
        List<Delivery> result = deliveryService.getDeliveriesBySenderId(senderId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(deliveryRepository, times(1)).findBySenderId(senderId);
    }

    @Test
    void getDeliveriesByRecipientId_ShouldReturnDeliveriesForRecipient() {
        // Given
        UUID recipientId = UUID.randomUUID();
        List<Delivery> deliveries = Arrays.asList(testDelivery);
        when(deliveryRepository.findByRecipientId(recipientId)).thenReturn(deliveries);

        // When
        List<Delivery> result = deliveryService.getDeliveriesByRecipientId(recipientId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(deliveryRepository, times(1)).findByRecipientId(recipientId);
    }

    @Test
    void getDeliveriesByStatus_ShouldReturnDeliveriesWithStatus() {
        // Given
        Delivery.DeliveryStatus status = Delivery.DeliveryStatus.CREATED;
        List<Delivery> deliveries = Arrays.asList(testDelivery);
        when(deliveryRepository.findByStatus(status)).thenReturn(deliveries);

        // When
        List<Delivery> result = deliveryService.getDeliveriesByStatus(status);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(status, result.get(0).getStatus());
        verify(deliveryRepository, times(1)).findByStatus(status);
    }

    @Test
    void updateDeliveryStatus_ToDispatched_ShouldPublishDispatchedEvent() {
        // Given
        UUID deliveryId = UUID.randomUUID();
        testDelivery.setId(deliveryId);
        testDelivery.setStatus(Delivery.DeliveryStatus.CREATED);

        Delivery updatedDelivery = Delivery.builder()
                .id(deliveryId)
                .senderId(testDelivery.getSenderId())
                .recipientId(testDelivery.getRecipientId())
                .pickupAddress(testDelivery.getPickupAddress())
                .deliveryAddress(testDelivery.getDeliveryAddress())
                .packageDetails(testDelivery.getPackageDetails())
                .priority(testDelivery.getPriority())
                .requestedDeliveryTime(testDelivery.getRequestedDeliveryTime())
                .status(Delivery.DeliveryStatus.DISPATCHED)
                .createdAt(testDelivery.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(testDelivery));
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(updatedDelivery);

        // When
        Delivery result = deliveryService.updateDeliveryStatus(deliveryId, Delivery.DeliveryStatus.DISPATCHED);

        // Then
        assertNotNull(result);
        assertEquals(Delivery.DeliveryStatus.DISPATCHED, result.getStatus());
        assertNotNull(result.getUpdatedAt());
        verify(deliveryRepository, times(1)).save(any(Delivery.class));
        verify(rabbitTemplate, times(1)).convertAndSend(eq("delivery.exchange"), eq("delivery.dispatched"), any(Delivery.class));
    }

    @Test
    void updateDeliveryStatus_ToInTransit_ShouldPublishInTransitEvent() {
        // Given
        UUID deliveryId = UUID.randomUUID();
        testDelivery.setId(deliveryId);
        testDelivery.setStatus(Delivery.DeliveryStatus.DISPATCHED);

        Delivery updatedDelivery = Delivery.builder()
                .id(deliveryId)
                .senderId(testDelivery.getSenderId())
                .recipientId(testDelivery.getRecipientId())
                .pickupAddress(testDelivery.getPickupAddress())
                .deliveryAddress(testDelivery.getDeliveryAddress())
                .packageDetails(testDelivery.getPackageDetails())
                .priority(testDelivery.getPriority())
                .requestedDeliveryTime(testDelivery.getRequestedDeliveryTime())
                .status(Delivery.DeliveryStatus.IN_TRANSIT)
                .createdAt(testDelivery.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(testDelivery));
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(updatedDelivery);

        // When
        Delivery result = deliveryService.updateDeliveryStatus(deliveryId, Delivery.DeliveryStatus.IN_TRANSIT);

        // Then
        assertNotNull(result);
        assertEquals(Delivery.DeliveryStatus.IN_TRANSIT, result.getStatus());
        assertNotNull(result.getUpdatedAt());
        verify(deliveryRepository, times(1)).save(any(Delivery.class));
        verify(rabbitTemplate, times(1)).convertAndSend(eq("delivery.exchange"), eq("delivery.in_transit"), any(Delivery.class));
    }

    @Test
    void updateDeliveryStatus_ToDelivered_ShouldPublishDeliveredEvent() {
        // Given
        UUID deliveryId = UUID.randomUUID();
        testDelivery.setId(deliveryId);
        testDelivery.setStatus(Delivery.DeliveryStatus.IN_TRANSIT);

        Delivery updatedDelivery = Delivery.builder()
                .id(deliveryId)
                .senderId(testDelivery.getSenderId())
                .recipientId(testDelivery.getRecipientId())
                .pickupAddress(testDelivery.getPickupAddress())
                .deliveryAddress(testDelivery.getDeliveryAddress())
                .packageDetails(testDelivery.getPackageDetails())
                .priority(testDelivery.getPriority())
                .requestedDeliveryTime(testDelivery.getRequestedDeliveryTime())
                .status(Delivery.DeliveryStatus.DELIVERED)
                .createdAt(testDelivery.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(testDelivery));
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(updatedDelivery);

        // When
        Delivery result = deliveryService.updateDeliveryStatus(deliveryId, Delivery.DeliveryStatus.DELIVERED);

        // Then
        assertNotNull(result);
        assertEquals(Delivery.DeliveryStatus.DELIVERED, result.getStatus());
        assertNotNull(result.getUpdatedAt());
        verify(deliveryRepository, times(1)).save(any(Delivery.class));
        verify(rabbitTemplate, times(1)).convertAndSend(eq("delivery.exchange"), eq("delivery.delivered"), any(Delivery.class));
    }

    @Test
    void updateDeliveryStatus_ToOtherStatus_ShouldNotPublishEvent() {
        // Given
        UUID deliveryId = UUID.randomUUID();
        testDelivery.setId(deliveryId);
        testDelivery.setStatus(Delivery.DeliveryStatus.CREATED);

        Delivery updatedDelivery = Delivery.builder()
                .id(deliveryId)
                .senderId(testDelivery.getSenderId())
                .recipientId(testDelivery.getRecipientId())
                .pickupAddress(testDelivery.getPickupAddress())
                .deliveryAddress(testDelivery.getDeliveryAddress())
                .packageDetails(testDelivery.getPackageDetails())
                .priority(testDelivery.getPriority())
                .requestedDeliveryTime(testDelivery.getRequestedDeliveryTime())
                .status(Delivery.DeliveryStatus.CANCELLED)
                .createdAt(testDelivery.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(testDelivery));
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(updatedDelivery);

        // When
        Delivery result = deliveryService.updateDeliveryStatus(deliveryId, Delivery.DeliveryStatus.CANCELLED);

        // Then
        assertNotNull(result);
        assertEquals(Delivery.DeliveryStatus.CANCELLED, result.getStatus());
        assertNotNull(result.getUpdatedAt());
        verify(deliveryRepository, times(1)).save(any(Delivery.class));
        // Vérifier qu'aucun événement n'est publié pour les statuts non suivis
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Delivery.class));
    }
}
