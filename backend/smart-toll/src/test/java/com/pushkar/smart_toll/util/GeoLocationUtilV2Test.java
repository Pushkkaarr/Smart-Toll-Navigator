package com.pushkar.smart_toll.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for GeoLocationUtilV2
 * Tests geolocation calculations including distance, proximity, and bounding boxes
 */
@DisplayName("GeoLocationUtilV2 Tests")
class GeoLocationUtilV2Test {

    private GeoLocationUtilV2 geoLocationUtil;

    // Test coordinates for major Indian cities
    private static final double BANGALORE_LAT = 12.9716;
    private static final double BANGALORE_LNG = 77.5946;
    
    private static final double PUNE_LAT = 18.5204;
    private static final double PUNE_LNG = 73.8567;
    
    private static final double KHARGHAR_LAT = 19.0544;
    private static final double KHARGHAR_LNG = 73.0362;
    
    private static final double ALIBAG_LAT = 18.9271;
    private static final double ALIBAG_LNG = 72.8294;
    
    private static final double MUMBAI_LAT = 19.0760;
    private static final double MUMBAI_LNG = 72.8777;

    @BeforeEach
    void setUp() {
        geoLocationUtil = new GeoLocationUtilV2();
    }

    // ==================== Distance Calculations ====================

    @Test
    @DisplayName("Should calculate distance between Bangalore and Pune")
    void testDistanceBangaloreToPune() {
        double distance = geoLocationUtil.calculateDistance(BANGALORE_LAT, BANGALORE_LNG, PUNE_LAT, PUNE_LNG);
        
        // Actual distance is approximately 560 km
        assertTrue(distance > 550 && distance < 570, 
            "Bangalore to Pune distance should be ~560 km, got: " + distance);
    }

    @Test
    @DisplayName("Should calculate distance between Kharghar and Alibag")
    void testDistanceKhargharToAlibag() {
        double distance = geoLocationUtil.calculateDistance(KHARGHAR_LAT, KHARGHAR_LNG, ALIBAG_LAT, ALIBAG_LNG);
        
        // Actual distance is approximately 34 km
        assertTrue(distance > 30 && distance < 40,
            "Kharghar to Alibag distance should be ~34 km, got: " + distance);
    }

    @Test
    @DisplayName("Should return zero distance for same location")
    void testZeroDistance() {
        double distance = geoLocationUtil.calculateDistance(BANGALORE_LAT, BANGALORE_LNG, BANGALORE_LAT, BANGALORE_LNG);
        
        assertEquals(0.0, distance, 0.001, "Distance between same points should be zero");
    }

    @ParameterizedTest
    @CsvSource({
        "19.0760, 72.8777, 19.0544, 73.0362, 20, 30",  // Mumbai to Kharghar ~20-30 km
        "18.5204, 73.8567, 19.0760, 72.8777, 120, 140", // Pune to Mumbai ~120-140 km
        "12.9716, 77.5946, 19.0760, 72.8777, 350, 380"  // Bangalore to Mumbai ~350-380 km
    })
    @DisplayName("Should calculate distances for multiple city pairs")
    void testDistancesForMultipleCities(double lat1, double lng1, double lat2, double lng2, 
                                        double minExpected, double maxExpected) {
        double distance = geoLocationUtil.calculateDistance(lat1, lng1, lat2, lng2);
        
        assertTrue(distance >= minExpected && distance <= maxExpected,
            String.format("Distance should be between %.0f and %.0f km, got: %.2f km", 
                minExpected, maxExpected, distance));
    }

    // ==================== Route Proximity Detection ====================

    @Test
    @DisplayName("Should detect if point is near to route (within tolerance)")
    void testPointNearRoute() {
        // Create a simple route: Kharghar to Alibag
        List<List<Double>> polyline = new ArrayList<>();
        polyline.add(List.of(KHARGHAR_LAT, KHARGHAR_LNG));
        polyline.add(List.of(ALIBAG_LAT, ALIBAG_LNG));
        
        // Test point very close to the route
        double testLat = (KHARGHAR_LAT + ALIBAG_LAT) / 2;  // Midpoint
        double testLng = (KHARGHAR_LNG + ALIBAG_LNG) / 2;
        
        boolean isNear = geoLocationUtil.isTollNearRoute(polyline, testLat, testLng);
        assertTrue(isNear, "Midpoint should be near the route");
    }

