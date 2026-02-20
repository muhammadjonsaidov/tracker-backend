package com.rhaen.tracker.feature.tracking.persistence;

import com.rhaen.tracker.BaseIntegrationTest;
import com.rhaen.tracker.feature.user.persistence.UserEntity;
import com.rhaen.tracker.feature.user.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class TrackingSessionRepositoryTest extends BaseIntegrationTest {

        @Autowired
        private TrackingSessionRepository sessionRepository;
        @Autowired
        private UserRepository userRepository;

        @Test
        void findActiveByUserId_returnsLatestActive() {
                String suffix = UUID.randomUUID().toString().substring(0, 8);
                UserEntity user = userRepository.save(UserEntity.builder()
                                .username("repo_" + suffix)
                                .email("repo_" + suffix + "@mail.com")
                                .passwordHash("hashed")
                                .role(UserEntity.Role.USER)
                                .build());

                sessionRepository.save(TrackingSessionEntity.builder()
                                .user(user)
                                .startTime(Instant.now().minusSeconds(600))
                                .status(TrackingSessionEntity.Status.STOPPED)
                                .updatedAt(Instant.now())
                                .build());

                TrackingSessionEntity active = sessionRepository.save(TrackingSessionEntity.builder()
                                .user(user)
                                .startTime(Instant.now())
                                .status(TrackingSessionEntity.Status.ACTIVE)
                                .updatedAt(Instant.now())
                                .build());

                var found = sessionRepository.findActiveByUserId(user.getId());
                assertThat(found).isPresent();
                assertThat(found.get().getId()).isEqualTo(active.getId());
        }
}
