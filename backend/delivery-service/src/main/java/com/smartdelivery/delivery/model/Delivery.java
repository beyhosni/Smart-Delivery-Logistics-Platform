package com.smartdelivery.delivery.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deliveries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID senderId;

    @Column(nullable = false)
    private UUID recipientId;

    @Embedded
    private Address pickupAddress;

    @Embedded
    private Address deliveryAddress;

    @Embedded
    private PackageDetails packageDetails;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryPriority priority;

    @Column
    private LocalDateTime requestedDeliveryTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum DeliveryPriority {
        LOW, NORMAL, HIGH, URGENT
    }

    public enum DeliveryStatus {
        CREATED, DISPATCHED, PICKED_UP, IN_TRANSIT, DELIVERED, CANCELLED
    }
}