    @Test
    @DisplayName("Should reject point far from route")
    void testPointFarFromRoute() {
        // Create a route: Kharghar to Alibag
        List<List<Double>> polyline = new ArrayList<>();
        polyline.add(List.of(KHARGHAR_LAT, KHARGHAR_LNG));
        polyline.add(List.of(ALIBAG_LAT, ALIBAG_LNG));
        
        // Test point very far from the route (Bangalore)
        boolean isNear = geoLocationUtil.isTollNearRoute(polyline, BANGALORE_LAT, BANGALORE_LNG);
        assertFalse(isNear, "Mumbai should not be near Kharghar-Alibag route");
    }

    @Test
    @DisplayName("Should handle single-point polyline (no route)")
    void testSinglePointPolyline() {
        List<List<Double>> polyline = new ArrayList<>();
        polyline.add(List.of(KHARGHAR_LAT, KHARGHAR_LNG));
        
        boolean isNear = geoLocationUtil.isTollNearRoute(polyline, KHARGHAR_LAT, KHARGHAR_LNG);
        assertTrue(isNear, "Point at route start should be near");
    }

    @Test
    @DisplayName("Should handle empty polyline")
    void testEmptyPolyline() {
        List<List<Double>> polyline = new ArrayList<>();
        
        boolean isNear = geoLocationUtil.isTollNearRoute(polyline, KHARGHAR_LAT, KHARGHAR_LNG);
        assertFalse(isNear, "Empty polyline should return false");
    }

    // ==================== Bounding Box Tests ====================

    /*
    @Test
    @DisplayName("Should generate bounding box around location")
    void testBoundingBoxGeneration() {
        double radius = 50.0; // 50km radius
        double[] box = geoLocationUtil.getBoundingBox(BANGALORE_LAT, BANGALORE_LNG, radius);
        
        assertNotNull(box, "Bounding box should not be null");
        assertEquals(4, box.length, "Bounding box should have 4 values [minLat, maxLat, minLng, maxLng]");
        
        // Verify bounds
        assertTrue(box[0] < BANGALORE_LAT, "Min latitude should be less than center");
        assertTrue(box[1] > BANGALORE_LAT, "Max latitude should be greater than center");
        assertTrue(box[2] < BANGALORE_LNG, "Min longitude should be less than center");
        assertTrue(box[3] > BANGALORE_LNG, "Max longitude should be greater than center");
    }

    @Test
    @DisplayName("Should have valid bounds (minLat < maxLat, minLng < maxLng)")
    void testBoundingBoxValidity() {
        double[] box = geoLocationUtil.getBoundingBox(PUNE_LAT, PUNE_LNG, 100.0);
        
        assertTrue(box[0] < box[1], "Min latitude should be less than max latitude");
        assertTrue(box[2] < box[3], "Min longitude should be less than max longitude");
    }

    @ParameterizedTest
    @CsvSource({
        "12.9716, 77.5946, 10",   // Bangalore, 10km
        "19.0760, 72.8777, 50",   // Mumbai, 50km
        "18.5204, 73.8567, 100"   // Pune, 100km
    })
    @DisplayName("Should generate bounding boxes for multiple locations")
    void testMultipleBoundingBoxes(double lat, double lng, double radiusKm) {
        double[] box = geoLocationUtil.getBoundingBox(lat, lng, radiusKm);
        
        assertNotNull(box);
        assertEquals(4, box.length);
        assertTrue(box[0] < box[1]);
        assertTrue(box[2] < box[3]);
    }
    */

    // ==================== Point-to-Segment Distance ====================

    /*
    @Test
    @DisplayName("Should calculate perpendicular distance from point to line segment")
    void testPointToSegmentDistance() {
        // Line segment from Kharghar to Alibag
        double lat1 = KHARGHAR_LAT, lng1 = KHARGHAR_LNG;
        double lat2 = ALIBAG_LAT, lng2 = ALIBAG_LNG;
        
        // Midpoint should have minimal distance
        double midLat = (lat1 + lat2) / 2;
        double midLng = (lng1 + lng2) / 2;
        
        double distance = geoLocationUtil.distancePointToLineSegment(midLat, midLng, lat1, lng1, lat2, lng2);
        
        assertTrue(distance < 5.0, "Distance from midpoint to segment should be small, got: " + distance);
    }

    @Test
    @DisplayName("Should return small distance for point on line segment")
    void testPointOnLineSegment() {
        double lat1 = 19.0, lng1 = 73.0;
        double lat2 = 18.9, lng2 = 72.9;
        
        // Point exactly on segment (average)
        double pointLat = (lat1 + lat2) / 2;
        double pointLng = (lng1 + lng2) / 2;
        
        double distance = geoLocationUtil.distancePointToLineSegment(pointLat, pointLng, lat1, lng1, lat2, lng2);
        
        assertTrue(distance < 1.0, "Point on segment should have near-zero distance");
    }
    */

    // ==================== Distance Along Route ====================

