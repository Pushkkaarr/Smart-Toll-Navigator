package com.pushkar.smart_toll.util;

public class GeoLocationUtil {
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculate the distance between two geographic points using Haversine formula
     * @param lat1 Latitude of point 1
     * @param lng1 Longitude of point 1
     * @param lat2 Latitude of point 2
     * @param lng2 Longitude of point 2
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
     * Check if a point is approximately on the route between two points
     * Uses a tolerance of 50km from the direct route
     */
    public static boolean isPointNearRoute(
            double sourceLat, double sourceLng,
            double destLat, double destLng,
            double pointLat, double pointLng) {
        
        // Distance from point to source
        double distToSource = calculateDistance(sourceLat, sourceLng, pointLat, pointLng);
        // Distance from point to destination
        double distToDest = calculateDistance(pointLat, pointLng, destLat, destLng);
        // Direct distance from source to destination
        double directDist = calculateDistance(sourceLat, sourceLng, destLat, destLng);
        
        // Tolerance: 15% of direct distance or 50km, whichever is greater
        double tolerance = Math.max(directDist * 0.15, 50.0);
        
        // Point is on route if: distToSource + distToDest ≈ directDist (within tolerance)
        return Math.abs((distToSource + distToDest) - directDist) <= tolerance;
    }

    /**
     * Get bounding box coordinates for a route
     * Adds a buffer zone around the direct path
     */
    public static BoundingBox getBoundingBox(
            double sourceLat, double sourceLng,
            double destLat, double destLng) {
        
        double minLat = Math.min(sourceLat, destLat) - 1.0; // ~111km buffer
        double maxLat = Math.max(sourceLat, destLat) + 1.0;
        double minLng = Math.min(sourceLng, destLng) - 1.0;
        double maxLng = Math.max(sourceLng, destLng) + 1.0;
        
        return new BoundingBox(minLat, maxLat, minLng, maxLng);
    }

    public static class BoundingBox {
        public final double minLat;
        public final double maxLat;
        public final double minLng;
        public final double maxLng;

        public BoundingBox(double minLat, double maxLat, double minLng, double maxLng) {
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLng = minLng;
            this.maxLng = maxLng;
        }
    }
}
