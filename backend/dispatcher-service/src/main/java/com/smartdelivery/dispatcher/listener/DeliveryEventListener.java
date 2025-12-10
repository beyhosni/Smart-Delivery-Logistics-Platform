package com.smartdelivery.dispatcher.listener;

import com.smartdelivery.dispatcher.service.DispatcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryEventListener {

    private final DispatcherService dispatcherService;

    @RabbitListener(queues = "${rabbitmq.queue.created.name}")
    public void handleDeliveryCreated(Map<String, Object> message) {
        try {
            log.info("Received delivery.created event");

            // Extraire les informations du message
            UUID deliveryId = UUID.fromString(message.get("id").toString());

            // Obtenir les coordonnées de l'adresse de ramassage
            Map<String, Object> pickupAddress = (Map<String, Object>) message.get("pickupAddress");
            Map<String, Object> coordinates = (Map<String, Object>) pickupAddress.get("coordinates");

            Double pickupLatitude = Double.parseDouble(coordinates.get("latitude").toString());
            Double pickupLongitude = Double.parseDouble(coordinates.get("longitude").toString());

            // Attribuer automatiquement la livraison au livreur le plus proche
            dispatcherService.assignDelivery(deliveryId, pickupLatitude, pickupLongitude);

        } catch (Exception e) {
            log.error("Error processing delivery.created event", e);
        }
    }
}
