package com.smartdelivery.routeoptimizer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteRequest {
    private List<Point> waypoints;
    private String profile; // car, foot, bike
    private Boolean elevation; // include elevation data
    private Boolean pointsEncoded; // whether points are encoded
    private String locale; // language for instructions
    private Boolean instructions; // whether to return turn instructions
    private Boolean calcPoints; // whether to return points
    private Boolean debug; // debug mode
}
