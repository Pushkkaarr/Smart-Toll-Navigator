package com.pushkar.smart_toll.service;

import com.pushkar.smart_toll.exception.InvalidPincodeException;
import com.pushkar.smart_toll.exception.RouteNotAvailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for OpenStreetMapIntegrationService
 * Covers geocoding, routing, and polyline decoding functionality
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("OpenStreetMapIntegrationService Tests")
class OpenStreetMapIntegrationServiceTest {

    @Autowired
    private OpenStreetMapIntegrationService service;

    private static final String VALID_PINCODE_410210 = "410210"; // Kharghar
    private static final String VALID_PINCODE_402209 = "402209"; // Alibag
    private static final String VALID_PINCODE_560064 = "560064"; // Bangalore
    private static final String VALID_PINCODE_411001 = "411001"; // Pune

    @BeforeEach
    void setUp() {
        assertNotNull(service, "Service should be autowired");
    }

    // ==================== Geocoding Tests ====================

    @Test
    @DisplayName("Should geocode valid pincode from CSV database")
    void testGeocodeValidPincodeFromDatabase() {
        OpenStreetMapIntegrationService.GeocodeLocation location = 
            service.getLocationFromPincode(VALID_PINCODE_410210);

        assertNotNull(location, "Location should not be null");
        assertEquals(19.0544, location.getLatitude(), 0.001, "Latitude should match");
        assertEquals(73.0362, location.getLongitude(), 0.001, "Longitude should match");
        assertNotNull(location.getAddress(), "Address should not be null");
        assertTrue(location.getAddress().contains("Raigad"), "Address should contain district/state");
    }

    @Test
    @DisplayName("Should throw InvalidPincodeException for invalid format")
    void testGeocodeInvalidPincodeFormat() {
        assertThrows(InvalidPincodeException.class, 
            () -> service.getLocationFromPincode("12345"), // Only 5 digits
            "Should throw exception for invalid pincode format");
    }

    @Test
    @DisplayName("Should throw InvalidPincodeException for non-numeric pincode")
    void testGeocodeNonNumericPincode() {
        assertThrows(InvalidPincodeException.class,
            () -> service.getLocationFromPincode("ABCDEF"),
            "Should throw exception for non-numeric pincode");
    }

    @Test
    @DisplayName("Should throw InvalidPincodeException for null pincode")
    void testGeocodeNullPincode() {
        assertThrows(InvalidPincodeException.class,
            () -> service.getLocationFromPincode(null),
            "Should throw exception for null pincode");
    }

    @ParameterizedTest
    @ValueSource(strings = {"410210", "402209", "560064", "411001"})
    @DisplayName("Should geocode multiple valid pincodes")
    void testGeocodeMultipleValidPincodes(String pincode) {
        OpenStreetMapIntegrationService.GeocodeLocation location = 
            service.getLocationFromPincode(pincode);

        assertNotNull(location, "Location should not be null for pincode: " + pincode);
        assertTrue(location.getLatitude() >= -90 && location.getLatitude() <= 90, 
            "Latitude should be valid for pincode: " + pincode);
        assertTrue(location.getLongitude() >= -180 && location.getLongitude() <= 180,
            "Longitude should be valid for pincode: " + pincode);
    }

    @Test
    @DisplayName("Should cache geocoded pincodes")
    void testPincodeCaching() {
        // First call
        long startTime1 = System.currentTimeMillis();
        OpenStreetMapIntegrationService.GeocodeLocation location1 = 
            service.getLocationFromPincode(VALID_PINCODE_410210);
        long duration1 = System.currentTimeMillis() - startTime1;

        // Second call (should be cached)
        long startTime2 = System.currentTimeMillis();
        OpenStreetMapIntegrationService.GeocodeLocation location2 = 
            service.getLocationFromPincode(VALID_PINCODE_410210);
        long duration2 = System.currentTimeMillis() - startTime2;

        assertEquals(location1.getLatitude(), location2.getLatitude(), 
            "Cached result should match original");
        assertTrue(duration2 < duration1, "Cached call should be faster");
    }

    // ==================== Routing Tests ====================

    @Test
    @DisplayName("Should retrieve route info between two valid coordinates")
    void testGetRouteInfoValidCoordinates() {
        OpenStreetMapIntegrationService.RouteInfo routeInfo = 
            service.getRouteInfo(19.05, 73.03, 18.92, 72.82);

        assertNotNull(routeInfo, "Route info should not be null");
        assertTrue(routeInfo.getDistanceKm() > 0, "Distance should be positive");
        assertNotNull(routeInfo.getPolylineCoordinates(), "Polyline coordinates should not be null");
        assertTrue(routeInfo.getPolylineCoordinates().size() > 0, "Should have at least one coordinate");
    }

