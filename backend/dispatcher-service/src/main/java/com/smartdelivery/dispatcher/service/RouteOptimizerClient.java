package com.smartdelivery.dispatcher.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Client pour interagir avec le service d'optimisation de routes.
 * Fournit des méthodes pour créer, récupérer, mettre à jour et supprimer des routes optimisées.
 */
@Service
public class RouteOptimizerClient {

    private static final Logger logger = LoggerFactory.getLogger(RouteOptimizerClient.class);
    private static final int MAX_LATITUDE = 90;
    private static final int MIN_LATITUDE = -90;
    private static final int MAX_LONGITUDE = 180;
    private static final int MIN_LONGITUDE = -180;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int RETRY_DELAY_MS = 1000;

    private final RestTemplate restTemplate;
    private final String routeOptimizerServiceUrl;

    public RouteOptimizerClient(
            RestTemplate restTemplate,
            @Value("${route.optimizer.service.url:http://route-optimizer-service:8085}") String routeOptimizerServiceUrl) {
        this.restTemplate = restTemplate;
        this.routeOptimizerServiceUrl = routeOptimizerServiceUrl;
    }

    /**
     * Crée une route optimisée pour une livraison
     * @param deliveryId ID de la livraison
     * @param courierId ID du livreur
     * @param pickupLatitude Latitude du point de ramassage
     * @param pickupLongitude Longitude du point de ramassage
     * @param deliveryLatitude Latitude du point de livraison
     * @param deliveryLongitude Longitude du point de livraison
     * @return ID de la route créée
     * @throws RouteOptimizerException en cas d'erreur lors de la création de la route
     * @throws ValidationException si les paramètres d'entrée sont invalides
     */
    public UUID createOptimizedRoute(
            UUID deliveryId,
            UUID courierId,
            Double pickupLatitude,
            Double pickupLongitude,
            Double deliveryLatitude,
            Double deliveryLongitude) {

        // Validation des entrées
        validateRouteParameters(deliveryId, courierId, pickupLatitude, pickupLongitude, deliveryLatitude, deliveryLongitude);

        logger.info("Création d'une route optimisée pour la livraison {} avec le livreur {}", deliveryId, courierId);
        
        String url = routeOptimizerServiceUrl + "/api/routes";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("deliveryId", deliveryId.toString());
        requestBody.put("courierId", courierId.toString());
        requestBody.put("createdAt", LocalDateTime.now().toString());

        Map<String, Object> pickupLocation = new HashMap<>();
        pickupLocation.put("latitude", pickupLatitude);
        pickupLocation.put("longitude", pickupLongitude);
        requestBody.put("pickupLocation", pickupLocation);

        Map<String, Object> deliveryLocation = new HashMap<>();
        deliveryLocation.put("latitude", deliveryLatitude);
        deliveryLocation.put("longitude", deliveryLongitude);
        requestBody.put("deliveryLocation", deliveryLocation);

        // Configuration des en-têtes
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            // Tentative avec mécanisme de retry
            ResponseEntity<Map> response = executeWithRetry(() -> 
                restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class),
                "createOptimizedRoute",
                MAX_RETRY_ATTEMPTS);
            
            if (response != null && response.getBody() != null && response.getBody().containsKey("routeId")) {
                UUID routeId = UUID.fromString((String) response.getBody().get("routeId"));
                logger.info("Route optimisée créée avec succès: {}", routeId);
                return routeId;
            }
            
