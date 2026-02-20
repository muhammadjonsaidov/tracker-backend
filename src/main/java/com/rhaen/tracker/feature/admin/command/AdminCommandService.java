package com.rhaen.tracker.feature.admin.command;

import com.rhaen.tracker.common.audit.AuditService;
import com.rhaen.tracker.common.exception.NotFoundException;
import com.rhaen.tracker.feature.user.persistence.UserEntity;
import com.rhaen.tracker.feature.user.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminCommandService {

        private final UserRepository userRepository;
        private final AuditService auditService;
        private final com.rhaen.tracker.feature.tracking.realtime.LastLocationCache lastLocationCache;

        @Transactional
        public void updateUserRole(UUID adminId, UUID userId, String role) {
                UserEntity user = userRepository.findById(userId)
                                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

                String oldRole = user.getRole().name();
                user.setRole(UserEntity.Role.valueOf(role.toUpperCase()));
                userRepository.save(user);

                auditService.logUserAction(
                                adminId,
                                "ADMIN_UPDATE_USER_ROLE",
                                "USER",
                                userId,
                                Map.of("oldRole", oldRole, "newRole", role));
        }

        @Transactional
        public void deleteUser(UUID adminId, UUID userId) {
                UserEntity user = userRepository.findById(userId)
                                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

                userRepository.delete(user);
                lastLocationCache.remove(userId);

                auditService.logUserAction(
                                adminId,
                                "ADMIN_DELETE_USER",
                                "USER",
                                userId,
                                Map.of("username", user.getUsername(), "email", user.getEmail()));
        }
}
