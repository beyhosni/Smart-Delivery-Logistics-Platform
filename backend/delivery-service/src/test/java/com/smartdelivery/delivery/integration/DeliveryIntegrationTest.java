
package com.smartdelivery.delivery.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdelivery.delivery.model.Delivery;
import com.smartdelivery.delivery.model.Address;
import com.smartdelivery.delivery.model.PackageDetails;
import com.smartdelivery.delivery.repository.DeliveryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
class DeliveryIntegrationTest {

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
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private Delivery testDelivery;
    private Address pickupAddress;
    private Address deliveryAddress;
    private PackageDetails packageDetails;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

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

    @AfterEach
    void tearDown() {
        deliveryRepository.deleteAll();
    }

    @Test
    void createDelivery_ShouldSaveDeliveryToDatabase() throws Exception {
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

        // When & Then
        mockMvc.perform(post("/api/deliveries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newDelivery)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("CREATED"));

        // Vérifier que la livraison a été sauvegardée en base de données
        assertEquals(1, deliveryRepository.count());
    }

    @Test
    void getDeliveryById_WithValidId_ShouldReturnDelivery() throws Exception {
        // Given
        Delivery savedDelivery = deliveryRepository.save(testDelivery);

        // When & Then
        mockMvc.perform(get("/api/deliveries/{id}", savedDelivery.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedDelivery.getId().toString()))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void getAllDeliveries_ShouldReturnAllDeliveries() throws Exception {
        // Given
        deliveryRepository.save(testDelivery);

        // When & Then
        mockMvc.perform(get("/api/deliveries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    void updateDeliveryStatus_ShouldUpdateDeliveryInDatabase() throws Exception {
        // Given
        Delivery savedDelivery = deliveryRepository.save(testDelivery);
        Delivery.DeliveryStatus newStatus = Delivery.DeliveryStatus.DISPATCHED;

        // When & Then
        mockMvc.perform(put("/api/deliveries/{id}/status", savedDelivery.getId())
                .param("status", newStatus.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedDelivery.getId().toString()))
                .andExpect(jsonPath("$.status").value(newStatus.toString()))
                .andExpect(jsonPath("$.updatedAt").exists());

        // Vérifier que le statut a été mis à jour en base de données
        Delivery updatedDelivery = deliveryRepository.findById(savedDelivery.getId()).orElse(null);
        assertNotNull(updatedDelivery);
        assertEquals(newStatus, updatedDelivery.getStatus());
        assertNotNull(updatedDelivery.getUpdatedAt());
    }
}
