package com.rhaen.tracker.feature.tracking.history;

import com.rhaen.tracker.common.exception.BadRequestException;
import com.rhaen.tracker.common.exception.NotFoundException;
import com.rhaen.tracker.feature.admin.api.dto.AdminDtos;
import com.rhaen.tracker.feature.tracking.persistence.TrackingPointEntity;
import com.rhaen.tracker.feature.tracking.persistence.TrackingPointRepository;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionRepository;
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

    public List<AdminDtos.PointRow> getSessionPoints(UUID sessionId, Instant from, Instant to, Integer maxPoints) {
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

        if (points.isEmpty()) return List.of();
        if (points.size() <= max) return map(points);

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

    private List<AdminDtos.PointRow> map(List<TrackingPointEntity> points) {
        return points.stream().map(p -> {
            Point g = p.getPoint();
            double lon = g.getX();
            double lat = g.getY();
            return new AdminDtos.PointRow(
                    p.getDeviceTimestamp(),
                    lat,
                    lon,
                    p.getAccuracyM(),
                    p.getSpeedMps(),
                    p.getHeadingDeg()
            );
        }).toList();
    }
}
