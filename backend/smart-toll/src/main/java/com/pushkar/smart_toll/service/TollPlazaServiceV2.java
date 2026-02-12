package com.pushkar.smart_toll.service;

import com.pushkar.smart_toll.dto.RouteInfoDTO;
import com.pushkar.smart_toll.dto.TollPlazaDTO;
import com.pushkar.smart_toll.dto.TollPlazasRequestDTO;
import com.pushkar.smart_toll.dto.TollPlazasResponseDTO;
import com.pushkar.smart_toll.exception.InvalidPincodeException;
import com.pushkar.smart_toll.exception.SamePincodeException;
import com.pushkar.smart_toll.model.TollPlaza;
import com.pushkar.smart_toll.repository.TollPlazaRepository;
import com.pushkar.smart_toll.util.GeoLocationUtilV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Production-grade toll plaza service
 * Uses OpenStreetMap APIs (Nominatim + OSRM) for free routing and real polyline-based toll matching
 * No hardcoded data - all dynamic from CSV and open-source APIs
 * No API keys required - 100% free and open source
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TollPlazaServiceV2 {
    private final TollPlazaRepository tollPlazaRepository;
    private final OpenStreetMapIntegrationService osmService;

    /**
     * Find toll plazas between two pincodes
     * 
     * @param request Source and destination pincodes
     * @return Response with route info and tolls on that route
     * @throws InvalidPincodeException if pincodes are invalid
     * @throws SamePincodeException if source equals destination
     */
    @Cacheable(value = "tollPlazasCache", key = "#request.sourcePincode + '-' + #request.destinationPincode")
    public TollPlazasResponseDTO findTollPlazasBetweenPincodes(TollPlazasRequestDTO request) {
        log.info("Finding toll plazas between {} and {}", 
                request.getSourcePincode(), 
                request.getDestinationPincode());

        // Validation
        validatePincodes(request.getSourcePincode(), request.getDestinationPincode());

        try {
            // Step 1: Geocode source and destination pincodes
            log.debug("Step 1: Geocoding source pincode {}", request.getSourcePincode());
            OpenStreetMapIntegrationService.GeocodeLocation sourceLocation = 
                osmService.getLocationFromPincode(request.getSourcePincode());

            log.debug("Step 2: Geocoding destination pincode {}", request.getDestinationPincode());
            OpenStreetMapIntegrationService.GeocodeLocation destLocation = 
                osmService.getLocationFromPincode(request.getDestinationPincode());

            log.info("Source: {} ({}, {}), Destination: {} ({}, {})",
                    sourceLocation.getAddress(), sourceLocation.getLatitude(), sourceLocation.getLongitude(),
                    destLocation.getAddress(), destLocation.getLatitude(), destLocation.getLongitude());

            // Step 2: Get actual route from OpenStreetMap OSRM API
            log.debug("Step 3: Fetching route from OSRM Routing API");
            OpenStreetMapIntegrationService.RouteInfo routeInfo = osmService.getRouteInfo(
                    sourceLocation.getLatitude(),
                    sourceLocation.getLongitude(),
                    destLocation.getLatitude(),
                    destLocation.getLongitude()
            );

            log.info("Route found: {} km with {} waypoints", 
                    routeInfo.getDistanceKm(), 
                    routeInfo.getPolylineCoordinates().size());

            // Step 3: Find tolls along the actual route
            log.debug("Step 4: Matching tolls from CSV to actual route");
            List<TollPlazaDTO> tollPlazasOnRoute = findTollPlazasAlongRoute(routeInfo);

            // Sort by distance from source
            tollPlazasOnRoute.sort(Comparator.comparingDouble(TollPlazaDTO::getDistanceFromSource));

            // Create response
            RouteInfoDTO routeInfoDTO = RouteInfoDTO.builder()
                    .sourcePincode(request.getSourcePincode())
                    .destinationPincode(request.getDestinationPincode())
                    .distanceInKm(Math.round(routeInfo.getDistanceKm() * 100.0) / 100.0)
                    .build();

            TollPlazasResponseDTO response = TollPlazasResponseDTO.builder()
                    .route(routeInfoDTO)
                    .tollPlazas(tollPlazasOnRoute)
                    .build();

            log.info("Found {} toll plazas on the route", tollPlazasOnRoute.size());
            return response;

        } catch (Exception e) {
            log.error("Error finding toll plazas: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Find toll plazas that lie on the actual route polyline
     */
    private List<TollPlazaDTO> findTollPlazasAlongRoute(OpenStreetMapIntegrationService.RouteInfo routeInfo) {
        log.debug("Getting all toll plazas from database");
        List<TollPlaza> allTolls = tollPlazaRepository.findAll();
        
        log.debug("Total tolls available in CSV: {}", allTolls.size());

        // Filter tolls that are near the actual route
        List<TollPlazaDTO> tollPlazasOnRoute = allTolls.stream()
                .filter(plaza -> {
                    // Check if toll is within route tolerance using actual polyline
                    boolean isNearRoute = GeoLocationUtilV2.isTollNearRoute(
                            routeInfo.getPolylineCoordinates(),
                            plaza.getLatitude(),
                            plaza.getLongitude()
                    );
                    
                    if (isNearRoute) {
                        log.trace("Toll {} is on route", plaza.getName());
                    }
                    return isNearRoute;
                })
                .map(plaza -> {
                    // Calculate distance from route start
                    double distanceFromSource = GeoLocationUtilV2.distanceFromRouteStart(
                            routeInfo.getPolylineCoordinates(),
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
                // Deduplicate based on exact match of name, lat, lng
                .distinct()
                .collect(Collectors.toList());

        return tollPlazasOnRoute;
    }

    /**
     * Validate source and destination pincodes
     */
    private void validatePincodes(String sourcePincode, String destinationPincode) {
        if (!isValidPincode(sourcePincode) || !isValidPincode(destinationPincode)) {
            throw new InvalidPincodeException("Invalid source or destination pincode");
        }

        if (sourcePincode.equals(destinationPincode)) {
            throw new SamePincodeException("Source and destination pincodes cannot be the same");
        }
    }

    private boolean isValidPincode(String pincode) {
        return pincode != null && pincode.matches("^[0-9]{6}$");
    }

    /**
     * Get all toll plazas (for admin purposes)
     */
    public List<TollPlaza> getAllTollPlazas() {
        return tollPlazaRepository.findAll();
    }

    /**
     * Get total count of tolls in CSV
     */
    public long getTollPlazaCount() {
        return tollPlazaRepository.count();
    }
}
