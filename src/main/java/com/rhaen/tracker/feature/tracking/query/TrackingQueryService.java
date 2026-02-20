package com.rhaen.tracker.feature.tracking.query;

import com.rhaen.tracker.feature.tracking.dto.TrackingDtos;
import com.rhaen.tracker.feature.tracking.history.SessionHistoryService;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrackingQueryService {

    private final TrackingSessionRepository sessionRepository;
    private final SessionHistoryService sessionHistoryService;

    public Page<TrackingDtos.SessionRow> listSessions(UUID userId, int page, int size) {
        int pageNumber = Math.max(0, page);
        int pageSize = Math.min(100, Math.max(1, size));
        var pageable = PageRequest.of(pageNumber, pageSize);

        return sessionRepository.findByUser_IdOrderByStartTimeDesc(userId, pageable)
                .map(s -> new TrackingDtos.SessionRow(
                        s.getId(),
                        s.getStartTime(),
                        s.getStopTime(),
                        s.getStatus().name(),
                        s.getLastPointAt()
                ));
    }

    public TrackingDtos.PointsResponse getSessionPoints(UUID sessionId,
                                                        UUID userId,
                                                        boolean isAdmin,
                                                        Instant from,
                                                        Instant to,
                                                        Integer max,
                                                        boolean downsample,
                                                        double simplifyEpsM) {
        return sessionHistoryService.getSessionPointsForUser(
                sessionId,
                userId,
                isAdmin,
                from,
                to,
                max,
                downsample,
                simplifyEpsM
        );
    }
}
