package com.homoSSAFYens.homSSAFYens.utill;

public class GeoUtil {
    private static final double EARTH_RADIUS_KM = 6371.0088;

    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2-lat1);
        double dLon = Math.toRadians(lon2-lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2)*Math.sin(dLon/2);
        double c = 2*Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return EARTH_RADIUS_KM * c;
    }

    // 시연용 간소화: “경로상의 점들”까지의 최소 거리만 사용
    public static double minDistanceToPointsKm(double lat, double lon, double[][] path) {
        double min = Double.MAX_VALUE;
        for (double[] p : path) {
            double d = haversineKm(lat, lon, p[0], p[1]);
            if (d < min) min = d;
        }
        return min;
    }
}
