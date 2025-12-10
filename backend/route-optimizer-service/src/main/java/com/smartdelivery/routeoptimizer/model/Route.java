package com.smartdelivery.routeoptimizer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "routes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID deliveryId;

    @Column(nullable = false)
    private UUID courierId;

    @ElementCollection
    @CollectionTable(name = "route_points", joinColumns = @JoinColumn(name = "route_id"))
    @OrderColumn(name = "point_order")
    private List<RoutePoint> points;

    @Column(nullable = false)
    private Double totalDistance; // en mètres

    @Column(nullable = false)
    private Long totalDuration; // en secondes

    @Column(nullable = false)
    private LocalDateTime estimatedArrivalTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RouteStatus status;

    public enum RouteStatus {
        PLANNED, ACTIVE, COMPLETED, CANCELLED
    }
}
