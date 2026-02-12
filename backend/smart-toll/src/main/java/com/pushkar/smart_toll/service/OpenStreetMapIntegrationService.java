package com.pushkar.smart_toll.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pushkar.smart_toll.exception.InvalidPincodeException;
import com.pushkar.smart_toll.exception.RouteNotAvailableException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Production-ready Open Street Map integration service
 * Uses free, open-source APIs with no API keys required
 * - Photon for geocoding (Komoot's lightweight OSM geocoder - no strict rate limiting)
 * - OSRM for routing (Open Source Routing Machine)
 * No hardcoded values - all data dynamically retrieved
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenStreetMapIntegrationService {

    // Free tier APIs - no authentication required
    private static final String PHOTON_BASE_URL = "https://photon.komoot.io";
    private static final String OSRM_BASE_URL = "https://router.project-osrm.org";
    private static final String PINCODE_CSV_PATH = "classpath:all_india_pincode_directory_2025.csv";

    private final RestTemplate restTemplate;
    private final ResourceLoader resourceLoader;

    // In-memory cache for pincode database loaded from CSV
    private final Map<String, PincodeData> pincodeDatabase = new HashMap<>();

    @PostConstruct
    public void loadPincodeDatabase() {
        try {
            log.info("Loading pincode database from CSV file...");
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(resourceLoader.getResource(PINCODE_CSV_PATH).getInputStream())
            );

            String line;
            boolean isHeader = true;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                try {
                    // Parse CSV: circlename,regionname,divisionname,officename,pincode,officetype,delivery,district,statename,latitude,longitude
                    String[] parts = parseCSVLine(line);
                    if (parts.length >= 11) {
                        String pincode = parts[4].trim();
                        String latitude = parts[9].trim();
                        String longitude = parts[10].trim();
                        String district = parts[7].trim();
                        String stateName = parts[8].trim();

                        // Skip entries with missing coordinates
                        if (!latitude.equals("NA") && !longitude.equals("NA") && !pincode.isEmpty()) {
                            try {
                                double lat = Double.parseDouble(latitude);
                                double lng = Double.parseDouble(longitude);

                                // Keep only the first entry for each pincode to avoid duplicates
                                if (!pincodeDatabase.containsKey(pincode)) {
                                    String address = district + ", " + stateName;
                                    pincodeDatabase.put(pincode, new PincodeData(lat, lng, address));
                                }
                            } catch (NumberFormatException e) {
                                // Skip entries with invalid coordinates
                            }
                        }
                    }
                } catch (Exception e) {
                    log.trace("Error parsing pincode CSV line {}: {}", lineNumber, e.getMessage());
                }
            }

            reader.close();
            log.info("✓ Loaded {} pincodes from CSV database", pincodeDatabase.size());

        } catch (IOException e) {
            log.error("Failed to load pincode CSV database: {}", e.getMessage());
        }
    }

    /**
     * Parse CSV line handling quoted fields
     */
    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    /**
     * Get coordinates for a pincode using embedded database or Photon fallback
     * @param pincode 6-digit Indian pincode
     * @return GeocodeLocation with latitude and longitude
     * @throws InvalidPincodeException if pincode is invalid
     * @throws RouteNotAvailableException if API call fails
     */
    @Cacheable(value = "pincodeCache", key = "#pincode")
    public GeocodeLocation getLocationFromPincode(String pincode) {
        if (!isValidPincode(pincode)) {
            throw new InvalidPincodeException("Invalid pincode format: " + pincode);
        }

        try {
            log.info("Geocoding pincode: {}", pincode);

            // First, try to get from CSV pincode database
            if (pincodeDatabase.containsKey(pincode)) {
                PincodeData data = pincodeDatabase.get(pincode);
                GeocodeLocation location = new GeocodeLocation(
                    data.getLat(),
                    data.getLng(),
                    data.getName()
                );
                log.info("Geocoded pincode {} from CSV database to lat: {}, lng: {}",
                    pincode, data.getLat(), data.getLng());
                return location;
            }

            // Fallback to Photon API for unknown pincodes
            log.debug("Pincode {} not in CSV database, trying Photon API", pincode);
            return geocodeViaPhoton(pincode);

        } catch (RouteNotAvailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error geocoding pincode {}: {}", pincode, e.getMessage());
            throw new RouteNotAvailableException(
                "Failed to geocode pincode " + pincode + ". Please verify the pincode is valid.",
                e
            );
        }
    }

    private GeocodeLocation geocodeViaPhoton(String pincode) {
        try {
            // Call Photon Geocoding API (free, no rate limiting as strict as Nominatim)
            // Photon searches for postal codes, addresses, etc.
            String url = String.format(
                "%s/api?q=%s&limit=1&lang=en",
                PHOTON_BASE_URL, pincode
            );

            log.debug("Calling Photon Geocode API: {}", url);

            PhotonGeocodeResponse response = restTemplate.getForObject(url, PhotonGeocodeResponse.class);

            if (response == null || response.getFeatures() == null || response.getFeatures().isEmpty()) {
                throw new RouteNotAvailableException(
                    "Pincode " + pincode + " not found. Ensure it's a valid Indian pincode."
                );
            }

            PhotonFeature feature = response.getFeatures().get(0);
            double lat = feature.getGeometry().getCoordinates().get(1);
            double lng = feature.getGeometry().getCoordinates().get(0);
            String address = feature.getProperties().getName();

            GeocodeLocation location = new GeocodeLocation(lat, lng, address);

            log.info("Geocoded pincode {} via Photon to lat: {}, lng: {}", pincode, lat, lng);
            return location;

        } catch (RestClientException e) {
            log.error("Photon Geocode API error for pincode {}: {}", pincode, e.getMessage());
            throw new RouteNotAvailableException(
                "Failed to geocode pincode " + pincode + ". Please verify the pincode is valid.",
                e
            );
        }
    }

    /**
     * Get route information between two coordinates using OSRM (Open Source Routing Machine)
     * @param sourceLat Source latitude
     * @param sourceLng Source longitude
     * @param destLat Destination latitude
     * @param destLng Destination longitude
     * @return RouteInfo with distance and polyline coordinates
     * @throws RouteNotAvailableException if API call fails
     */
    @Cacheable(value = "routeCache", key = "#sourceLat + '-' + #sourceLng + '-' + #destLat + '-' + #destLng")
    public RouteInfo getRouteInfo(double sourceLat, double sourceLng, double destLat, double destLng) {
        try {
            log.info("Fetching route from ({}, {}) to ({}, {})", sourceLat, sourceLng, destLat, destLng);

            // Call OSRM Route API
            // Note: OSRM expects [lng, lat] format (GeoJSON convention)
            String url = String.format(
                "%s/route/v1/driving/%f,%f;%f,%f?geometries=polyline&overview=full&steps=false",
                OSRM_BASE_URL, sourceLng, sourceLat, destLng, destLat
            );

            log.debug("Calling OSRM Direction API: {}", url);

            OSRMDirectionResponse response = restTemplate.getForObject(url, OSRMDirectionResponse.class);

            if (response == null || response.getRoutes() == null || response.getRoutes().isEmpty()) {
                throw new RouteNotAvailableException(
                    "No route found between source and destination. OSRM returned: " + 
                    (response != null ? response.getMessage() : "null")
                );
            }

            OSRMRoute route = response.getRoutes().get(0);
            // OSRM returns distance in meters, convert to km
            double distanceKm = route.getDistance() / 1000.0;
            
            // Extract polyline coordinates (OSRM returns GeoJSON format)
            List<List<Double>> polylineCoordinates = decodePolyline(route.getGeometry());

            log.info("Route found: {} km, {} waypoints", distanceKm, polylineCoordinates.size());

            return new RouteInfo(sourceLat, sourceLng, destLat, destLng, distanceKm, polylineCoordinates);

        } catch (RestClientException e) {
            log.error("OSRM Direction API error: {}", e.getMessage());
            throw new RouteNotAvailableException(
                "Failed to fetch route information from OSRM. " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Decode Google polyline format used by OSRM (Google algorithm)
     * @param encoded Encoded polyline string
     * @return List of [lat, lng] coordinate pairs
     */
    private List<List<Double>> decodePolyline(String encoded) {
        List<List<Double>> coordinates = new ArrayList<>();
        int index = 0, lat = 0, lng = 0;

        while (index < encoded.length()) {
            int result = 0, shift = 0;
            int b;
            
            // Decode latitude
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20 && index < encoded.length());
            
            int dlat = (result & 1) != 0 ? ~(result >> 1) : result >> 1;
            lat += dlat;

            result = 0;
            shift = 0;
            
            // Decode longitude
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20 && index < encoded.length());
            
            int dlng = (result & 1) != 0 ? ~(result >> 1) : result >> 1;
            lng += dlng;

            List<Double> coord = new ArrayList<>();
            coord.add(lat / 1e5);
            coord.add(lng / 1e5);
            coordinates.add(coord);
        }

        return coordinates;
    }

    private boolean isValidPincode(String pincode) {
        return pincode != null && pincode.matches("^[0-9]{6}$");
    }

    // ==================== DTOs ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeocodeLocation {
        private double latitude;
        private double longitude;
        private String address;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteInfo {
        private double sourceLat;
        private double sourceLng;
        private double destLat;
        private double destLng;
        private double distanceKm;
        private List<List<Double>> polylineCoordinates; // List of [lat, lng]
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PhotonGeocodeResponse {
        private List<PhotonFeature> features;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PhotonFeature {
        private PhotonGeometry geometry;
        private PhotonProperties properties;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PhotonGeometry {
        private List<Double> coordinates; // [lng, lat]
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PhotonProperties {
        private String name;
        private String postcode;
        private String city;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OSRMDirectionResponse {
        private List<OSRMRoute> routes;
        private String code;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OSRMRoute {
        private double distance; // in meters
        private double duration;
        private String geometry; // encoded polyline
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PincodeData {
        private double lat;
        private double lng;
        private String name;
    }
}
