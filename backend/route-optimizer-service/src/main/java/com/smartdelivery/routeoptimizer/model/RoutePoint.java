package com.smartdelivery.routeoptimizer.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutePoint {

    private Double latitude;

    private Double longitude;

    private String address;

    private Long estimatedArrival; // en secondes depuis le début de la route

    private Boolean isWaypoint; // false pour le point de départ et de destination

    private String instruction; // instruction de navigation (ex: "Tournez à droite")
}
