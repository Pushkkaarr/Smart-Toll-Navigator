package com.pushkar.smart_toll.controller;

import com.pushkar.smart_toll.dto.TollPlazasRequestDTO;
import com.pushkar.smart_toll.dto.TollPlazasResponseDTO;
import com.pushkar.smart_toll.service.TollPlazaServiceV2;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoint for toll plaza queries
 * Production-grade service with real Mappls API integration
 */
@RestController
@RequestMapping("/api/v1/toll-plazas")
@RequiredArgsConstructor
@Slf4j
public class TollPlazaController {
    private final TollPlazaServiceV2 tollPlazaService;

    /**
     * Find toll plazas between two Indian pincodes
     * 
     * @param request Source and destination pincodes (must be 6-digit)
     * @return List of toll plazas on the route with distances from source
     */
    @PostMapping
    public ResponseEntity<TollPlazasResponseDTO> findTollPlazas(
            @Valid @RequestBody TollPlazasRequestDTO request) {
        log.info("Received request to find toll plazas from {} to {}",
                request.getSourcePincode(),
                request.getDestinationPincode());

        try {
            TollPlazasResponseDTO response = tollPlazaService.findTollPlazasBetweenPincodes(request);
            
            // Log result
            if (response.getTollPlazas().isEmpty()) {
                log.info("No toll plazas found on the route");
            } else {
                log.info("Found {} toll plazas on the route", response.getTollPlazas().size());
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing toll plaza request: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Health check endpoint
     * Returns API status and total toll plazas available
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        long tollPlazaCount = tollPlazaService.getTollPlazaCount();
        return ResponseEntity.ok(HealthResponse.builder()
                .status("UP")
                .tollPlazaCount(tollPlazaCount)
                .message("Service is operational with " + tollPlazaCount + " toll plazas in database")
                .build());
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HealthResponse {
        private String status;
        private Long tollPlazaCount;
        private String message;
    }
}
