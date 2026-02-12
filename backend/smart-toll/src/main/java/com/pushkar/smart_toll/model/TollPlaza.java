package com.pushkar.smart_toll.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TollPlaza {
    private Long id;
    private String name;
    private Double latitude;
    private Double longitude;
    private String state;
    private Double distanceFromSource; // In kilometers
}
