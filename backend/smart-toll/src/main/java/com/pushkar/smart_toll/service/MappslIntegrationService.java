package com.pushkar.smart_toll.service;

import com.pushkar.smart_toll.dto.external.MappslGeocodingResponse;
import com.pushkar.smart_toll.dto.external.MappslRouteResponse;
import com.pushkar.smart_toll.exception.InvalidPincodeException;
import com.pushkar.smart_toll.exception.RouteNotAvailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class MappslIntegrationService {

    @Value("${mappls.api.key:}")
    private String mapplsApiKey;

    @Value("${mappls.api.base.url:https://apis.mappls.com}")
    private String mapplsBaseUrl;

    private final RestTemplate restTemplate;

    /**
     * Get geocoding information for a pincode
     * This would call Mappls geocoding API in production
     */
    @Cacheable(value = "pincodeCache", key = "#pincode")
    public MappslGeocodingResponse.Geometry getLocationFromPincode(String pincode) {
        try {
            if (!isValidPincode(pincode)) {
                throw new InvalidPincodeException("Invalid pincode format: " + pincode);
            }

            // Mock implementation - In production, this would call Mappls Geocoding API
            // URL: POST {mapplsBaseUrl}/apis/v1/geocode?address={pincode}&key={apiKey}
            
            log.debug("Fetching location for pincode: {}", pincode);
            
            // For now, return mock data. In production with Mappls API:
            // MappslGeocodingResponse response = restTemplate.getForObject(
            //     mapplsBaseUrl + "/apis/v1/geocode?address=" + pincode + "&key=" + mapplsApiKey,
            //     MappslGeocodingResponse.class);
            
            // Mock implementation
            return getMockLocationForPincode(pincode);
        } catch (RestClientException e) {
            log.error("Error fetching location from Mappls for pincode: {}", pincode, e);
            throw new RouteNotAvailableException("Unable to fetch location details for pincode: " + pincode, e);
        }
    }

    /**
     * Get route information between two pincodes
     */
    @Cacheable(value = "routeCache", key = "#sourcePincode + '-' + #destinationPincode")
    public RouteInfo getRouteInfo(String sourcePincode, String destinationPincode) {
        try {
            if (!isValidPincode(sourcePincode) || !isValidPincode(destinationPincode)) {
                throw new InvalidPincodeException("Invalid source or destination pincode");
            }

            if (sourcePincode.equals(destinationPincode)) {
                throw new IllegalArgumentException("Source and destination pincodes cannot be the same");
            }

            log.debug("Fetching route info for {} to {}", sourcePincode, destinationPincode);

            // Get coordinates for both pincodes
            MappslGeocodingResponse.Geometry sourceGeometry = getLocationFromPincode(sourcePincode);
            MappslGeocodingResponse.Geometry destGeometry = getLocationFromPincode(destinationPincode);

            // Mock route distance calculation
            // In production, this would call Mappls Direction API:
            // POST {mapplsBaseUrl}/apis/v1/routes?origin={sourceCoord}&destination={destCoord}&key={apiKey}
            
            double distance = calculateMockRouteDistance(
                    sourceGeometry.getLocation().getLatitude(),
                    sourceGeometry.getLocation().getLongitude(),
                    destGeometry.getLocation().getLatitude(),
                    destGeometry.getLocation().getLongitude()
            );

            return new RouteInfo(
                    sourceGeometry.getLocation().getLatitude(),
                    sourceGeometry.getLocation().getLongitude(),
                    destGeometry.getLocation().getLatitude(),
                    destGeometry.getLocation().getLongitude(),
                    distance
            );
        } catch (RestClientException e) {
            log.error("Error fetching route from Mappls", e);
            throw new RouteNotAvailableException("Unable to fetch route information", e);
        }
    }

    private boolean isValidPincode(String pincode) {
        return pincode != null && pincode.matches("^[0-9]{6}$");
    }

    private MappslGeocodingResponse.Geometry getMockLocationForPincode(String pincode) {
        // Map of some Indian pincodes to their coordinates
        // In production, this would come from Mappls API
        java.util.Map<String, double[]> pincodeCoordinates = new java.util.HashMap<>();
        pincodeCoordinates.put("110001", new double[]{28.6329, 77.2197});  // Delhi
        pincodeCoordinates.put("560001", new double[]{12.9716, 77.5946});  // Bangalore
        pincodeCoordinates.put("400001", new double[]{18.9520, 72.8347});  // Mumbai
        pincodeCoordinates.put("560064", new double[]{13.0826, 77.6093});  // Bangalore (alt)
        pincodeCoordinates.put("411045", new double[]{18.5204, 73.8567});  // Pune
        pincodeCoordinates.put("700001", new double[]{22.5726, 88.3639});  // Kolkata
        pincodeCoordinates.put("600001", new double[]{13.0456, 80.2994});  // Chennai
        pincodeCoordinates.put("380001", new double[]{23.0225, 72.5714});  // Ahmedabad
        pincodeCoordinates.put("360001", new double[]{21.1458, 72.7454});  // Rajkot
        pincodeCoordinates.put("395001", new double[]{21.1929, 72.8479});  // Surat

        double[] coords = pincodeCoordinates.getOrDefault(pincode, generateRandomCoordinates(pincode));

        MappslGeocodingResponse.Geometry geometry = new MappslGeocodingResponse.Geometry();
        MappslGeocodingResponse.Location location = new MappslGeocodingResponse.Location();
        location.setLatitude(coords[0]);
        location.setLongitude(coords[1]);
        geometry.setLocation(location);

        return geometry;
    }

    private double[] generateRandomCoordinates(String pincode) {
        // Generate pseudo-random coordinates based on pincode for testing flexibility
        int hash = pincode.hashCode();
        double lat = 8 + ((Math.abs(hash) % 20000) / 1000.0);
        double lng = 68 + ((Math.abs(hash >> 16) % 3200) / 100.0);
        return new double[]{lat, lng};
    }

    private double calculateMockRouteDistance(double sourceLat, double sourceLng,
                                              double destLat, double destLng) {
        // Using Haversine formula for approximation
        // In production, this would be the actual road distance from Mappls API
        double lat1Rad = Math.toRadians(sourceLat);
        double lat2Rad = Math.toRadians(destLat);
        double lngDiff = Math.toRadians(destLng - sourceLng);

        double a = Math.sin((lat2Rad - lat1Rad) / 2) * Math.sin((lat2Rad - lat1Rad) / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.sin(lngDiff / 2) * Math.sin(lngDiff / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = 6371.0 * c; // Earth's radius in km

        // Add 15% for actual road distance
        return distance * 1.15;
    }

    public static class RouteInfo {
        public final double sourceLat;
        public final double sourceLng;
        public final double destLat;
        public final double destLng;
        public final double distanceKm;

        public RouteInfo(double sourceLat, double sourceLng, double destLat, double destLng, double distanceKm) {
            this.sourceLat = sourceLat;
            this.sourceLng = sourceLng;
            this.destLat = destLat;
            this.destLng = destLng;
            this.distanceKm = distanceKm;
        }
    }
}
