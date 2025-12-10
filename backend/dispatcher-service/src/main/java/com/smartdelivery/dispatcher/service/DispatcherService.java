package com.smartdelivery.dispatcher.service;

import com.smartdelivery.dispatcher.model.*;
import com.smartdelivery.dispatcher.repository.CourierRepository;
import com.smartdelivery.dispatcher.repository.DeliveryAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatcherService {

    private final CourierRepository courierRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final RouteOptimizerClient routeOptimizerClient;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    /**
     * Attribue automatiquement une livraison au livreur le plus proche et disponible
     * @param deliveryId ID de la livraison
     * @param pickupLatitude Latitude du point de ramassage
     * @param pickupLongitude Longitude du point de ramassage
     * @return L'assignment créé
     */
    @Transactional
    public DeliveryAssignment assignDelivery(UUID deliveryId, Double pickupLatitude, Double pickupLongitude) {
        log.info("Assigning delivery {} to nearest available courier", deliveryId);

        // Définir la zone de recherche (environ 10 km autour du point de ramassage)
        Double searchRadius = 0.1; // environ 10 km
        Double minLatitude = pickupLatitude - searchRadius;
        Double maxLatitude = pickupLatitude + searchRadius;
        Double minLongitude = pickupLongitude - searchRadius;
        Double maxLongitude = pickupLongitude + searchRadius;

        // Trouver les livreurs disponibles dans la zone
        List<Courier> availableCouriers = courierRepository.findAvailableCouriersInArea(
                minLatitude, maxLatitude, minLongitude, maxLongitude);

        if (availableCouriers.isEmpty()) {
            // Si aucun livreur n'est trouvé dans la zone, élargir la recherche
            searchRadius = 0.2; // environ 20 km
            minLatitude = pickupLatitude - searchRadius;
            maxLatitude = pickupLatitude + searchRadius;
            minLongitude = pickupLongitude - searchRadius;
            maxLongitude = pickupLongitude + searchRadius;

            availableCouriers = courierRepository.findAvailableCouriersInArea(
                    minLatitude, maxLatitude, minLongitude, maxLongitude);
        }

        if (availableCouriers.isEmpty()) {
            throw new RuntimeException("Aucun livreur disponible pour cette livraison");
        }

        // Sélectionner le livreur le plus proche
        Courier nearestCourier = findNearestCourier(availableCouriers, pickupLatitude, pickupLongitude);

        // Créer l'assignment
        DeliveryAssignment assignment = DeliveryAssignment.builder()
                .courier(nearestCourier)
                .deliveryId(deliveryId)
                .status(DeliveryAssignment.AssignmentStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .build();

        // Mettre à jour le statut du livreur
        nearestCourier.setStatus(Courier.CourierStatus.BUSY);
        courierRepository.save(nearestCourier);

        // Sauvegarder l'assignment
        DeliveryAssignment savedAssignment = assignmentRepository.save(assignment);

        // Créer une route optimisée pour cette livraison
        try {
            // Obtenir les détails de la livraison (dans une vraie implémentation, 
            // nous appellerions le delivery-service)
            Double deliveryLatitude = 48.8584; // Exemple: Tour Eiffel
            Double deliveryLongitude = 2.2945;

            // Créer la route optimisée
            UUID routeId = routeOptimizerClient.createOptimizedRoute(
                    deliveryId, nearestCourier.getId(),
                    pickupLatitude, pickupLongitude,
                    deliveryLatitude, deliveryLongitude);

            log.info("Created optimized route {} for delivery {}", routeId, deliveryId);
        } catch (Exception e) {
            log.error("Error creating optimized route for delivery {}", deliveryId, e);
        }

        // Publier l'événement d'attribution
        publishDeliveryDispatchedEvent(savedAssignment);

        log.info("Assigned delivery {} to courier {}", deliveryId, nearestCourier.getId());
        return savedAssignment;
    }

    /**
     * Met à jour le statut d'un assignment
     * @param assignmentId ID de l'assignment
     * @param status Nouveau statut
     * @return L'assignment mis à jour
     */
    @Transactional
    public DeliveryAssignment updateAssignmentStatus(UUID assignmentId, DeliveryAssignment.AssignmentStatus status) {
        DeliveryAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment non trouvé avec l'ID: " + assignmentId));

        assignment.setStatus(status);
        assignment.setUpdatedAt(LocalDateTime.now());

        if (status == DeliveryAssignment.AssignmentStatus.COMPLETED) {
            assignment.setCompletedAt(LocalDateTime.now());
            // Libérer le livreur
            Courier courier = assignment.getCourier();
            courier.setStatus(Courier.CourierStatus.AVAILABLE);
            courierRepository.save(courier);
        }

        DeliveryAssignment savedAssignment = assignmentRepository.save(assignment);
        log.info("Updated assignment status to {} for assignment ID: {}", status, assignmentId);
        return savedAssignment;
    }

    /**
     * Récupère tous les assignments d'un livreur
     * @param courierId ID du livreur
     * @return La liste des assignments
     */
    public List<DeliveryAssignment> getAssignmentsByCourierId(UUID courierId) {
        return assignmentRepository.findByCourierId(courierId);
    }

    /**
     * Récupère les assignments actifs d'un livreur
     * @param courierId ID du livreur
     * @return La liste des assignments actifs
     */
    public List<DeliveryAssignment> getActiveAssignmentsByCourierId(UUID courierId) {
        return assignmentRepository.findActiveAssignmentsByCourierId(courierId);
    }

    /**
     * Annule tous les assignments actifs d'un livreur
     * @param courierId ID du livreur
     * @return La liste des assignments annulés
     */
    @Transactional
    public List<DeliveryAssignment> cancelAllActiveAssignmentsByCourierId(UUID courierId) {
        List<DeliveryAssignment> activeAssignments = assignmentRepository.findActiveAssignmentsByCourierId(courierId);

        for (DeliveryAssignment assignment : activeAssignments) {
            assignment.setStatus(DeliveryAssignment.AssignmentStatus.CANCELLED);
            assignment.setUpdatedAt(LocalDateTime.now());
            assignmentRepository.save(assignment);
        }

        // Libérer le livreur
        Courier courier = courierRepository.findById(courierId)
                .orElseThrow(() -> new RuntimeException("Livreur non trouvé avec l'ID: " + courierId));
        courier.setStatus(Courier.CourierStatus.AVAILABLE);
        courierRepository.save(courier);

        log.info("Cancelled {} active assignments for courier {}", activeAssignments.size(), courierId);
        return activeAssignments;
    }

    /**
     * Trouve le livreur le plus proche parmi une liste
     * @param couriers Liste des livreurs
     * @param latitude Latitude du point de référence
     * @param longitude Longitude du point de référence
     * @return Le livreur le plus proche
     */
    private Courier findNearestCourier(List<Courier> couriers, Double latitude, Double longitude) {
        if (couriers.isEmpty()) {
            return null;
        }

        Courier nearestCourier = null;
        Double minDistance = Double.MAX_VALUE;

        for (Courier courier : couriers) {
            if (courier.getCurrentLocation() != null) {
                Double distance = calculateDistance(
                        latitude, longitude,
                        courier.getCurrentLocation().getLatitude(),
                        courier.getCurrentLocation().getLongitude());

                if (distance < minDistance) {
                    minDistance = distance;
                    nearestCourier = courier;
                }
            }
        }

        return nearestCourier;
    }

    /**
     * Calcule la distance entre deux points géographiques (formule de Haversine)
     * @param lat1 Latitude du premier point
     * @param lon1 Longitude du premier point
     * @param lat2 Latitude du deuxième point
     * @param lon2 Longitude du deuxième point
     * @return Distance en kilomètres
     */
    private Double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        final int R = 6371; // Rayon de la Terre en kilomètres

        Double latDistance = Math.toRadians(lat2 - lat1);
        Double lonDistance = Math.toRadians(lon2 - lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        Double distance = R * c;

        return distance;
    }

    /**
     * Publie l'événement d'attribution de livraison
     * @param assignment L'assignment créé
     */
    private void publishDeliveryDispatchedEvent(DeliveryAssignment assignment) {
        // Dans une vraie implémentation, nous construirions un objet d'événement plus complet
        rabbitTemplate.convertAndSend(exchangeName, "delivery.dispatched", assignment);
        log.info("Published delivery.dispatched event for delivery ID: {}", assignment.getDeliveryId());
    }
}
