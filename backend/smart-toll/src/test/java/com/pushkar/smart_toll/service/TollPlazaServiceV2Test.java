package com.pushkar.smart_toll.service;

import com.pushkar.smart_toll.dto.TollPlazasRequestDTO;
import com.pushkar.smart_toll.dto.TollPlazasResponseDTO;
import com.pushkar.smart_toll.exception.InvalidPincodeException;
import com.pushkar.smart_toll.exception.SamePincodeException;
import com.pushkar.smart_toll.model.TollPlaza;
import com.pushkar.smart_toll.repository.TollPlazaRepository;
import com.pushkar.smart_toll.util.GeoLocationUtilV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test suite for TollPlazaServiceV2
 * Tests core business logic for toll plaza retrieval between pincodes
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("TollPlazaServiceV2 Tests")
class TollPlazaServiceV2Test {

    @Mock
    private TollPlazaRepository tollPlazaRepository;

    @Mock
    private OpenStreetMapIntegrationService osmService;

    private TollPlazaServiceV2 tollPlazaService;

    private static final String VALID_PINCODE_SOURCE = "410210"; // Kharghar
    private static final String VALID_PINCODE_DEST = "402209";   // Alibag
    private static final String INVALID_PINCODE = "12345";       // Invalid format
    private static final String SAME_PINCODE = "410210";

    @BeforeEach
    void setUp() {
        // Initialize service with mocked dependencies
        tollPlazaService = new TollPlazaServiceV2(tollPlazaRepository, osmService);
    }

    // ==================== Valid Pincode Scenarios ====================

