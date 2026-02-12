package com.pushkar.smart_toll.service;

import com.pushkar.smart_toll.dto.RouteInfoDTO;
import com.pushkar.smart_toll.dto.TollPlazaDTO;
import com.pushkar.smart_toll.dto.TollPlazasRequestDTO;
import com.pushkar.smart_toll.dto.TollPlazasResponseDTO;
import com.pushkar.smart_toll.exception.InvalidPincodeException;
import com.pushkar.smart_toll.exception.SamePincodeException;
import com.pushkar.smart_toll.model.TollPlaza;
import com.pushkar.smart_toll.repository.TollPlazaRepository;
import com.pushkar.smart_toll.util.GeoLocationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TollPlazaService {
    private final TollPlazaRepository tollPlazaRepository;
    private final MappslIntegrationService mappslIntegrationService;

    /**
     * Find toll plazas between two pincodes
     */
    @Cacheable(value = "tollPlazasCache", key = "#request.sourcePincode + '-' + #request.destinationPincode")
    public TollPlazasResponseDTO findTollPlazasBetweenPincodes(TollPlazasRequestDTO request) {
        log.info("Finding toll plazas between {} and {}", 
                request.getSourcePincode(), 
                request.getDestinationPincode());

        // Validation
        validatePincodes(request.getSourcePincode(), request.getDestinationPincode());

        // Get route information
        MappslIntegrationService.RouteInfo routeInfo = mappslIntegrationService.getRouteInfo(
                request.getSourcePincode(),
                request.getDestinationPincode()
        );

        // Find toll plazas on the route
        List<TollPlazaDTO> tollPlazasOnRoute = findTollPlazasAlongRoute(routeInfo);

        // Sort by distance from source
        tollPlazasOnRoute.sort(Comparator.comparingDouble(TollPlazaDTO::getDistanceFromSource));

        // Create response
        RouteInfoDTO routeInfoDTO = RouteInfoDTO.builder()
                .sourcePincode(request.getSourcePincode())
                .destinationPincode(request.getDestinationPincode())
                .distanceInKm(Math.round(routeInfo.distanceKm * 100.0) / 100.0)
                .build();

        TollPlazasResponseDTO response = TollPlazasResponseDTO.builder()
                .route(routeInfoDTO)
                .tollPlazas(tollPlazasOnRoute)
                .build();

        log.info("Found {} toll plazas on the route", tollPlazasOnRoute.size());
        return response;
    }

    /**
     * Find toll plazas that are on the route between two coordinates
     */
    private List<TollPlazaDTO> findTollPlazasAlongRoute(MappslIntegrationService.RouteInfo routeInfo) {
        // Get bounding box for the route with buffer
        GeoLocationUtil.BoundingBox boundingBox = GeoLocationUtil.getBoundingBox(
                routeInfo.sourceLat,
                routeInfo.sourceLng,
                routeInfo.destLat,
                routeInfo.destLng
        );

        // Get toll plazas within bounding box
        List<TollPlaza> candidatePlazas = tollPlazaRepository.findByLatitudeBetweenAndLongitudeBetween(
                boundingBox.minLat,
                boundingBox.maxLat,
                boundingBox.minLng,
                boundingBox.maxLng
        );

        // Filter toll plazas that are near the route
        List<TollPlazaDTO> tollPlazasOnRoute = candidatePlazas.stream()
                .filter(plaza -> GeoLocationUtil.isPointNearRoute(
                        routeInfo.sourceLat,
                        routeInfo.sourceLng,
                        routeInfo.destLat,
                        routeInfo.destLng,
                        plaza.getLatitude(),
                        plaza.getLongitude()
                ))
                .map(plaza -> {
                    // Calculate distance from source
                    double distanceFromSource = GeoLocationUtil.calculateDistance(
                            routeInfo.sourceLat,
                            routeInfo.sourceLng,
                            plaza.getLatitude(),
                            plaza.getLongitude()
                    );

                    return TollPlazaDTO.builder()
                            .name(plaza.getName())
                            .latitude(plaza.getLatitude())
                            .longitude(plaza.getLongitude())
                            .distanceFromSource(Math.round(distanceFromSource * 100.0) / 100.0)
                            .build();
                })
                .collect(Collectors.toList());

        return tollPlazasOnRoute;
    }

    /**
     * Validate pincodes
     */
    private void validatePincodes(String sourcePincode, String destinationPincode) {
        // Check if pincodes are valid format
        if (!isValidPincode(sourcePincode) || !isValidPincode(destinationPincode)) {
            throw new InvalidPincodeException("Invalid source or destination pincode");
        }

        // Check if source and destination are not the same
        if (sourcePincode.equals(destinationPincode)) {
            throw new SamePincodeException("Source and destination pincodes cannot be the same");
        }
    }

    private boolean isValidPincode(String pincode) {
        return pincode != null && pincode.matches("^[0-9]{6}$");
    }

    /**
     * Get all toll plazas
     */
    public List<TollPlaza> getAllTollPlazas() {
        return tollPlazaRepository.findAll();
    }

    /**
     * Get toll plaza count
     */
    public long getTollPlazaCount() {
        return tollPlazaRepository.count();
    }
}
