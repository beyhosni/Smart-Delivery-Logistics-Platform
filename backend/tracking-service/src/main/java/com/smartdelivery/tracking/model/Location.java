package com.smartdelivery.tracking.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {

    private Double latitude;

    private Double longitude;

    private String address;

    private LocalDateTime timestamp;

    private Double speed; // en km/h

    private Double heading; // direction en degrés (0-360)

    private Double accuracy; // précision en mètres
}
