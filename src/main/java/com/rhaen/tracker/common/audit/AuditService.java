package com.rhaen.tracker.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhaen.tracker.feature.admin.persistence.AdminAuditLogEntity;
import com.rhaen.tracker.feature.admin.persistence.AdminAuditLogRepository;
import com.rhaen.tracker.feature.user.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditService {
    private final AdminAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public void logUserAction(UUID actorUserId,
                              String action,
                              String targetType,
                              UUID targetId,
                              Map<String, Object> metadata) {
        try {
            if (actorUserId == null) {
                return;
            }
            var user = userRepository.findById(actorUserId).orElse(null);
            if (user == null) {
                return;
            }

            String metadataJson = metadata == null ? null : objectMapper.writeValueAsString(metadata);

            var entry = AdminAuditLogEntity.builder()
                    .admin(user)
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .metadata(metadataJson)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            log.warn("Failed to persist audit log entry, action={}", action, ex);
        }
    }
}
