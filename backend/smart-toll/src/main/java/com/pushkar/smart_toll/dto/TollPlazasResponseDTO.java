package com.pushkar.smart_toll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TollPlazasResponseDTO {
    private RouteInfoDTO route;
    private List<TollPlazaDTO> tollPlazas;
}
