
package com.smartdelivery.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdelivery.delivery.model.Delivery;
import com.smartdelivery.delivery.model.Address;
import com.smartdelivery.delivery.model.PackageDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DeliveryFlowTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management-alpine"))
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

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
                .senderId(UUID.randomUUID())
                .recipientId(UUID.randomUUID())
                .pickupAddress(pickupAddress)
                .deliveryAddress(deliveryAddress)
                .packageDetails(packageDetails)
                .priority(Delivery.DeliveryPriority.NORMAL)
                .requestedDeliveryTime(LocalDateTime.now().plusDays(1))
                .build();
    }

    @Test
    void testCompleteDeliveryFlow() {
        // Étape 1: Créer une nouvelle livraison
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Delivery> createDeliveryRequest = new HttpEntity<>(testDelivery, headers);

        ResponseEntity<Delivery> createResponse = restTemplate.postForEntity(
                "/api/deliveries", createDeliveryRequest, Delivery.class);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody().getId());
        assertEquals(Delivery.DeliveryStatus.CREATED, createResponse.getBody().getStatus());

        UUID deliveryId = createResponse.getBody().getId();

        // Étape 2: Récupérer la livraison créée
        ResponseEntity<Delivery> getResponse = restTemplate.getForEntity(
                "/api/deliveries/" + deliveryId, Delivery.class);

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals(deliveryId, getResponse.getBody().getId());
        assertEquals(Delivery.DeliveryStatus.CREATED, getResponse.getBody().getStatus());

        // Étape 3: Mettre à jour le statut à DISPATCHED
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> updateStatusRequest = new HttpEntity<>(headers);

        ResponseEntity<Delivery> updateResponse = restTemplate.exchange(
                "/api/deliveries/" + deliveryId + "/status?status=DISPATCHED",
                HttpMethod.PUT, updateStatusRequest, Delivery.class);

        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals(Delivery.DeliveryStatus.DISPATCHED, updateResponse.getBody().getStatus());
        assertNotNull(updateResponse.getBody().getUpdatedAt());

        // Étape 4: Mettre à jour le statut à IN_TRANSIT
        updateResponse = restTemplate.exchange(
                "/api/deliveries/" + deliveryId + "/status?status=IN_TRANSIT",
                HttpMethod.PUT, updateStatusRequest, Delivery.class);

        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals(Delivery.DeliveryStatus.IN_TRANSIT, updateResponse.getBody().getStatus());

        // Étape 5: Mettre à jour le statut à DELIVERED
        updateResponse = restTemplate.exchange(
                "/api/deliveries/" + deliveryId + "/status?status=DELIVERED",
                HttpMethod.PUT, updateStatusRequest, Delivery.class);

        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals(Delivery.DeliveryStatus.DELIVERED, updateResponse.getBody().getStatus());

        // Étape 6: Vérifier que tous les statuts intermédiaires ont été enregistrés
        // Dans un vrai scénario, nous vérifierions également que les événements ont été publiés
        // et que les autres services ont réagi correctement à ces événements
    }

    @Test
    void testDeliveryRetrievalByStatus() {
        // Créer plusieurs livraisons avec différents statuts
        Delivery delivery1 = createTestDelivery();
        Delivery delivery2 = createTestDelivery();
        Delivery delivery3 = createTestDelivery();

        // Créer les livraisons
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Delivery> createDeliveryRequest = new HttpEntity<>(delivery1, headers);

        ResponseEntity<Delivery> response1 = restTemplate.postForEntity(
                "/api/deliveries", createDeliveryRequest, Delivery.class);
        ResponseEntity<Delivery> response2 = restTemplate.postForEntity(
                "/api/deliveries", new HttpEntity<>(delivery2, headers), Delivery.class);
        ResponseEntity<Delivery> response3 = restTemplate.postForEntity(
                "/api/deliveries", new HttpEntity<>(delivery3, headers), Delivery.class);

        UUID deliveryId1 = response1.getBody().getId();
        UUID deliveryId2 = response2.getBody().getId();
        UUID deliveryId3 = response3.getBody().getId();

        // Mettre à jour les statuts
        HttpEntity<String> updateStatusRequest = new HttpEntity<>(headers);
        restTemplate.exchange(
                "/api/deliveries/" + deliveryId2 + "/status?status=DISPATCHED",
                HttpMethod.PUT, updateStatusRequest, Delivery.class);
        restTemplate.exchange(
                "/api/deliveries/" + deliveryId3 + "/status?status=DELIVERED",
                HttpMethod.PUT, updateStatusRequest, Delivery.class);

        // Récupérer les livraisons par statut
        ResponseEntity<Delivery[]> createdResponse = restTemplate.getForEntity(
                "/api/deliveries/status/CREATED", Delivery[].class);
        ResponseEntity<Delivery[]> dispatchedResponse = restTemplate.getForEntity(
                "/api/deliveries/status/DISPATCHED", Delivery[].class);
        ResponseEntity<Delivery[]> deliveredResponse = restTemplate.getForEntity(
                "/api/deliveries/status/DELIVERED", Delivery[].class);

        // Vérifier les résultats
        assertEquals(1, createdResponse.getBody().length);
        assertEquals(1, dispatchedResponse.getBody().length);
        assertEquals(1, deliveredResponse.getBody().length);
    }

    private Delivery createTestDelivery() {
        return Delivery.builder()
                .senderId(UUID.randomUUID())
                .recipientId(UUID.randomUUID())
                .pickupAddress(Address.builder()
                        .street("Test Pickup St")
                        .city("Test City")
                        .postalCode("12345")
                        .country("Test Country")
                        .coordinates(new com.smartdelivery.delivery.model.Coordinates(48.8566, 2.3522))
                        .build())
                .deliveryAddress(Address.builder()
                        .street("Test Delivery Ave")
                        .city("Test City")
                        .postalCode("54321")
                        .country("Test Country")
                        .coordinates(new com.smartdelivery.delivery.model.Coordinates(48.8584, 2.2945))
                        .build())
                .packageDetails(PackageDetails.builder()
                        .weight(2.5)
                        .dimensions(new com.smartdelivery.delivery.model.Dimensions(30, 20, 10))
                        .build())
                .priority(Delivery.DeliveryPriority.NORMAL)
                .requestedDeliveryTime(LocalDateTime.now().plusDays(1))
                .build();
    }
}
