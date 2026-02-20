package com.rhaen.tracker.feature.admin.api;

import com.rhaen.tracker.feature.admin.query.AdminQueryService;
import com.rhaen.tracker.feature.tracking.persistence.TrackingSessionEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminQueryService service;

    @Test
    void delegates_allEndpoints() {
        AdminController controller = new AdminController(service);
        when(service.listUsers()).thenReturn(List.of());
        when(service.listLastLocations()).thenReturn(List.of());
        when(service.getSessionPoints(any(), any(), any(), any())).thenReturn(List.of());
        when(service.getSessionSummary(any())).thenReturn(null);
        when(service.listSessions(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(null);
        when(service.listAuditLogs(anyInt(), anyInt())).thenReturn(new PageImpl<>(List.of()));

        assertThat(controller.users().message()).isEqualTo("OK");
        assertThat(controller.lastLocations().message()).isEqualTo("OK");
        assertThat(controller.sessionPoints(java.util.UUID.randomUUID(), null, null, null).message()).isEqualTo("OK");
        assertThat(controller.sessionSummary(java.util.UUID.randomUUID()).message()).isEqualTo("OK");
        assertThat(controller.sessions(null, TrackingSessionEntity.Status.ACTIVE, null, null, 0, 20).message()).isEqualTo("OK");
        assertThat(controller.auditLogs(0, 20).message()).isEqualTo("OK");
    }
}
