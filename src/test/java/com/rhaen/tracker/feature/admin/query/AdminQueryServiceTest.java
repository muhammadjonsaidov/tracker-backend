package com.rhaen.tracker.feature.admin.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhaen.tracker.common.exception.NotFoundException;
import com.rhaen.tracker.feature.admin.dto.AdminDtos;
import com.rhaen.tracker.feature.admin.persistence.AdminAuditLogEntity;
import com.rhaen.tracker.feature.admin.persistence.AdminAuditLogRepository;
import com.rhaen.tracker.feature.tracking.dto.TrackingDtos;
import com.rhaen.tracker.feature.tracking.history.SessionHistoryService;
import com.rhaen.tracker.feature.tracking.persistence.SessionSummaryEntity;
import com.rhaen.tracker.feature.tracking.persistence.SessionSummaryRepository;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionEntity;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionRepository;
import com.rhaen.tracker.feature.tracking.realtime.LastLocationCache;
import com.rhaen.tracker.feature.tracking.realtime.LastLocationSnapshot;
import com.rhaen.tracker.feature.user.persistence.UserEntity;
import com.rhaen.tracker.feature.user.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.net.InetAddress;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminQueryServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TrackingSessionRepository sessionRepository;
    @Mock
    private SessionSummaryRepository summaryRepository;
    @Mock
    private SessionHistoryService historyService;
    @Mock
    private LastLocationCache lastLocationCache;
    @Mock
    private AdminAuditLogRepository auditRepository;

    private AdminQueryService service;

    @BeforeEach
    void setUp() {
        service = new AdminQueryService(userRepository, sessionRepository, summaryRepository, historyService,
                lastLocationCache, auditRepository);
    }

    @Test
    void listUsers_mapsFields() {
        UserEntity u = UserEntity.builder()
                .id(UUID.randomUUID())
                .username("john")
                .email("j@e.com")
                .passwordHash("x")
                .role(UserEntity.Role.ADMIN)
                .createdAt(Instant.now())
                .build();
        when(userRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(u)));

        AdminDtos.UserPage rows = service.listUsers(0, 10);

        assertThat(rows.items()).hasSize(1);
        assertThat(rows.items().getFirst().role()).isEqualTo("ADMIN");
    }

    @Test
    void listLastLocations_sortsActiveFirst_thenTsDesc() {
        LastLocationSnapshot activeOld = new LastLocationSnapshot(UUID.randomUUID(), UUID.randomUUID(), "ACTIVE", true,
                41, 69, Instant.now().minusSeconds(60), null, null, null);
        LastLocationSnapshot stoppedNew = new LastLocationSnapshot(UUID.randomUUID(), UUID.randomUUID(), "STOPPED",
                false, 41, 69, Instant.now(), null, null, null);
        LastLocationSnapshot activeNew = new LastLocationSnapshot(UUID.randomUUID(), UUID.randomUUID(), "ACTIVE", true,
                41, 69, Instant.now(), null, null, null);
        when(lastLocationCache.getAll()).thenReturn(List.of(activeOld, stoppedNew, activeNew));
        when(lastLocationCache.isStale(any())).thenReturn(false);

        List<AdminDtos.LastLocationRow> rows = service.listLastLocations();

        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).active()).isTrue();
        assertThat(rows.get(1).active()).isTrue();
        assertThat(rows.get(2).active()).isFalse();
        assertThat(rows.get(0).ts()).isAfterOrEqualTo(rows.get(1).ts());
    }

    @Test
    void listSessions_mapsPage() {
        UUID userId = UUID.randomUUID();
        TrackingSessionEntity session = TrackingSessionEntity.builder()
                .id(UUID.randomUUID())
                .user(UserEntity.builder().id(userId).username("u").email("e@e.com").passwordHash("x")
                        .role(UserEntity.Role.USER).build())
                .status(TrackingSessionEntity.Status.ACTIVE)
                .startTime(Instant.now())
                .build();

        when(sessionRepository.search(eq(userId), eq(TrackingSessionEntity.Status.ACTIVE), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(session)));

        AdminDtos.SessionPage page = service.listSessions(userId, TrackingSessionEntity.Status.ACTIVE, null, null, 0,
                20);

        assertThat(page.items()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void getSessionSummary_throwsWhenMissing() {
        UUID sessionId = UUID.randomUUID();
        when(summaryRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSessionSummary(sessionId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getSessionSummary_mapsBbox() {
        UUID sessionId = UUID.randomUUID();
        SessionSummaryEntity s = SessionSummaryEntity.builder()
                .sessionId(sessionId)
                .polyline("p")
                .simplifiedPolyline("sp")
                .distanceM(10.0)
                .durationS(5)
                .avgSpeedMps(2.0)
                .maxSpeedMps(3.0)
                .pointsCount(2)
                .bboxMinLat(1.0)
                .bboxMinLon(2.0)
                .bboxMaxLat(3.0)
                .bboxMaxLon(4.0)
                .build();
        when(summaryRepository.findById(sessionId)).thenReturn(Optional.of(s));

        AdminDtos.SessionSummaryResponse out = service.getSessionSummary(sessionId);

        assertThat(out.bbox().minLat()).isEqualTo(1.0);
        assertThat(out.polyline()).isEqualTo("p");
    }

    @Test
    void getSessionPoints_delegates() {
        UUID sid = UUID.randomUUID();
        List<TrackingDtos.PointRow> rows = List
                .of(new TrackingDtos.PointRow(Instant.now(), 41.0, 69.0, null, null, null));
        when(historyService.getSessionPoints(eq(sid), any(), any(), any())).thenReturn(rows);

        assertThat(service.getSessionPoints(sid, null, null, 100)).hasSize(1);
    }

    @Test
    void listAuditLogs_mapsNullableFields() throws Exception {
        UserEntity admin = UserEntity.builder().id(UUID.randomUUID()).username("admin").email("a@a.com")
                .passwordHash("x").role(UserEntity.Role.ADMIN).build();
        AdminAuditLogEntity a1 = AdminAuditLogEntity.builder()
                .id(1L)
                .admin(admin)
                .action("LOGIN_SUCCESS")
                .targetType("USER")
                .targetId(UUID.randomUUID())
                .metadata(new ObjectMapper().readTree("{\"k\":1}"))
                .ipAddress(InetAddress.getByName("127.0.0.1"))
                .userAgent("ua")
                .createdAt(Instant.now())
                .build();
        AdminAuditLogEntity a2 = AdminAuditLogEntity.builder()
                .id(2L)
                .admin(null)
                .action("LOGIN_FAILED")
                .createdAt(Instant.now())
                .build();

        when(auditRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(new PageImpl<>(List.of(a1, a2)));

        var page = service.listAuditLogs(-1, 200);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().getFirst().metadata()).contains("\"k\":1");
        assertThat(page.getContent().get(1).adminId()).isNull();
        verify(auditRepository)
                .findAllByOrderByCreatedAtDesc(argThat(p -> p.getPageNumber() == 0 && p.getPageSize() == 100));
    }
}
