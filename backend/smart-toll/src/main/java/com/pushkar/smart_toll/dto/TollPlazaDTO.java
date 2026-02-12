package com.pushkar.smart_toll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TollPlazaDTO {
    private String name;
    private Double latitude;
    private Double longitude;
    private Double distanceFromSource;
}
