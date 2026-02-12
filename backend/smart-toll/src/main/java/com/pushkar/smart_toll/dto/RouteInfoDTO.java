package com.pushkar.smart_toll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteInfoDTO {
    private String sourcePincode;
    private String destinationPincode;
    private Double distanceInKm;
}
