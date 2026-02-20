package com.rhaen.tracker.feature.tracking.query;

import com.rhaen.tracker.feature.tracking.dto.TrackingDtos;
import com.rhaen.tracker.feature.tracking.history.SessionHistoryService;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionEntity;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionRepository;
import com.rhaen.tracker.feature.user.persistence.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackingQueryServiceTest {

    @Mock
    private TrackingSessionRepository sessionRepository;
    @Mock
    private SessionHistoryService historyService;

    @Test
    void listSessions_appliesPaginationBounds_andMaps() {
        TrackingQueryService service = new TrackingQueryService(sessionRepository, historyService);
        UUID userId = UUID.randomUUID();
        TrackingSessionEntity s = TrackingSessionEntity.builder()
                .id(UUID.randomUUID())
                .user(UserEntity.builder().id(userId).username("u").email("e@e.com").passwordHash("x").role(UserEntity.Role.USER).build())
                .status(TrackingSessionEntity.Status.ACTIVE)
                .startTime(Instant.now())
                .build();
        when(sessionRepository.findByUser_IdOrderByStartTimeDesc(eq(userId), any())).thenReturn(new PageImpl<>(List.of(s)));

        var page = service.listSessions(userId, -1, 999);

        assertThat(page.getContent()).hasSize(1);
        verify(sessionRepository).findByUser_IdOrderByStartTimeDesc(eq(userId), argThat(p -> p.getPageNumber() == 0 && p.getPageSize() == 100));
    }

    @Test
    void getSessionPoints_delegatesToHistoryService() {
        TrackingQueryService service = new TrackingQueryService(sessionRepository, historyService);
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TrackingDtos.PointsResponse expected = new TrackingDtos.PointsResponse(List.of(), false, 0);
        when(historyService.getSessionPointsForUser(eq(sessionId), eq(userId), eq(false), any(), any(), eq(10), eq(true), eq(5.0)))
                .thenReturn(expected);

        var out = service.getSessionPoints(sessionId, userId, false, null, null, 10, true, 5.0);

        assertThat(out).isSameAs(expected);
    }
}
