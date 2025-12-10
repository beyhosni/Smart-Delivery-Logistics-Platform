package com.smartdelivery.delivery.controller;

import com.smartdelivery.delivery.model.Delivery;
import com.smartdelivery.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
@Slf4j
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping
    public ResponseEntity<Delivery> createDelivery(@Valid @RequestBody Delivery delivery) {
        log.info("Creating new delivery");
        Delivery createdDelivery = deliveryService.createDelivery(delivery);
        return new ResponseEntity<>(createdDelivery, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Delivery> getDeliveryById(@PathVariable UUID id) {
        log.info("Getting delivery with ID: {}", id);
        Delivery delivery = deliveryService.getDeliveryById(id);
        return ResponseEntity.ok(delivery);
    }

    @GetMapping
    public ResponseEntity<List<Delivery>> getAllDeliveries() {
        log.info("Getting all deliveries");
        List<Delivery> deliveries = deliveryService.getAllDeliveries();
        return ResponseEntity.ok(deliveries);
    }

    @GetMapping("/sender/{senderId}")
    public ResponseEntity<List<Delivery>> getDeliveriesBySenderId(@PathVariable UUID senderId) {
        log.info("Getting deliveries for sender ID: {}", senderId);
        List<Delivery> deliveries = deliveryService.getDeliveriesBySenderId(senderId);
        return ResponseEntity.ok(deliveries);
    }

    @GetMapping("/recipient/{recipientId}")
    public ResponseEntity<List<Delivery>> getDeliveriesByRecipientId(@PathVariable UUID recipientId) {
        log.info("Getting deliveries for recipient ID: {}", recipientId);
        List<Delivery> deliveries = deliveryService.getDeliveriesByRecipientId(recipientId);
        return ResponseEntity.ok(deliveries);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Delivery>> getDeliveriesByStatus(@PathVariable Delivery.DeliveryStatus status) {
        log.info("Getting deliveries with status: {}", status);
        List<Delivery> deliveries = deliveryService.getDeliveriesByStatus(status);
        return ResponseEntity.ok(deliveries);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Delivery> updateDeliveryStatus(
            @PathVariable UUID id,
            @RequestParam Delivery.DeliveryStatus status) {
        log.info("Updating delivery status to {} for ID: {}", status, id);
        Delivery updatedDelivery = deliveryService.updateDeliveryStatus(id, status);
        return ResponseEntity.ok(updatedDelivery);
    }
}
