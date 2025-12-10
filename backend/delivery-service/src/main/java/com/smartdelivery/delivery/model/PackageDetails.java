package com.smartdelivery.delivery.model;

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
public class PackageDetails {

    @Column(name = "weight")
    private Double weight;

    @Embedded
    private Dimensions dimensions;

    @Column(name = "description")
    private String description;

    @Column(name = "is_fragile")
    private Boolean fragile;

    @Column(name = "special_instructions")
    private String specialInstructions;
}
