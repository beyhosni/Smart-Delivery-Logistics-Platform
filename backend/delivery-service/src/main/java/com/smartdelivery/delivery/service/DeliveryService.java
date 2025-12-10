package com.smartdelivery.delivery.service;

import com.smartdelivery.delivery.model.Delivery;
import com.smartdelivery.delivery.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.routingkey.created}")
    private String createdRoutingKey;

    public Delivery createDelivery(Delivery delivery) {
        delivery.setId(UUID.randomUUID());
        delivery.setStatus(Delivery.DeliveryStatus.CREATED);
        delivery.setCreatedAt(LocalDateTime.now());

        Delivery savedDelivery = deliveryRepository.save(delivery);

        // Publier l'événement delivery.created
        publishDeliveryCreatedEvent(savedDelivery);

        log.info("Created delivery with ID: {}", savedDelivery.getId());
        return savedDelivery;
    }

    public Delivery getDeliveryById(UUID id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Delivery not found with ID: " + id));
    }

    public List<Delivery> getAllDeliveries() {
        return deliveryRepository.findAll();
    }

    public List<Delivery> getDeliveriesBySenderId(UUID senderId) {
        return deliveryRepository.findBySenderId(senderId);
    }

    public List<Delivery> getDeliveriesByRecipientId(UUID recipientId) {
        return deliveryRepository.findByRecipientId(recipientId);
    }

    public List<Delivery> getDeliveriesByStatus(Delivery.DeliveryStatus status) {
        return deliveryRepository.findByStatus(status);
    }

    public Delivery updateDeliveryStatus(UUID id, Delivery.DeliveryStatus status) {
        Delivery delivery = getDeliveryById(id);
        delivery.setStatus(status);
        delivery.setUpdatedAt(LocalDateTime.now());

        Delivery updatedDelivery = deliveryRepository.save(delivery);

        // Publier l'événement approprié en fonction du statut
        switch (status) {
            case DISPATCHED:
                publishDeliveryDispatchedEvent(updatedDelivery);
                break;
            case IN_TRANSIT:
                publishDeliveryInTransitEvent(updatedDelivery);
                break;
            case DELIVERED:
                publishDeliveryDeliveredEvent(updatedDelivery);
                break;
            default:
                break;
        }

        log.info("Updated delivery status to {} for ID: {}", status, id);
        return updatedDelivery;
    }

    private void publishDeliveryCreatedEvent(Delivery delivery) {
        rabbitTemplate.convertAndSend(exchangeName, createdRoutingKey, delivery);
        log.info("Published delivery.created event for delivery ID: {}", delivery.getId());
    }

    private void publishDeliveryDispatchedEvent(Delivery delivery) {
        rabbitTemplate.convertAndSend(exchangeName, "delivery.dispatched", delivery);
        log.info("Published delivery.dispatched event for delivery ID: {}", delivery.getId());
    }

    private void publishDeliveryInTransitEvent(Delivery delivery) {
        rabbitTemplate.convertAndSend(exchangeName, "delivery.in_transit", delivery);
        log.info("Published delivery.in_transit event for delivery ID: {}", delivery.getId());
    }

    private void publishDeliveryDeliveredEvent(Delivery delivery) {
        rabbitTemplate.convertAndSend(exchangeName, "delivery.delivered", delivery);
        log.info("Published delivery.delivered event for delivery ID: {}", delivery.getId());
    }
}