            throw new RouteOptimizerException("Réponse invalide du service d'optimisation de routes");
        } catch (RestClientException e) {
            logger.error("Erreur lors de la création de la route optimisée: {}", e.getMessage());
            throw new RouteOptimizerException("Impossible de créer la route optimisée", e);
        }
    }

    /**
     * Récupère les détails d'une route optimisée
     * @param routeId ID de la route
     * @return Détails de la route
     * @throws RouteOptimizerException en cas d'erreur lors de la récupération
     * @throws ValidationException si l'ID de la route est invalide
     */
    public Map<String, Object> getRouteDetails(UUID routeId) {
        if (routeId == null) {
            throw new ValidationException("L'ID de la route ne peut pas être nul");
        }

        logger.info("Récupération des détails de la route: {}", routeId);
        
        String url = routeOptimizerServiceUrl + "/api/routes/" + routeId.toString();
        
        try {
            ResponseEntity<Map> response = executeWithRetry(() -> 
                restTemplate.getForEntity(url, Map.class),
                "getRouteDetails",
                MAX_RETRY_ATTEMPTS);
                
            if (response != null && response.getBody() != null) {
                logger.info("Détails de la route {} récupérés avec succès", routeId);
                return response.getBody();
            }
            
            throw new RouteOptimizerException("Réponse invalide du service d'optimisation de routes");
        } catch (RestClientException e) {
            logger.error("Erreur lors de la récupération des détails de la route {}: {}", routeId, e.getMessage());
            throw new RouteOptimizerException("Impossible de récupérer les détails de la route", e);
        }
    }

    /**
     * Met à jour une route optimisée existante
     * @param routeId ID de la route à mettre à jour
     * @param status Nouveau statut de la route
     * @return true si la mise à jour a réussi, false sinon
     * @throws RouteOptimizerException en cas d'erreur lors de la mise à jour
     * @throws ValidationException si l'ID de la route est invalide
     */
    public boolean updateRouteStatus(UUID routeId, String status) {
        if (routeId == null) {
            throw new ValidationException("L'ID de la route ne peut pas être nul");
        }
        if (status == null || status.trim().isEmpty()) {
            throw new ValidationException("Le statut ne peut pas être nul ou vide");
        }

        logger.info("Mise à jour du statut de la route {} à: {}", routeId, status);
        
        String url = routeOptimizerServiceUrl + "/api/routes/" + routeId.toString() + "/status";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("status", status);
        requestBody.put("updatedAt", LocalDateTime.now().toString());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<Void> response = executeWithRetry(() -> 
                restTemplate.exchange(url, HttpMethod.PUT, requestEntity, Void.class),
                "updateRouteStatus",
                MAX_RETRY_ATTEMPTS);
                
            if (response != null && response.getStatusCode() == HttpStatus.OK) {
                logger.info("Statut de la route {} mis à jour avec succès", routeId);
                return true;
            }
            
            return false;
        } catch (RestClientException e) {
            logger.error("Erreur lors de la mise à jour du statut de la route {}: {}", routeId, e.getMessage());
            throw new RouteOptimizerException("Impossible de mettre à jour le statut de la route", e);
        }
    }

    /**
     * Supprime une route optimisée
     * @param routeId ID de la route à supprimer
     * @return true si la suppression a réussi, false sinon
     * @throws RouteOptimizerException en cas d'erreur lors de la suppression
     * @throws ValidationException si l'ID de la route est invalide
     */
    public boolean deleteRoute(UUID routeId) {
        if (routeId == null) {
            throw new ValidationException("L'ID de la route ne peut pas être nul");
        }

        logger.info("Suppression de la route: {}", routeId);
        
        String url = routeOptimizerServiceUrl + "/api/routes/" + routeId.toString();
        
        try {
            ResponseEntity<Void> response = executeWithRetry(() -> 
                restTemplate.exchange(url, HttpMethod.DELETE, null, Void.class),
                "deleteRoute",
                MAX_RETRY_ATTEMPTS);
                
            if (response != null && response.getStatusCode() == HttpStatus.NO_CONTENT) {
                logger.info("Route {} supprimée avec succès", routeId);
                return true;
            }
            
            return false;
        } catch (RestClientException e) {
            logger.error("Erreur lors de la suppression de la route {}: {}", routeId, e.getMessage());
            throw new RouteOptimizerException("Impossible de supprimer la route", e);
        }
    }

    /**
     * Récupère toutes les routes pour un livreur spécifique
     * @param courierId ID du livreur
     * @param status Filtre optionnel sur le statut des routes
     * @return Liste des routes
     * @throws RouteOptimizerException en cas d'erreur lors de la récupération
     * @throws ValidationException si l'ID du livreur est invalide
     */
    public List<Map<String, Object>> getRoutesByCourier(UUID courierId, String status) {
        if (courierId == null) {
            throw new ValidationException("L'ID du livreur ne peut pas être nul");
        }

        logger.info("Récupération des routes pour le livreur {} avec le statut: {}", courierId, status);
        
        UriComponentsBuilder builder = UriComponentsBuilder
            .fromHttpUrl(routeOptimizerServiceUrl + "/api/routes")
            .queryParam("courierId", courierId.toString());
            
        if (status != null && !status.trim().isEmpty()) {
            builder.queryParam("status", status);
        }
        
        String url = builder.toUriString();
        
        try {
            ResponseEntity<Map> response = executeWithRetry(() -> 
                restTemplate.getForEntity(url, Map.class),
                "getRoutesByCourier",
                MAX_RETRY_ATTEMPTS);
                
            if (response != null && response.getBody() != null && response.getBody().containsKey("routes")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> routes = (List<Map<String, Object>>) response.getBody().get("routes");
                logger.info("{} routes récupérées pour le livreur {}", routes.size(), courierId);
                return routes;
            }
            
            return Collections.emptyList();
        } catch (RestClientException e) {
            logger.error("Erreur lors de la récupération des routes pour le livreur {}: {}", courierId, e.getMessage());
            throw new RouteOptimizerException("Impossible de récupérer les routes pour le livreur", e);
        }
    }

    /**
     * Exécute une requête avec mécanisme de retry en cas d'échec
     * @param request Supplier de la requête à exécuter
     * @param operationName Nom de l'opération pour les logs
     * @param maxAttempts Nombre maximal de tentatives
     * @return Réponse de la requête
     * @throws RestClientException si toutes les tentatives échouent
     */
    private <T> ResponseEntity<T> executeWithRetry(
            Supplier<ResponseEntity<T>> request, 
            String operationName, 
            int maxAttempts) throws RestClientException {
        
        int attempt = 0;
        while (attempt < maxAttempts) {
            try {
                return request.get();
            } catch (RestClientException e) {
                attempt++;
                logger.warn("Tentative {} échouée pour l'opération {}: {}", attempt, operationName, e.getMessage());
                
                if (attempt >= maxAttempts) {
                    throw e;
                }
                
                try {
                    Thread.sleep(RETRY_DELAY_MS * attempt); // Délai progressif
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RestClientException("Opération interrompue pendant le retry", ie);
                }
            }
        }
        
        throw new RestClientException("Échec de toutes les tentatives pour l'opération " + operationName);
    }

    /**
     * Valide les paramètres de création de route
     * @param deliveryId ID de la livraison
     * @param courierId ID du livreur
     * @param pickupLatitude Latitude du point de ramassage
     * @param pickupLongitude Longitude du point de ramassage
     * @param deliveryLatitude Latitude du point de livraison
     * @param deliveryLongitude Longitude du point de livraison
     * @throws ValidationException si un paramètre est invalide
     */
    private void validateRouteParameters(
            UUID deliveryId,
            UUID courierId,
            Double pickupLatitude,
            Double pickupLongitude,
            Double deliveryLatitude,
            Double deliveryLongitude) {
        
        if (deliveryId == null) {
            throw new ValidationException("L'ID de la livraison ne peut pas être nul");
        }
        
        if (courierId == null) {
            throw new ValidationException("L'ID du livreur ne peut pas être nul");
        }
        
        if (pickupLatitude == null || pickupLongitude == null || 
            deliveryLatitude == null || deliveryLongitude == null) {
            throw new ValidationException("Les coordonnées GPS ne peuvent pas être nulles");
        }
        
        if (pickupLatitude < MIN_LATITUDE || pickupLatitude > MAX_LATITUDE ||
            deliveryLatitude < MIN_LATITUDE || deliveryLatitude > MAX_LATITUDE) {
            throw new ValidationException(
                String.format("La latitude doit être comprise entre %d et %d", MIN_LATITUDE, MAX_LATITUDE));
        }
        
        if (pickupLongitude < MIN_LONGITUDE || pickupLongitude > MAX_LONGITUDE ||
            deliveryLongitude < MIN_LONGITUDE || deliveryLongitude > MAX_LONGITUDE) {
            throw new ValidationException(
                String.format("La longitude doit être comprise entre %d et %d", MIN_LONGITUDE, MAX_LONGITUDE));
        }
    }

    /**
     * Exception personnalisée pour les erreurs de validation
     */
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

    /**
     * Exception personnalisée pour les erreurs du service d'optimisation de routes
     */
    public static class RouteOptimizerException extends RuntimeException {
        public RouteOptimizerException(String message) {
            super(message);
        }
        
        public RouteOptimizerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