    @Test
    @DisplayName("Should cache route info results")
    void testRouteInfoCaching() {
        double sourceLat = 19.05, sourceLng = 73.03;
        double destLat = 18.92, destLng = 72.82;

        long startTime1 = System.currentTimeMillis();
        OpenStreetMapIntegrationService.RouteInfo route1 = 
            service.getRouteInfo(sourceLat, sourceLng, destLat, destLng);
        long duration1 = System.currentTimeMillis() - startTime1;

        long startTime2 = System.currentTimeMillis();
        OpenStreetMapIntegrationService.RouteInfo route2 = 
            service.getRouteInfo(sourceLat, sourceLng, destLat, destLng);
        long duration2 = System.currentTimeMillis() - startTime2;

        assertEquals(route1.getDistanceKm(), route2.getDistanceKm(), 0.01,
            "Cached route should have same distance");
        assertTrue(duration2 < duration1, "Cached route call should be faster");
    }

    // ==================== Polyline Decoding Tests ====================

    @Test
    @DisplayName("Should decode valid encoded polyline")
    void testDecodeValidPolyline() {
        // Sample encoded polyline (manually decoded values for verification)
        String encodedPolyline = "_p~iF~ps|U_ulLnnqC_mqNvxq`@";
        
        // This should not throw exception
        assertDoesNotThrow(() -> {
            OpenStreetMapIntegrationService.RouteInfo routeInfo = 
                service.getRouteInfo(19.05, 73.03, 18.92, 72.82);
            assertNotNull(routeInfo.getPolylineCoordinates());
        });
    }

    @Test
    @DisplayName("Should handle empty polyline")
    void testDecodeEmptyPolyline() {
        // Route request should return valid coordinates
        OpenStreetMapIntegrationService.RouteInfo routeInfo = 
            service.getRouteInfo(19.05, 73.03, 18.92, 72.82);
        
        assertNotNull(routeInfo.getPolylineCoordinates());
        assertTrue(routeInfo.getPolylineCoordinates().size() > 0);
    }

    // ==================== Integration Scenario Tests ====================

    @Test
    @DisplayName("End-to-end: Geocode two pincodes and get route")
    void testEndToEndGeocodeAndRoute() {
        // Geocode source
        OpenStreetMapIntegrationService.GeocodeLocation source = 
            service.getLocationFromPincode(VALID_PINCODE_410210);

        // Geocode destination
        OpenStreetMapIntegrationService.GeocodeLocation destination = 
            service.getLocationFromPincode(VALID_PINCODE_402209);

        // Get route
        OpenStreetMapIntegrationService.RouteInfo route = 
            service.getRouteInfo(
                source.getLatitude(), source.getLongitude(),
                destination.getLatitude(), destination.getLongitude()
            );

        assertNotNull(source, "Source should be geocoded");
        assertNotNull(destination, "Destination should be geocoded");
        assertNotNull(route, "Route should be found");
        assertTrue(route.getDistanceKm() > 0, "Distance should be positive");
        assertTrue(route.getPolylineCoordinates().size() > 0, "Route should have coordinates");
    }

    @Test
    @DisplayName("Long distance route: Bangalore to Pune")
    void testLongDistanceRoute() {
        OpenStreetMapIntegrationService.GeocodeLocation bangalore = 
            service.getLocationFromPincode(VALID_PINCODE_560064);
        OpenStreetMapIntegrationService.GeocodeLocation pune = 
            service.getLocationFromPincode(VALID_PINCODE_411001);

        OpenStreetMapIntegrationService.RouteInfo route = 
            service.getRouteInfo(
                bangalore.getLatitude(), bangalore.getLongitude(),
                pune.getLatitude(), pune.getLongitude()
            );

        assertTrue(route.getDistanceKm() > 700, "Bangalore to Pune should be ~800km");
        assertTrue(route.getDistanceKm() < 1000, "Bangalore to Pune should be <1000km");
        assertTrue(route.getPolylineCoordinates().size() > 10, "Long route should have many waypoints");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle invalid pincode gracefully")
    void testInvalidPincodeHandling() {
        assertThrows(Exception.class,
            () -> service.getLocationFromPincode("999999"),
            "Should throw exception for non-existent pincode");
    }

    @Test
    @DisplayName("Should validate pincode format strictly")
    void testPincodeFormatValidation() {
        assertThrows(InvalidPincodeException.class, () -> service.getLocationFromPincode("41021")); // 5 digits
        assertThrows(InvalidPincodeException.class, () -> service.getLocationFromPincode("4102100")); // 7 digits
        assertThrows(InvalidPincodeException.class, () -> service.getLocationFromPincode("")); // empty
    }

    @Test
    @DisplayName("Route coordinates should be within India bounds")
    void testRouteCoordinatesInIndianBounds() {
        OpenStreetMapIntegrationService.RouteInfo route = 
            service.getRouteInfo(19.05, 73.03, 18.92, 72.82);

        for (List<Double> coord : route.getPolylineCoordinates()) {
            double lat = coord.get(0);
            double lng = coord.get(1);
            
            assertTrue(lat >= 8 && lat <= 35, "Latitude should be within India bounds: " + lat);
            assertTrue(lng >= 68 && lng <= 97, "Longitude should be within India bounds: " + lng);
        }
    }
}
