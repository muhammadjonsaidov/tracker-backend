package com.rhaen.tracker.feature.tracking.summary;

import java.util.List;

public final class PolylineEncoder {

    private PolylineEncoder() {}

    public record LatLon(double lat, double lon) {}

    public static String encode(List<LatLon> points) {
        long lastLat = 0;
        long lastLon = 0;
        StringBuilder result = new StringBuilder(points.size() * 8);

        for (LatLon p : points) {
            long lat = Math.round(p.lat() * 1e5);
            long lon = Math.round(p.lon() * 1e5);

            long dLat = lat - lastLat;
            long dLon = lon - lastLon;

            encodeSigned(dLat, result);
            encodeSigned(dLon, result);

            lastLat = lat;
            lastLon = lon;
        }
        return result.toString();
    }

    private static void encodeSigned(long value, StringBuilder sb) {
        long s = value << 1;
        if (value < 0) s = ~s;
        encodeUnsigned(s, sb);
    }

    private static void encodeUnsigned(long value, StringBuilder sb) {
        while (value >= 0x20) {
            long nextValue = (0x20 | (value & 0x1f)) + 63;
            sb.append((char) nextValue);
            value >>= 5;
        }
        sb.append((char) (value + 63));
    }
}
