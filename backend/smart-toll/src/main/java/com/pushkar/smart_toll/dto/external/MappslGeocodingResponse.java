package com.pushkar.smart_toll.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MappslGeocodingResponse {
    @JsonProperty("results")
    private GeocodingResult[] results;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeocodingResult {
        @JsonProperty("formatted_address")
        private String formattedAddress;

        @JsonProperty("geometry")
        private Geometry geometry;

        @JsonProperty("place_id")
        private String placeId;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Geometry {
        @JsonProperty("location")
        private Location location;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        @JsonProperty("lat")
        private Double latitude;

        @JsonProperty("lng")
        private Double longitude;
    }
}
