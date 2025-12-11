package com.smartdelivery.tracking.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "location_updates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdate {

    @Id
    private String id;

    private String deliveryId;
    private String courierId;
    private Double latitude;
    private Double longitude;
    private LocalDateTime timestamp;
    private String address;
    private Double speed; // en km/h
    private Double heading; // direction en degrés (0-360)
}
