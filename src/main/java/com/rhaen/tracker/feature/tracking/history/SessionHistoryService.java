package com.rhaen.tracker.feature.tracking.history;

import com.rhaen.tracker.common.exception.BadRequestException;
import com.rhaen.tracker.common.exception.ForbiddenException;
import com.rhaen.tracker.common.exception.NotFoundException;
import com.rhaen.tracker.feature.tracking.dto.TrackingDtos;
import com.rhaen.tracker.feature.tracking.persistence.TrackingPointEntity;
import com.rhaen.tracker.feature.tracking.persistence.TrackingPointRepository;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionEntity;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionRepository;
import com.rhaen.tracker.feature.tracking.summary.GeoMath;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionHistoryService {

    private final TrackingSessionRepository sessionRepository;
    private final TrackingPointRepository pointRepository;
    private final TrackingHistoryProperties props;

    public List<TrackingDtos.PointRow> getSessionPoints(UUID sessionId, Instant from, Instant to, Integer maxPoints) {
        // session mavjudligini tekshir
        sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));

        int max = (maxPoints == null || maxPoints <= 0) ? props.defaultMaxPoints() : maxPoints;

        // window yo‘q bo‘lsa umumiy count
        long count = (from != null && to != null)
                ? pointRepository.countBySessionIdAndDeviceTimestampBetween(sessionId, from, to)
                : pointRepository.countBySessionId(sessionId);

        if (count > props.hardLimitPoints()) {
            throw new BadRequestException("Too many points (" + count + "). Use /summary polyline instead.");
        }

        List<TrackingPointEntity> points = (from != null && to != null)
                ? pointRepository.findBySessionIdAndDeviceTimestampBetweenOrderByDeviceTimestampAsc(sessionId, from, to)
                : pointRepository.findBySessionIdOrderByDeviceTimestampAsc(sessionId);

        if (points.isEmpty())
            return List.of();
        if (points.size() <= max)
            return map(points);

        // downsample
        int step = (int) Math.ceil(points.size() / (double) max);
        List<TrackingPointEntity> sampled = new ArrayList<>(max + 2);

        for (int i = 0; i < points.size(); i += step) {
            sampled.add(points.get(i));
        }
        if (sampled.getLast() != points.getLast()) {
            sampled.add(points.getLast());
        }

        return map(sampled);
    }

    public TrackingDtos.PointsResponse getSessionPointsForUser(UUID sessionId,
            UUID userId,
            boolean isAdmin,
            Instant from,
            Instant to,
            Integer maxPoints,
            boolean downsample,
            double simplifyEpsM) {
        TrackingSessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));

        if (!isAdmin && !session.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Session does not belong to user");
        }

        int max = resolveMax(maxPoints);
        long total = countPointsByRange(sessionId, from, to);

        if (total > props.hardLimitPoints()) {
            throw new BadRequestException("Too many points (" + total + "). Use /summary polyline instead.");
        }

        List<TrackingPointEntity> points = findPointsByRange(sessionId, from, to);
        if (points.isEmpty()) {
            return new TrackingDtos.PointsResponse(List.of(), false, total);
        }

        List<TrackingPointEntity> selected = points;
        if (downsample) {
            if (simplifyEpsM > 0) {
                selected = simplifyRdp(points, simplifyEpsM);
            }
            if (selected.size() > max) {
                selected = downsampleByStep(selected, max);
            }
        } else if (selected.size() > max) {
            selected = selected.subList(0, max);
        }

        boolean truncated = total > max;
        return new TrackingDtos.PointsResponse(map(selected), truncated, total);
    }

    private long countPointsByRange(UUID sessionId, Instant from, Instant to) {
        if (from == null && to == null) {
            return pointRepository.countBySessionId(sessionId);
        }
        if (from != null && to != null) {
            return pointRepository.countBySessionIdAndDeviceTimestampBetween(sessionId, from, to);
        }
        if (from != null) {
            return pointRepository.countBySessionIdAndDeviceTimestampGreaterThanEqual(sessionId, from);
        }
        return pointRepository.countBySessionIdAndDeviceTimestampLessThanEqual(sessionId, to);
    }

    private List<TrackingPointEntity> findPointsByRange(UUID sessionId, Instant from, Instant to) {
        if (from == null && to == null) {
            return pointRepository.findBySessionIdOrderByDeviceTimestampAsc(sessionId);
        }
        if (from != null && to != null) {
            return pointRepository.findBySessionIdAndDeviceTimestampBetweenOrderByDeviceTimestampAsc(sessionId, from,
                    to);
        }
        if (from != null) {
            return pointRepository.findBySessionIdAndDeviceTimestampGreaterThanEqualOrderByDeviceTimestampAsc(sessionId,
                    from);
        }
        return pointRepository.findBySessionIdAndDeviceTimestampLessThanEqualOrderByDeviceTimestampAsc(sessionId, to);
    }

    private List<TrackingDtos.PointRow> map(List<TrackingPointEntity> points) {
        return points.stream().map(p -> {
            Point g = p.getPoint();
            double lon = g.getX();
            double lat = g.getY();
            return new TrackingDtos.PointRow(
                    p.getDeviceTimestamp(),
                    lat,
                    lon,
                    p.getAccuracyM(),
                    p.getSpeedMps(),
                    p.getHeadingDeg());
        }).toList();
    }

    private int resolveMax(Integer maxPoints) {
        int max = (maxPoints == null || maxPoints <= 0) ? props.defaultMaxPoints() : maxPoints;
        return Math.min(max, props.hardLimitPoints());
    }

    private List<TrackingPointEntity> downsampleByStep(List<TrackingPointEntity> points, int max) {
        if (max <= 0 || points.size() <= max)
            return points;
        int step = (int) Math.ceil(points.size() / (double) max);
        List<TrackingPointEntity> out = new ArrayList<>(max + 2);
        for (int i = 0; i < points.size(); i += step) {
            out.add(points.get(i));
        }
        if (out.getLast() != points.getLast()) {
            out.add(points.getLast());
        }
        return out;
    }

    private List<TrackingPointEntity> simplifyRdp(List<TrackingPointEntity> points, double epsilonMeters) {
        if (points == null || points.size() < 3)
            return points;

        int n = points.size();
        double lat0 = points.getFirst().getPoint().getY();

        boolean[] keep = new boolean[n];
        keep[0] = true;
        keep[n - 1] = true;

        rdp(points, 0, n - 1, epsilonMeters, keep, lat0);

        List<TrackingPointEntity> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (keep[i])
                out.add(points.get(i));
        }
        return out;
    }

    private void rdp(List<TrackingPointEntity> points, int start, int end, double eps, boolean[] keep, double lat0) {
        if (end <= start + 1)
            return;

        double maxDist = -1;
        int index = -1;

        var aPoint = points.get(start).getPoint();
        var bPoint = points.get(end).getPoint();
        var a = GeoMath.project(aPoint.getY(), aPoint.getX(), lat0);
        var b = GeoMath.project(bPoint.getY(), bPoint.getX(), lat0);

        for (int i = start + 1; i < end; i++) {
            var pPoint = points.get(i).getPoint();
            var p = GeoMath.project(pPoint.getY(), pPoint.getX(), lat0);
            double d = perpendicularDistance(p.x(), p.y(), a.x(), a.y(), b.x(), b.y());
            if (d > maxDist) {
                maxDist = d;
                index = i;
            }
        }

        if (maxDist > eps && index != -1) {
            keep[index] = true;
            rdp(points, start, index, eps, keep, lat0);
            rdp(points, index, end, eps, keep, lat0);
        }
    }

    private double perpendicularDistance(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        if (dx == 0 && dy == 0) {
            double ex = px - ax;
            double ey = py - ay;
            return Math.sqrt(ex * ex + ey * ey);
        }
        double t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        double cx = ax + t * dx;
        double cy = ay + t * dy;
        double ex = px - cx;
        double ey = py - cy;
        return Math.sqrt(ex * ex + ey * ey);
    }
}
