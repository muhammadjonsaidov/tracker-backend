package com.rhaen.tracker.feature.tracking.ingest;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TrackingPointIngestRepository {

    private final JdbcTemplate jdbc;

    public int insertBatch(UUID sessionId, Instant receivedAt, List<IngestPointRow> rows) {
        if (rows.isEmpty()) return 0;

        String sql = """
            INSERT INTO tracking_points
              (session_id, event_id, device_timestamp, received_at, point, accuracy_m, speed_mps, heading_deg, provider, is_mock)
            VALUES
              (?, ?, ?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, ?, ?, ?, ?, ?)
            ON CONFLICT (session_id, event_id) DO NOTHING
            """;

        int[][] counts = jdbc.batchUpdate(sql, rows, 500, (PreparedStatement ps, IngestPointRow r) -> {
            ps.setObject(1, sessionId);
            ps.setObject(2, r.eventId());
            OffsetDateTime deviceTs = r.deviceTimestamp().atOffset(ZoneOffset.UTC);
            OffsetDateTime receivedTs = receivedAt.atOffset(ZoneOffset.UTC);
            ps.setObject(3, deviceTs);
            ps.setObject(4, receivedTs);

            ps.setDouble(5, r.lon());
            ps.setDouble(6, r.lat());

            if (r.accuracyM() == null) ps.setNull(7, java.sql.Types.REAL); else ps.setFloat(7, r.accuracyM());
            if (r.speedMps() == null) ps.setNull(8, java.sql.Types.REAL); else ps.setFloat(8, r.speedMps());
            if (r.headingDeg() == null) ps.setNull(9, java.sql.Types.REAL); else ps.setFloat(9, r.headingDeg());

            ps.setString(10, r.provider());
            ps.setBoolean(11, r.isMock());
        });

        int inserted = 0;
        for (int[] batch : counts) {
            for (int c : batch) {
                if (c > 0) inserted += c; // conflict bo‘lsa 0
            }
        }
        return inserted;
    }

    public record IngestPointRow(
            UUID eventId,
            double lat,
            double lon,
            Instant deviceTimestamp,
            Float accuracyM,
            Float speedMps,
            Float headingDeg,
            String provider,
            boolean isMock
    ) {}
}
