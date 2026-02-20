package com.rhaen.tracker.feature.tracking.summary;

import com.rhaen.tracker.feature.tracking.persistence.*;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionSummaryService {

    private final TrackingPointRepository pointRepository;
    private final SessionSummaryRepository summaryRepository;
    private final TrackingSummaryProperties props;

    @Transactional
    public void buildOrRebuild(TrackingSessionEntity session) {
        UUID sessionId = session.getId();

        List<TrackingPointEntity> points = pointRepository.findBySessionIdOrderByDeviceTimestampAsc(sessionId);

        SessionSummaryEntity summary = summaryRepository.findById(sessionId)
                .orElseGet(() -> SessionSummaryEntity.builder()
                        .session(session)
                        .build());

        if (points.isEmpty()) {
            summary.setPointsCount(0);
            summary.setDistanceM(0d);
            summary.setDurationS(0);
            summary.setAvgSpeedMps(0d);
            summary.setMaxSpeedMps(0d);
            summary.setPolyline("");
            summary.setSimplifiedPolyline("");
            summary.setUpdatedAt(Instant.now());
            summaryRepository.save(summary);
            return;
        }

        // Convert to LatLon list
        List<PolylineEncoder.LatLon> latlons = new ArrayList<>(points.size());
        double minLat =  90, minLon =  180;
        double maxLat = -90, maxLon = -180;

        Double maxSpeed = null;

        for (TrackingPointEntity p : points) {
            Point g = p.getPoint();
            double lon = g.getX();
            double lat = g.getY();
            latlons.add(new PolylineEncoder.LatLon(lat, lon));

            if (lat < minLat) minLat = lat;
            if (lon < minLon) minLon = lon;
            if (lat > maxLat) maxLat = lat;
            if (lon > maxLon) maxLon = lon;

            if (p.getSpeedMps() != null) {
                maxSpeed = (maxSpeed == null) ? (double) p.getSpeedMps() : Math.max(maxSpeed, p.getSpeedMps());
            }
        }

        // Distance (haversine sum)
        double distance = 0;
        for (int i = 1; i < latlons.size(); i++) {
            var a = latlons.get(i - 1);
            var b = latlons.get(i);
            distance += GeoMath.haversineMeters(a.lat(), a.lon(), b.lat(), b.lon());
        }

        Instant start = session.getStartTime();
        Instant stop = session.getStopTime() != null ? session.getStopTime() : Instant.now();
        int durationS = (int) Math.max(0, ChronoUnit.SECONDS.between(start, stop));

        double avgSpeed = durationS > 0 ? distance / durationS : 0d;

        // If speed_mps wasn't provided, estimate max speed from segments
        if (maxSpeed == null) {
            double estimatedMax = 0d;
            for (int i = 1; i < points.size(); i++) {
                Instant t1 = points.get(i - 1).getDeviceTimestamp();
                Instant t2 = points.get(i).getDeviceTimestamp();
                long dt = Math.max(1, ChronoUnit.SECONDS.between(t1, t2));
                var a = latlons.get(i - 1);
                var b = latlons.get(i);
                double d = GeoMath.haversineMeters(a.lat(), a.lon(), b.lat(), b.lon());
                estimatedMax = Math.max(estimatedMax, d / dt);
            }
            maxSpeed = estimatedMax;
        }

        // Downsample for full polyline if too many points
        List<PolylineEncoder.LatLon> polylinePts = downsample(latlons, props.maxPolylinePoints());

        String polyline = PolylineEncoder.encode(polylinePts);

        // Simplified polyline (RDP) -> encode
        List<PolylineEncoder.LatLon> simplified = RdpSimplifier.simplify(latlons, props.simplifyEpsilonM());
        String simplifiedPolyline = PolylineEncoder.encode(simplified);

        // Start/end points
        Point startPoint = points.getFirst().getPoint();
        Point endPoint = points.getLast().getPoint();

        summary.setPointsCount(points.size());
        summary.setDistanceM(distance);
        summary.setDurationS(durationS);
        summary.setAvgSpeedMps(avgSpeed);
        summary.setMaxSpeedMps(maxSpeed);

        summary.setStartPoint(startPoint);
        summary.setEndPoint(endPoint);

        summary.setBboxMinLat(minLat);
        summary.setBboxMinLon(minLon);
        summary.setBboxMaxLat(maxLat);
        summary.setBboxMaxLon(maxLon);

        summary.setPolyline(polyline);
        summary.setSimplifiedPolyline(simplifiedPolyline);

        summary.setUpdatedAt(Instant.now());

        summaryRepository.save(summary);
    }

    private List<PolylineEncoder.LatLon> downsample(List<PolylineEncoder.LatLon> pts, int max) {
        if (max <= 0 || pts.size() <= max) return pts;
        int n = pts.size();
        int step = (int) Math.ceil(n / (double) max);
        List<PolylineEncoder.LatLon> out = new ArrayList<>(max + 2);
        for (int i = 0; i < n; i += step) out.add(pts.get(i));
        if (!out.getLast().equals(pts.get(n - 1))) out.add(pts.get(n - 1));
        return out;
    }
}
