package com.smartdelivery.tracking.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "delivery_tracking")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryTracking {

    @Id
    private String id;

    private UUID deliveryId;

    private UUID courierId;

    private Location currentLocation;

    private List<Location> locationHistory;

    private LocalDateTime lastUpdated;

    private LocalDateTime estimatedDeliveryTime;

    private TrackingStatus status;

    private String routeId;

    private Double progressPercentage;

    public enum TrackingStatus {
        DISPATCHED, PICKED_UP, IN_TRANSIT, DELIVERED
    }

    public void addLocationToHistory(Location location) {
        if (locationHistory == null) {
            locationHistory = new ArrayList<>();
        }
        locationHistory.add(location);
        lastUpdated = LocalDateTime.now();
    }
}
