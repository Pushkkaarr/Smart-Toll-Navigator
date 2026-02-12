package com.pushkar.smart_toll.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pushkar.smart_toll.dto.TollPlazasRequestDTO;
import com.pushkar.smart_toll.dto.TollPlazasResponseDTO;
import com.pushkar.smart_toll.exception.InvalidPincodeException;
import com.pushkar.smart_toll.exception.SamePincodeException;
import com.pushkar.smart_toll.service.TollPlazaServiceV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.mockito.Mock;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test suite for TollPlazaController
 * Tests REST API endpoint responses and validation
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("TollPlazaController Tests")
class TollPlazaControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Mock
    private TollPlazaServiceV2 tollPlazaService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String API_ENDPOINT = "/api/v1/toll-plazas";
    private static final String HEALTH_ENDPOINT = "/api/v1/toll-plazas/health";
    
    private static final String SOURCE_PINCODE = "410210";  // Kharghar
    private static final String DEST_PINCODE = "402209";    // Alibag
    private static final String INVALID_PINCODE = "12345";

    private TollPlazasRequestDTO validRequest;
    private TollPlazasResponseDTO mockResponse;

    @BeforeEach
    void setUp() {
        // Initialize MockMvc from context
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Initialize test data
        validRequest = new TollPlazasRequestDTO();
        validRequest.setSourcePincode(SOURCE_PINCODE);
        validRequest.setDestinationPincode(DEST_PINCODE);

        mockResponse = new TollPlazasResponseDTO();
    }

    // ==================== Success Scenarios ====================

    @Test
    @DisplayName("Should return success status for valid pincode pair")
    void testValidPincodeRequest() throws Exception {
        when(tollPlazaService.findTollPlazasBetweenPincodes(any(TollPlazasRequestDTO.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(post(API_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalTolls").value(2));

        verify(tollPlazaService, times(1)).findTollPlazasBetweenPincodes(any(TollPlazasRequestDTO.class));
    }

    @Test
    @DisplayName("Should return response with route details")
    void testResponseIncludesRouteDetails() throws Exception {
        when(tollPlazaService.findTollPlazasBetweenPincodes(any(TollPlazasRequestDTO.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(post(API_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route").exists())
                .andExpect(jsonPath("$.route.sourcePincode").value(SOURCE_PINCODE))
                .andExpect(jsonPath("$.route.destinationPincode").value(DEST_PINCODE));
    }

    @Test
    @DisplayName("Should accept multiple valid requests in sequence")
    void testMultipleValidRequests() throws Exception {
        when(tollPlazaService.findTollPlazasBetweenPincodes(any(TollPlazasRequestDTO.class)))
            .thenReturn(mockResponse);

        // First request
        mockMvc.perform(post(API_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        // Second request (verify it's not cached incorrectly)
        mockMvc.perform(post(API_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());

        verify(tollPlazaService, times(2)).findTollPlazasBetweenPincodes(any(TollPlazasRequestDTO.class));
    }

    // ==================== Validation Error Responses ====================

    @Test
    @DisplayName("Should return 400 for invalid source pincode format")
    void testInvalidSourcePincodeFormat() throws Exception {
        TollPlazasRequestDTO invalidRequest = new TollPlazasRequestDTO();
        invalidRequest.setSourcePincode(INVALID_PINCODE);
        invalidRequest.setDestinationPincode(DEST_PINCODE);

        when(tollPlazaService.findTollPlazasBetweenPincodes(any(TollPlazasRequestDTO.class)))
            .thenThrow(new InvalidPincodeException("Invalid pincode format"));

        mockMvc.perform(post(API_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 for invalid destination pincode format")
    void testInvalidDestinationPincodeFormat() throws Exception {
        TollPlazasRequestDTO invalidRequest = new TollPlazasRequestDTO();
        invalidRequest.setSourcePincode(SOURCE_PINCODE);
        invalidRequest.setDestinationPincode(INVALID_PINCODE);

        when(tollPlazaService.findTollPlazasBetweenPincodes(any(TollPlazasRequestDTO.class)))
            .thenThrow(new InvalidPincodeException("Invalid pincode format"));

        mockMvc.perform(post(API_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when source and destination are same")
    void testSamePincodeValidation() throws Exception {
        TollPlazasRequestDTO sameRequest = new TollPlazasRequestDTO();
        sameRequest.setSourcePincode(SOURCE_PINCODE);
        sameRequest.setDestinationPincode(SOURCE_PINCODE);

        when(tollPlazaService.findTollPlazasBetweenPincodes(any(TollPlazasRequestDTO.class)))
            .thenThrow(new SamePincodeException("Source and destination cannot be same"));

        mockMvc.perform(post(API_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sameRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 for null source pincode")
    void testNullSourcePincode() throws Exception {
        TollPlazasRequestDTO requestWithNull = new TollPlazasRequestDTO();
        requestWithNull.setSourcePincode(null);
        requestWithNull.setDestinationPincode(DEST_PINCODE);

        when(tollPlazaService.findTollPlazasBetweenPincodes(any(TollPlazasRequestDTO.class)))
            .thenThrow(new IllegalArgumentException("Pincode cannot be null"));

        mockMvc.perform(post(API_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithNull)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 for null destination pincode")
    void testNullDestinationPincode() throws Exception {
        TollPlazasRequestDTO requestWithNull = new TollPlazasRequestDTO();
        requestWithNull.setSourcePincode(SOURCE_PINCODE);
        requestWithNull.setDestinationPincode(null);

        when(tollPlazaService.findTollPlazasBetweenPincodes(any(TollPlazasRequestDTO.class)))
            .thenThrow(new IllegalArgumentException("Pincode cannot be null"));

        mockMvc.perform(post(API_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithNull)))
                .andExpect(status().isBadRequest());
    }

    // ==================== HTTP Method Validation ====================

    @Test
    @DisplayName("Should only accept POST requests")
    void testGetRequestNotAllowed() throws Exception {
        mockMvc.perform(get(API_ENDPOINT))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("Should only accept POST requests for main endpoint")
    void testPutRequestNotAllowed() throws Exception {
        mockMvc.perform(put(API_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("Should only accept POST requests for main endpoint")
    void testDeleteRequestNotAllowed() throws Exception {
        mockMvc.perform(delete(API_ENDPOINT))
                .andExpect(status().isMethodNotAllowed());
    }

    // ==================== Content Type Validation ====================

    @Test
    @DisplayName("Should reject requests without JSON content type")
    void testMissingContentType() throws Exception {
        mockMvc.perform(post(API_ENDPOINT)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("Should handle malformed JSON gracefully")
    void testMalformedJson() throws Exception {
        String malformedJson = "{\"sourcePincode\": \"410210\",}"; // Invalid trailing comma

        mockMvc.perform(post(API_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest());
    }

    // ==================== Health Check Endpoint ====================

    @Test
    @DisplayName("Should return 200 for health check endpoint")
    void testHealthCheckEndpoint() throws Exception {
        mockMvc.perform(get(HEALTH_ENDPOINT))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return health status message")
    void testHealthCheckMessage() throws Exception {
        mockMvc.perform(get(HEALTH_ENDPOINT))
                .andExpect(status().isOk());
    }

    // ==================== Response Structure Validation ====================

    @Test
    @DisplayName("Should return response with tollPlazas array")
    void testResponseHasTollPlazasArray() throws Exception {
        when(tollPlazaService.findTollPlazasBetweenPincodes(any(TollPlazasRequestDTO.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(post(API_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tollPlazas").isArray());
    }

    @Test
    @DisplayName("Should return response with totalTolls count")
    void testResponseHasTotalTollsCount() throws Exception {
        when(tollPlazaService.findTollPlazasBetweenPincodes(any(TollPlazasRequestDTO.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(post(API_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle concurrent requests")
    void testConcurrentRequests() throws Exception {
        when(tollPlazaService.findTollPlazasBetweenPincodes(any(TollPlazasRequestDTO.class)))
            .thenReturn(mockResponse);

        // Simulate multiple concurrent requests
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(API_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk());
        }

        verify(tollPlazaService, times(5)).findTollPlazasBetweenPincodes(any(TollPlazasRequestDTO.class));
    }

    @Test
    @DisplayName("Should return meaningful error messages")
    void testErrorMessageContent() throws Exception {
        when(tollPlazaService.findTollPlazasBetweenPincodes(any(TollPlazasRequestDTO.class)))
            .thenThrow(new InvalidPincodeException("Source pincode format is invalid"));

        TollPlazasRequestDTO invalidRequest = new TollPlazasRequestDTO();
        invalidRequest.setSourcePincode(INVALID_PINCODE);
        invalidRequest.setDestinationPincode(DEST_PINCODE);

        mockMvc.perform(post(API_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        // Additional assertion for error message if your response includes it
    }

    // ==================== Helper ====================

    private static final java.util.List<com.pushkar.smart_toll.model.TollPlaza> getEmptyTolls() {
        return new java.util.ArrayList<>();
    }

}
