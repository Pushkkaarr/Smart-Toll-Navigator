package com.pushkar.smart_toll.util;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Production-grade geolocation utilities using actual route geometry
 * Matches toll plazas to route polylines instead of geometric approximation
 */
@Slf4j
public class GeoLocationUtilV2 {
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double ROUTE_TOLERANCE_KM = 5.0; // Tolls within 5km of actual route

    /**
     * Calculate great-circle distance between two points using Haversine formula
     * @param lat1 Point 1 latitude
     * @param lng1 Point 1 longitude
     * @param lat2 Point 2 latitude
     * @param lng2 Point 2 longitude
     * @return Distance in kilometers
     */
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Check if a toll plaza is near the actual route
     * Uses polyline geometry to accurately determine if a point falls on/near the route
     * 
     * @param polylineCoordinates List of [lat, lng] pairs from route polyline
     * @param tollLat Toll plaza latitude
     * @param tollLng Toll plaza longitude
     * @return true if toll is within ROUTE_TOLERANCE_KM of any segment
     */
    public static boolean isTollNearRoute(List<List<Double>> polylineCoordinates, 
                                         double tollLat, double tollLng) {
        if (polylineCoordinates == null || polylineCoordinates.isEmpty()) {
            log.warn("Empty polyline coordinates provided");
            return false;
        }

        // Check distance from toll to every segment of the route
        for (int i = 0; i < polylineCoordinates.size() - 1; i++) {
            List<Double> point1 = polylineCoordinates.get(i);
            List<Double> point2 = polylineCoordinates.get(i + 1);

            double distanceToSegment = distancePointToLineSegment(
                tollLat, tollLng,
                point1.get(0), point1.get(1),
                point2.get(0), point2.get(1)
            );

            if (distanceToSegment <= ROUTE_TOLERANCE_KM) {
                log.debug("Toll at ({}, {}) is {} km from route", tollLat, tollLng, distanceToSegment);
                return true;
            }
        }

        return false;
    }

    /**
     * Calculate perpendicular distance from a point to a line segment
     * Uses projection math to find the closest point on the segment
     * 
     * @param pointLat Point latitude
     * @param pointLng Point longitude
     * @param segLat1 Segment start latitude
     * @param segLng1 Segment start longitude
     * @param segLat2 Segment end latitude
     * @param segLng2 Segment end longitude
     * @return Distance in kilometers
     */
    private static double distancePointToLineSegment(
            double pointLat, double pointLng,
            double segLat1, double segLng1,
            double segLat2, double segLng2) {

        // Convert to radians
        double lat1 = Math.toRadians(segLat1);
        double lng1 = Math.toRadians(segLng1);
        double lat2 = Math.toRadians(segLat2);
        double lng2 = Math.toRadians(segLng2);
        double pLat = Math.toRadians(pointLat);
        double pLng = Math.toRadians(pointLng);

        // Calculate distances
        double d13 = 2 * Math.asin(Math.sqrt(
            Math.pow(Math.sin((lat1 - pLat) / 2), 2) +
            Math.cos(lat1) * Math.cos(pLat) * Math.pow(Math.sin((lng1 - pLng) / 2), 2)
        ));

        double d23 = 2 * Math.asin(Math.sqrt(
            Math.pow(Math.sin((lat2 - pLat) / 2), 2) +
            Math.cos(lat2) * Math.cos(pLat) * Math.pow(Math.sin((lng2 - pLng) / 2), 2)
        ));

        double d12 = 2 * Math.asin(Math.sqrt(
            Math.pow(Math.sin((lat1 - lat2) / 2), 2) +
            Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin((lng1 - lng2) / 2), 2)
        ));

        // If point is beyond segment endpoints, return distance to nearest endpoint
        if (d13 + d23 > d12 + 0.0001) {
            return Math.min(d13, d23) * EARTH_RADIUS_KM;
        }

        // Cross-track distance
        double dxt = Math.asin(Math.sin(d13 / EARTH_RADIUS_KM) * 
                              Math.sin(calculateBearing(segLat1, segLng1, pLat, pLng) - 
                                      calculateBearing(segLat1, segLng1, segLat2, segLng2)));

        return Math.abs(dxt) * EARTH_RADIUS_KM;
    }

    /**
     * Calculate bearing between two points
     */
    private static double calculateBearing(double lat1, double lng1, double lat2, double lng2) {
        double dLng = lng2 - lng1;
        double y = Math.sin(dLng) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng);
        return Math.atan2(y, x);
    }

    /**
     * Calculate distance from toll to start of route
     */
    public static double distanceFromRouteStart(
            List<List<Double>> polylineCoordinates,
            double tollLat, double tollLng) {

        if (polylineCoordinates == null || polylineCoordinates.isEmpty()) {
            return Double.MAX_VALUE;
        }

        // Find distance along the route
        double cumulativeDistance = 0;
        List<Double> startPoint = polylineCoordinates.get(0);

        for (int i = 0; i < polylineCoordinates.size(); i++) {
            List<Double> currentPoint = polylineCoordinates.get(i);
            
            // Check distance to this segment
            if (i < polylineCoordinates.size() - 1) {
                List<Double> nextPoint = polylineCoordinates.get(i + 1);
                double segmentDistance = distancePointToLineSegment(
                    tollLat, tollLng,
                    currentPoint.get(0), currentPoint.get(1),
                    nextPoint.get(0), nextPoint.get(1)
                );

                // If toll is closest to this segment
                if (segmentDistance <= ROUTE_TOLERANCE_KM) {
                    double distToSegStart = calculateDistance(
                        startPoint.get(0), startPoint.get(1),
                        currentPoint.get(0), currentPoint.get(1)
                    );
                    double remainingOnSegment = calculateDistance(
                        currentPoint.get(0), currentPoint.get(1),
                        nextPoint.get(0), nextPoint.get(1)
                    );

                    // Approximate position on segment
                    return cumulativeDistance + (remainingOnSegment * 0.5);
                }
            }

            // Add distance to next point
            if (i < polylineCoordinates.size() - 1) {
                List<Double> nextPoint = polylineCoordinates.get(i + 1);
                cumulativeDistance += calculateDistance(
                    currentPoint.get(0), currentPoint.get(1),
                    nextPoint.get(0), nextPoint.get(1)
                );
            }
        }

        return Double.MAX_VALUE;
    }
}
