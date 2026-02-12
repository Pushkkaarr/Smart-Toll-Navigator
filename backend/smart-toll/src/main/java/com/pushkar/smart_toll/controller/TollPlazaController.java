package com.pushkar.smart_toll.controller;

import com.pushkar.smart_toll.dto.TollPlazasRequestDTO;
import com.pushkar.smart_toll.dto.TollPlazasResponseDTO;
import com.pushkar.smart_toll.service.TollPlazaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/toll-plazas")
@RequiredArgsConstructor
@Slf4j
public class TollPlazaController {
    private final TollPlazaService tollPlazaService;

    /**
     * Find toll plazas between two pincodes
     */
    @PostMapping
    public ResponseEntity<TollPlazasResponseDTO> findTollPlazas(
            @Valid @RequestBody TollPlazasRequestDTO request) {
        log.info("Received request to find toll plazas from {} to {}",
                request.getSourcePincode(),
                request.getDestinationPincode());

        TollPlazasResponseDTO response = tollPlazaService.findTollPlazasBetweenPincodes(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        long tollPlazaCount = tollPlazaService.getTollPlazaCount();
        return ResponseEntity.ok(HealthResponse.builder()
                .status("UP")
                .tollPlazaCount(tollPlazaCount)
                .build());
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HealthResponse {
        private String status;
        private Long tollPlazaCount;
    }
}
