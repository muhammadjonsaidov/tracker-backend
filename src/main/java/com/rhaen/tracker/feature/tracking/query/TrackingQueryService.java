package com.rhaen.tracker.feature.tracking.query;

import com.rhaen.tracker.feature.tracking.dto.TrackingDtos;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrackingQueryService {

    private final TrackingSessionRepository sessionRepository;

    public List<TrackingDtos.SessionRow> listSessions(UUID userId) {
        return sessionRepository.findByUser_IdOrderByStartTimeDesc(userId).stream()
                .map(s -> new TrackingDtos.SessionRow(
                        s.getId(),
                        s.getStartTime(),
                        s.getStopTime(),
                        s.getStatus().name(),
                        s.getLastPointAt()
                ))
                .toList();
    }
}