    @Test
    @DisplayName("Should calculate cumulative distance from start of route")
    void testCumulativeDistanceFromStart() {
        List<List<Double>> polyline = new ArrayList<>();
        polyline.add(List.of(KHARGHAR_LAT, KHARGHAR_LNG));
        polyline.add(List.of((KHARGHAR_LAT + ALIBAG_LAT) / 2, (KHARGHAR_LNG + ALIBAG_LNG) / 2)); // Midpoint
        polyline.add(List.of(ALIBAG_LAT, ALIBAG_LNG));
        
        // Distance to start should be zero
        double distToStart = geoLocationUtil.distanceFromRouteStart(polyline, KHARGHAR_LAT, KHARGHAR_LNG);
        assertEquals(0.0, distToStart, 2.0, "Distance from route start should be near zero");
    }

    @Test
    @DisplayName("Should calculate increasing distance along route")
    void testIncreasingDistanceAlongRoute() {
        List<List<Double>> polyline = new ArrayList<>();
        polyline.add(List.of(BANGALORE_LAT, BANGALORE_LNG));
        polyline.add(List.of(PUNE_LAT, PUNE_LNG));
        
        // Distance to start should be small
        double distToStart = geoLocationUtil.distanceFromRouteStart(polyline, BANGALORE_LAT, BANGALORE_LNG);
        
        // Distance to end should be larger
        double distToEnd = geoLocationUtil.distanceFromRouteStart(polyline, PUNE_LAT, PUNE_LNG);
        
        assertTrue(distToEnd > distToStart, "Distance should increase along route");
    }

    // ==================== India Boundary Tests ====================

    /*
    @Test
    @DisplayName("Should validate coordinates within India bounds")
    void testIndiaBoundaryValidation() {
        // Valid Indian coordinates
        assertTrue(geoLocationUtil.isWithinIndiaBounds(BANGALORE_LAT, BANGALORE_LNG), 
            "Bangalore should be within India bounds");
        assertTrue(geoLocationUtil.isWithinIndiaBounds(MUMBAI_LAT, MUMBAI_LNG),
            "Mumbai should be within India bounds");
    }

    @Test
    @DisplayName("Should reject coordinates outside India")
    void testOutsideIndiaBoundary() {
        double pakistanLat = 33.0, pakistanLng = 67.0;
        assertFalse(geoLocationUtil.isWithinIndiaBounds(pakistanLat, pakistanLng),
            "Pakistan should be outside India bounds");
        
        double earthLat = 0.0, earthLng = 0.0; // Near equator/prime meridian
        assertFalse(geoLocationUtil.isWithinIndiaBounds(earthLat, earthLng),
            "Near equator should be outside India bounds");
    }

    @ParameterizedTest
    @CsvSource({
        "8.0, 68.0",    // Southwest corner
        "35.0, 97.0",   // Northeast corner
        "20.0, 80.0"    // Center
    })
    @DisplayName("Should validate multiple coordinates within India")
    void testMultipleIndiaBoundaryChecks(double lat, double lng) {
        assertTrue(geoLocationUtil.isWithinIndiaBounds(lat, lng),
            String.format("Coordinates (%.1f, %.1f) should be within India bounds", lat, lng));
    }
    */

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle very small distances")
    void testVerySmallDistance() {
        double distance = geoLocationUtil.calculateDistance(
            BANGALORE_LAT, BANGALORE_LNG,
            BANGALORE_LAT + 0.0001, BANGALORE_LNG + 0.0001
        );
        
        assertTrue(distance >= 0 && distance < 1.0, "Small distance should be positive and less than 1km");
    }

    @Test
    @DisplayName("Should handle very large distances")
    void testVeryLargeDistance() {
        // Distance from southern tip to northern tip of India
        double southLat = 8.0, southLng = 77.0;
        double northLat = 35.0, northLng = 77.0;
        
        double distance = geoLocationUtil.calculateDistance(southLat, southLng, northLat, northLng);
        
        assertTrue(distance > 2500 && distance < 3000, 
            "North-south India distance should be ~3000km, got: " + distance);
    }

    @Test
    @DisplayName("Should be symmetric (distance A->B equals B->A)")
    void testDistanceSymmetry() {
        double distAtoB = geoLocationUtil.calculateDistance(BANGALORE_LAT, BANGALORE_LNG, PUNE_LAT, PUNE_LNG);
        double distBtoA = geoLocationUtil.calculateDistance(PUNE_LAT, PUNE_LNG, BANGALORE_LAT, BANGALORE_LNG);
        
        assertEquals(distAtoB, distBtoA, 0.01, "Distance should be symmetric");
    }
}
