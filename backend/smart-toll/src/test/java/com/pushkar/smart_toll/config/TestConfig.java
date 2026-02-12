package com.pushkar.smart_toll.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.mockito.Mockito;

/**
 * Test Configuration for Spring Boot Tests
 * Provides test beans and configurations for the test suite
 */
@TestConfiguration
public class TestConfig {

    /**
     * Provides test data or mock configurations if needed
     */
    @Bean
    public TestDataProvider testDataProvider() {
        return new TestDataProvider();
    }

    /**
     * Helper class for providing test data
     */
    public static class TestDataProvider {
        
        public static final String TEST_PINCODE_KHARGHAR = "410210";
        public static final String TEST_PINCODE_ALIBAG = "402209";
        public static final String TEST_PINCODE_BANGALORE = "560064";
        public static final String TEST_PINCODE_PUNE = "411001";
        
        public static final double KHARGHAR_LAT = 19.0544;
        public static final double KHARGHAR_LNG = 73.0362;
        
        public static final double ALIBAG_LAT = 18.9271;
        public static final double ALIBAG_LNG = 72.8294;
    }
}
