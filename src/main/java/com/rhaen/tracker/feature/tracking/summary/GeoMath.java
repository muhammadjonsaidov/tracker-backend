package com.rhaen.tracker.feature.tracking.summary;

public final class GeoMath {

    private static final double R = 6371000.0; // meters

    private GeoMath() {}

    public static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1);
        double p2 = Math.toRadians(lat2);
        double dp = Math.toRadians(lat2 - lat1);
        double dl = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dp / 2) * Math.sin(dp / 2)
                + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /** Equirectangular projection (approx) for RDP distance computations */
    public static XY project(double lat, double lon, double lat0) {
        double x = Math.toRadians(lon) * Math.cos(Math.toRadians(lat0)) * R;
        double y = Math.toRadians(lat) * R;
        return new XY(x, y);
    }

    public record XY(double x, double y) {}
}
