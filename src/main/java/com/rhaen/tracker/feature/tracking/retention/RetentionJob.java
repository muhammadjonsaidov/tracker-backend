package com.rhaen.tracker.feature.tracking.retention;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class RetentionJob {

    private final JdbcTemplate jdbc;
    private final RetentionProperties props;
    private final MeterRegistry meterRegistry;

    private Counter archivedSessions;
    private Counter prunedPoints;

    @PostConstruct
    void init() {
        archivedSessions = meterRegistry.counter("tracker.retention.sessions.archived.total");
        prunedPoints = meterRegistry.counter("tracker.retention.points.pruned.total");

        if (props.runAtStartup()) {
            // startup’da bir marta ishlatish (test/qa)
            runOnce();
        }
    }

    @Scheduled(fixedDelayString = "${app.tracking.retention.sweep-interval-seconds:86400}000")
    public void scheduled() {
        runOnce();
    }

    @Transactional
    public void runOnce() {
        Instant now = Instant.now();

        Instant archiveCutoff = now.minus(props.archiveAfterDays(), ChronoUnit.DAYS);
        Instant pruneCutoff   = now.minus(props.prunePointsAfterDays(), ChronoUnit.DAYS);

        // 1) sessions: STOPPED/EXPIRED -> ARCHIVED (batch)
        int archived = archiveSessions(archiveCutoff, props.batchSessions());
        if (archived > 0) archivedSessions.increment(archived);

        // 2) mark summaries as pruned (only for old sessions)
        markSummariesPruned(pruneCutoff, props.batchSessions());

        // 3) delete raw points older than pruneCutoff (batch loop)
        int deleted;
        int totalDeleted = 0;
        do {
            deleted = prunePointsBatch(pruneCutoff, props.batchPoints());
            totalDeleted += deleted;
        } while (deleted > 0);

        if (totalDeleted > 0) prunedPoints.increment(totalDeleted);
    }

    private int archiveSessions(Instant cutoff, int batch) {
        // Native update with LIMIT via subquery
        String sql = """
            UPDATE tracking_sessions
               SET status = 'ARCHIVED',
                   updated_at = now()
             WHERE id IN (
               SELECT id
                 FROM tracking_sessions
                WHERE status IN ('STOPPED','EXPIRED')
                  AND stop_time IS NOT NULL
                  AND stop_time < ?
                ORDER BY stop_time ASC
                LIMIT ?
             )
            """;
        OffsetDateTime cutoffTs = cutoff.atOffset(ZoneOffset.UTC);
        return jdbc.update(sql, ps -> {
            ps.setObject(1, cutoffTs);
            ps.setInt(2, batch);
        });
    }

    private int markSummariesPruned(Instant cutoff, int batch) {
        // Only mark summaries for sessions fully older than cutoff
        String sql = """
            UPDATE session_summary ss
               SET raw_points_pruned_at = now(),
                   updated_at = now()
             WHERE ss.session_id IN (
               SELECT s.id
                 FROM tracking_sessions s
                WHERE s.stop_time IS NOT NULL
                  AND s.stop_time < ?
                  AND s.status IN ('STOPPED','EXPIRED','ARCHIVED')
                ORDER BY s.stop_time ASC
                LIMIT ?
             )
               AND ss.raw_points_pruned_at IS NULL
            """;
        OffsetDateTime cutoffTs = cutoff.atOffset(ZoneOffset.UTC);
        return jdbc.update(sql, ps -> {
            ps.setObject(1, cutoffTs);
            ps.setInt(2, batch);
        });
    }

    private int prunePointsBatch(Instant cutoff, int batchPoints) {
        // Batch delete by id (LIMIT supported via subquery)
        String sql = """
            DELETE FROM tracking_points
             WHERE id IN (
               SELECT id
                 FROM tracking_points
                WHERE device_timestamp < ?
                ORDER BY device_timestamp ASC
                LIMIT ?
             )
            """;
        OffsetDateTime cutoffTs = cutoff.atOffset(ZoneOffset.UTC);
        return jdbc.update(sql, ps -> {
            ps.setObject(1, cutoffTs);
            ps.setInt(2, batchPoints);
        });
    }
}
