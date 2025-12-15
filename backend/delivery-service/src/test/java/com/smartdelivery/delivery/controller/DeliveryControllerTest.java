
package com.smartdelivery.delivery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdelivery.delivery.model.Delivery;
import com.smartdelivery.delivery.model.Address;
import com.smartdelivery.delivery.model.PackageDetails;
import com.smartdelivery.delivery.service.DeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeliveryController.class)
class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeliveryService deliveryService;

    @Autowired
    private ObjectMapper objectMapper;

    private Delivery testDelivery;
    private Address pickupAddress;
    private Address deliveryAddress;
    private PackageDetails packageDetails;

    @BeforeEach
    void setUp() {
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
                .id(UUID.randomUUID())
                .senderId(UUID.randomUUID())
                .recipientId(UUID.randomUUID())
                .pickupAddress(pickupAddress)
                .deliveryAddress(deliveryAddress)
                .packageDetails(packageDetails)
                .priority(Delivery.DeliveryPriority.NORMAL)
                .requestedDeliveryTime(LocalDateTime.now().plusDays(1))
                .status(Delivery.DeliveryStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createDelivery_ShouldReturnCreatedDelivery() throws Exception {
        // Given
        Delivery newDelivery = Delivery.builder()
                .senderId(testDelivery.getSenderId())
                .recipientId(testDelivery.getRecipientId())
                .pickupAddress(testDelivery.getPickupAddress())
                .deliveryAddress(testDelivery.getDeliveryAddress())
                .packageDetails(testDelivery.getPackageDetails())
                .priority(testDelivery.getPriority())
                .requestedDeliveryTime(testDelivery.getRequestedDeliveryTime())
                .build();

        when(deliveryService.createDelivery(any(Delivery.class))).thenReturn(testDelivery);

        // When & Then
        mockMvc.perform(post("/api/deliveries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newDelivery)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.senderId").value(testDelivery.getSenderId().toString()))
                .andExpect(jsonPath("$.recipientId").value(testDelivery.getRecipientId().toString()));
    }

    @Test
    void getDeliveryById_WithValidId_ShouldReturnDelivery() throws Exception {
        // Given
        UUID deliveryId = testDelivery.getId();
        when(deliveryService.getDeliveryById(deliveryId)).thenReturn(testDelivery);

        // When & Then
        mockMvc.perform(get("/api/deliveries/{id}", deliveryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deliveryId.toString()))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void getAllDeliveries_ShouldReturnAllDeliveries() throws Exception {
        // Given
        List<Delivery> deliveries = Arrays.asList(testDelivery);
        when(deliveryService.getAllDeliveries()).thenReturn(deliveries);

        // When & Then
        mockMvc.perform(get("/api/deliveries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(testDelivery.getId().toString()));
    }

    @Test
    void getDeliveriesBySenderId_ShouldReturnDeliveriesForSender() throws Exception {
        // Given
        UUID senderId = testDelivery.getSenderId();
        List<Delivery> deliveries = Arrays.asList(testDelivery);
        when(deliveryService.getDeliveriesBySenderId(senderId)).thenReturn(deliveries);

        // When & Then
        mockMvc.perform(get("/api/deliveries/sender/{senderId}", senderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].senderId").value(senderId.toString()));
    }

    @Test
    void getDeliveriesByRecipientId_ShouldReturnDeliveriesForRecipient() throws Exception {
        // Given
        UUID recipientId = testDelivery.getRecipientId();
        List<Delivery> deliveries = Arrays.asList(testDelivery);
        when(deliveryService.getDeliveriesByRecipientId(recipientId)).thenReturn(deliveries);

        // When & Then
        mockMvc.perform(get("/api/deliveries/recipient/{recipientId}", recipientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].recipientId").value(recipientId.toString()));
    }

    @Test
    void getDeliveriesByStatus_ShouldReturnDeliveriesWithStatus() throws Exception {
        // Given
        Delivery.DeliveryStatus status = Delivery.DeliveryStatus.CREATED;
        List<Delivery> deliveries = Arrays.asList(testDelivery);
        when(deliveryService.getDeliveriesByStatus(status)).thenReturn(deliveries);

        // When & Then
        mockMvc.perform(get("/api/deliveries/status/{status}", status))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value(status.toString()));
    }

    @Test
    void updateDeliveryStatus_ShouldReturnUpdatedDelivery() throws Exception {
        // Given
        UUID deliveryId = testDelivery.getId();
        Delivery.DeliveryStatus newStatus = Delivery.DeliveryStatus.DISPATCHED;

        Delivery updatedDelivery = Delivery.builder()
                .id(deliveryId)
                .senderId(testDelivery.getSenderId())
                .recipientId(testDelivery.getRecipientId())
                .pickupAddress(testDelivery.getPickupAddress())
                .deliveryAddress(testDelivery.getDeliveryAddress())
                .packageDetails(testDelivery.getPackageDetails())
                .priority(testDelivery.getPriority())
                .requestedDeliveryTime(testDelivery.getRequestedDeliveryTime())
                .status(newStatus)
                .createdAt(testDelivery.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(deliveryService.updateDeliveryStatus(eq(deliveryId), eq(newStatus))).thenReturn(updatedDelivery);

        // When & Then
        mockMvc.perform(put("/api/deliveries/{id}/status", deliveryId)
                .param("status", newStatus.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deliveryId.toString()))
                .andExpect(jsonPath("$.status").value(newStatus.toString()))
                .andExpect(jsonPath("$.updatedAt").exists());
    }
}
