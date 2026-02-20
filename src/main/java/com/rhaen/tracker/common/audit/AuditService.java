package com.rhaen.tracker.common.audit;

import com.fasterxml.jackson.databind.JsonNode;
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
            JsonNode metadataJson = metadata == null ? null : objectMapper.valueToTree(metadata);
            var user = actorUserId == null ? null : userRepository.findById(actorUserId).orElse(null);

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

    public void logSystemAction(String action,
                                String targetType,
                                UUID targetId,
                                Map<String, Object> metadata) {
        logUserAction(null, action, targetType, targetId, metadata);
    }
}