    @Test
    @DisplayName("Should retrieve tolls for valid source and destination pincodes")
    void testFindTollsWithValidPincodes() {
        // Setup
        TollPlazasRequestDTO request = new TollPlazasRequestDTO();
        request.setSourcePincode(VALID_PINCODE_SOURCE);
        request.setDestinationPincode(VALID_PINCODE_DEST);

        OpenStreetMapIntegrationService.GeocodeLocation sourceLoc = 
            new OpenStreetMapIntegrationService.GeocodeLocation(19.0544, 73.0362, "Kharghar");
        OpenStreetMapIntegrationService.GeocodeLocation destLoc = 
            new OpenStreetMapIntegrationService.GeocodeLocation(18.9271, 72.8294, "Alibag");

        List<List<Double>> polyline = new ArrayList<>();
        polyline.add(List.of(19.0544, 73.0362));
        polyline.add(List.of(18.9271, 72.8294));

        OpenStreetMapIntegrationService.RouteInfo routeInfo = 
            new OpenStreetMapIntegrationService.RouteInfo(19.0544, 73.0362, 18.9271, 72.8294, 34.31, polyline);

        when(osmService.getLocationFromPincode(VALID_PINCODE_SOURCE)).thenReturn(sourceLoc);
        when(osmService.getLocationFromPincode(VALID_PINCODE_DEST)).thenReturn(destLoc);
        when(osmService.getRouteInfo(19.0544, 73.0362, 18.9271, 72.8294)).thenReturn(routeInfo);
        when(tollPlazaRepository.findAll()).thenReturn(createMockTollPlazas());

        // Execute
        TollPlazasResponseDTO response = tollPlazaService.findTollPlazasBetweenPincodes(request);

        // Verify
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getRoute(), "Route should not be null");
        assertEquals(VALID_PINCODE_SOURCE, response.getRoute().getSourcePincode());
        assertEquals(VALID_PINCODE_DEST, response.getRoute().getDestinationPincode());
        assertNotNull(response.getTollPlazas(), "Toll plazas list should not be null");
    }

    // ==================== Invalid Pincode Validation ====================

    @Test
    @DisplayName("Should throw InvalidPincodeException for invalid source pincode format")
    void testInvalidSourcePincodeFormat() {
        TollPlazasRequestDTO request = new TollPlazasRequestDTO();
        request.setSourcePincode(INVALID_PINCODE);
        request.setDestinationPincode(VALID_PINCODE_DEST);

        assertThrows(InvalidPincodeException.class, 
            () -> tollPlazaService.findTollPlazasBetweenPincodes(request),
            "Should throw exception for invalid source pincode");
    }

    @Test
    @DisplayName("Should throw InvalidPincodeException for invalid destination pincode format")
    void testInvalidDestinationPincodeFormat() {
        TollPlazasRequestDTO request = new TollPlazasRequestDTO();
        request.setSourcePincode(VALID_PINCODE_SOURCE);
        request.setDestinationPincode(INVALID_PINCODE);

        assertThrows(InvalidPincodeException.class,
            () -> tollPlazaService.findTollPlazasBetweenPincodes(request),
            "Should throw exception for invalid destination pincode");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "12345", "1234567", "ABCDEF", "", "4102100"})
    @DisplayName("Should reject multiple invalid pincode formats")
    void testMultipleInvalidPincodeFormats(String invalidPincode) {
        TollPlazasRequestDTO request = new TollPlazasRequestDTO();
        request.setSourcePincode(invalidPincode);
        request.setDestinationPincode(VALID_PINCODE_DEST);

        assertThrows(InvalidPincodeException.class,
            () -> tollPlazaService.findTollPlazasBetweenPincodes(request));
    }

    // ==================== Same Pincode Validation ====================

    @Test
    @DisplayName("Should throw SamePincodeException when source equals destination")
    void testSamePincodeValidation() {
        TollPlazasRequestDTO request = new TollPlazasRequestDTO();
        request.setSourcePincode(SAME_PINCODE);
        request.setDestinationPincode(SAME_PINCODE);

        assertThrows(SamePincodeException.class,
            () -> tollPlazaService.findTollPlazasBetweenPincodes(request),
            "Should throw exception when source and destination are same");
    }

    // ==================== Toll Plaza Retrieval ====================

    @Test
    @DisplayName("Should return empty list when no tolls on route")
    void testNoTollsOnRoute() {
        TollPlazasRequestDTO request = new TollPlazasRequestDTO();
        request.setSourcePincode(VALID_PINCODE_SOURCE);
        request.setDestinationPincode(VALID_PINCODE_DEST);

        OpenStreetMapIntegrationService.GeocodeLocation sourceLoc = 
            new OpenStreetMapIntegrationService.GeocodeLocation(19.0544, 73.0362, "Kharghar");
        OpenStreetMapIntegrationService.GeocodeLocation destLoc = 
            new OpenStreetMapIntegrationService.GeocodeLocation(18.9271, 72.8294, "Alibag");

        List<List<Double>> polyline = new ArrayList<>();
        polyline.add(List.of(19.0544, 73.0362));
        polyline.add(List.of(18.9271, 72.8294));

        OpenStreetMapIntegrationService.RouteInfo routeInfo = 
            new OpenStreetMapIntegrationService.RouteInfo(19.0544, 73.0362, 18.9271, 72.8294, 34.31, polyline);

        when(osmService.getLocationFromPincode(VALID_PINCODE_SOURCE)).thenReturn(sourceLoc);
        when(osmService.getLocationFromPincode(VALID_PINCODE_DEST)).thenReturn(destLoc);
        when(osmService.getRouteInfo(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(routeInfo);
        when(tollPlazaRepository.findAll()).thenReturn(new ArrayList<>()); // Empty list

        TollPlazasResponseDTO response = tollPlazaService.findTollPlazasBetweenPincodes(request);

        assertEquals(0, response.getTollPlazas().size(), "Should have no tolls for empty database");
    }

    @Test
    @DisplayName("Should return tolls sorted by distance from source")
    void testTollsSortedByDistance() {
        TollPlazasRequestDTO request = new TollPlazasRequestDTO();
        request.setSourcePincode(VALID_PINCODE_SOURCE);
        request.setDestinationPincode(VALID_PINCODE_DEST);

        OpenStreetMapIntegrationService.GeocodeLocation sourceLoc = 
            new OpenStreetMapIntegrationService.GeocodeLocation(19.0544, 73.0362, "Kharghar");
        OpenStreetMapIntegrationService.GeocodeLocation destLoc = 
            new OpenStreetMapIntegrationService.GeocodeLocation(18.9271, 72.8294, "Alibag");

        List<List<Double>> polyline = new ArrayList<>();
        polyline.add(List.of(19.0544, 73.0362));
        polyline.add(List.of(19.00, 73.00));
        polyline.add(List.of(18.9271, 72.8294));

        OpenStreetMapIntegrationService.RouteInfo routeInfo = 
            new OpenStreetMapIntegrationService.RouteInfo(19.0544, 73.0362, 18.9271, 72.8294, 34.31, polyline);

        when(osmService.getLocationFromPincode(VALID_PINCODE_SOURCE)).thenReturn(sourceLoc);
        when(osmService.getLocationFromPincode(VALID_PINCODE_DEST)).thenReturn(destLoc);
        when(osmService.getRouteInfo(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(routeInfo);
        when(tollPlazaRepository.findAll()).thenReturn(createMockTollPlazas());

        TollPlazasResponseDTO response = tollPlazaService.findTollPlazasBetweenPincodes(request);

        // Verify sorting
        if (response.getTollPlazas().size() > 1) {
            for (int i = 0; i < response.getTollPlazas().size() - 1; i++) {
                double currentDistance = response.getTollPlazas().get(i).getDistanceFromSource();
                double nextDistance = response.getTollPlazas().get(i + 1).getDistanceFromSource();
                assertTrue(currentDistance <= nextDistance, "Tolls should be sorted by distance");
            }
        }
    }

    // ==================== Route Distance Validation ====================

    @Test
    @DisplayName("Should return correct route distance")
    void testRouteDistanceCalculation() {
        TollPlazasRequestDTO request = new TollPlazasRequestDTO();
        request.setSourcePincode(VALID_PINCODE_SOURCE);
        request.setDestinationPincode(VALID_PINCODE_DEST);

        OpenStreetMapIntegrationService.GeocodeLocation sourceLoc = 
            new OpenStreetMapIntegrationService.GeocodeLocation(19.0544, 73.0362, "Kharghar");
        OpenStreetMapIntegrationService.GeocodeLocation destLoc = 
            new OpenStreetMapIntegrationService.GeocodeLocation(18.9271, 72.8294, "Alibag");

        double expectedDistance = 34.31;
        List<List<Double>> polyline = new ArrayList<>();
        polyline.add(List.of(19.0544, 73.0362));
        polyline.add(List.of(18.9271, 72.8294));

        OpenStreetMapIntegrationService.RouteInfo routeInfo = 
            new OpenStreetMapIntegrationService.RouteInfo(19.0544, 73.0362, 18.9271, 72.8294, expectedDistance, polyline);

        when(osmService.getLocationFromPincode(VALID_PINCODE_SOURCE)).thenReturn(sourceLoc);
        when(osmService.getLocationFromPincode(VALID_PINCODE_DEST)).thenReturn(destLoc);
        when(osmService.getRouteInfo(19.0544, 73.0362, 18.9271, 72.8294)).thenReturn(routeInfo);
        when(tollPlazaRepository.findAll()).thenReturn(createMockTollPlazas());

        TollPlazasResponseDTO response = tollPlazaService.findTollPlazasBetweenPincodes(request);

        assertEquals(expectedDistance, response.getRoute().getDistanceInKm(), 0.01, 
            "Distance should match expected value");
    }

    // ==================== Null Request Handling ====================

    @Test
    @DisplayName("Should handle null source pincode")
    void testNullSourcePincode() {
        TollPlazasRequestDTO request = new TollPlazasRequestDTO();
        request.setSourcePincode(null);
        request.setDestinationPincode(VALID_PINCODE_DEST);

        assertThrows(Exception.class,
            () -> tollPlazaService.findTollPlazasBetweenPincodes(request));
    }

    @Test
    @DisplayName("Should handle null destination pincode")
    void testNullDestinationPincode() {
        TollPlazasRequestDTO request = new TollPlazasRequestDTO();
        request.setSourcePincode(VALID_PINCODE_SOURCE);
        request.setDestinationPincode(null);

        assertThrows(Exception.class,
            () -> tollPlazaService.findTollPlazasBetweenPincodes(request));
    }

    // ==================== Helper Methods ====================

    private List<TollPlaza> createMockTollPlazas() {
        List<TollPlaza> tolls = new ArrayList<>();
        
        TollPlaza toll1 = new TollPlaza();
        toll1.setId(1L);
        toll1.setName("Panvel Bypass Naka Toll Plaza");
        toll1.setLatitude(19.05);
        toll1.setLongitude(73.03);
        tolls.add(toll1);

        TollPlaza toll2 = new TollPlaza();
        toll2.setId(2L);
        toll2.setName("SPTPL Navi Mumbai Toll Plaza");
        toll2.setLatitude(19.00);
        toll2.setLongitude(73.00);
        tolls.add(toll2);

        return tolls;
    }
}
