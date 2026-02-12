package com.pushkar.smart_toll.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MappslRouteResponse {
    @JsonProperty("results")
    private RouteResult results;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RouteResult {
        @JsonProperty("distance")
        private Double distance; // in meters

        @JsonProperty("duration")
        private Double duration; // in seconds

        @JsonProperty("routes")
        private List<Route> routes;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Route {
        @JsonProperty("distance")
        private Double distance;

        @JsonProperty("duration")
        private Double duration;

        @JsonProperty("way_points")
        private List<WayPoint> wayPoints;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WayPoint {
        @JsonProperty("location")
        private Location location;

        @JsonProperty("distance")
        private Double distance;
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
