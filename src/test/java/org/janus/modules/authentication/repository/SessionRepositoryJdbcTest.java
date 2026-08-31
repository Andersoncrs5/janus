package org.janus.modules.authentication.repository;

import io.quarkus.test.junit.QuarkusTest;
import org.janus.help.BaseTest;
import org.janus.modules.authentication.domain.entity.SessionEntity;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
public class SessionRepositoryJdbcTest extends BaseTest {

    @Nested
    class Insert {

        @Test
        void shouldInsertSessionWithGeneratedIdWhenIdIsNull() {
            UserEntity user = initUser();
            SessionEntity session = createSampleSession(initUser().getId());
            session.setId(user.getId());

            SessionEntity saved = sessionRepository.insert(session);

            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getVersion()).isEqualTo(0L);
            assertThat(saved.getCreatedAt()).isNotNull();

            Optional<SessionEntity> fetched = sessionRepository.findById(saved.getId());
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getUserId()).isEqualTo(session.getUserId());
            assertThat(fetched.get().getIpAddress()).isEqualTo("192.168.1.1");
            assertThat(fetched.get().getUserAgent()).isEqualTo("Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        }

        @Test
        void shouldInsertSessionWithProvidedId() {
            UUID customId = initUser().getId();
            SessionEntity session = createSampleSession(initUser().getId());
            session.setId(customId);

            SessionEntity saved = sessionRepository.insert(session);

            assertThat(saved.getId()).isEqualTo(customId);

            Optional<SessionEntity> fetched = sessionRepository.findById(customId);
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getId()).isEqualTo(customId);
        }
    }

    @Nested
    class Save {

        @Test
        void shouldInsertWhenSessionDoesNotExist() {
            SessionEntity session = createSampleSession(initUser().getId());

            SessionEntity saved = sessionRepository.save(session);

            assertThat(saved.getId()).isNotNull();
            assertThat(sessionRepository.findById(saved.getId())).isPresent();
        }

        @Test
        void shouldUpdateWhenSessionExists() {
            SessionEntity session = initSession(initUser().getId());

            session.setIsRevoked(true);
            session.setIpAddress("10.0.0.1");

            SessionEntity updated = sessionRepository.save(session);

            assertThat(updated.getIsRevoked()).isTrue();
            assertThat(updated.getVersion()).isEqualTo(1L);

            Optional<SessionEntity> fetched = sessionRepository.findById(session.getId());
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getIsRevoked()).isTrue();
            assertThat(fetched.get().getIpAddress()).isEqualTo("10.0.0.1");
            assertThat(fetched.get().getVersion()).isEqualTo(1L);
        }

        @Test
        void shouldThrowExceptionWhenOptimisticLockFails() {
            SessionEntity session = initSession(initUser().getId());
            session.setVersion(99L);

            assertThatThrownBy(() -> sessionRepository.save(session))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to update Session entity (optimistic lock or deleted)");
        }
    }

    @Nested
    class FindById {

        @Test
        void shouldReturnSessionWhenIdExists() {
            SessionEntity session = initSession(initUser().getId());

            Optional<SessionEntity> fetched = sessionRepository.findById(session.getId());

            assertThat(fetched).isPresent();
            assertThat(fetched.get().getId()).isEqualTo(session.getId());
        }

        @Test
        void shouldReturnEmptyWhenIdDoesNotExist() {
            Optional<SessionEntity> fetched = sessionRepository.findById(UUID.randomUUID());

            assertThat(fetched).isEmpty();
        }
    }

    @Nested
    class FindActiveByUserId {

        @Test
        void shouldReturnOnlyActiveSessionsForUser() {
            UUID userId = initUser().getId();

            initSession(userId);

            SessionEntity revokedSession = createSampleSession(userId);
            revokedSession.setIsRevoked(true);
            sessionRepository.insert(revokedSession);

            SessionEntity expiredSession = createSampleSession(userId);
            expiredSession.setCreatedAt(OffsetDateTime.now().minusDays(2));
            expiredSession.setExpiresAt(OffsetDateTime.now().minusDays(1));
            sessionRepository.insert(expiredSession);

            List<SessionEntity> activeSessions = sessionRepository.findActiveByUserId(userId);

            assertThat(activeSessions).hasSize(1);
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoActiveSessions() {
            UUID userId = initUser().getId();

            List<SessionEntity> activeSessions = sessionRepository.findActiveByUserId(userId);

            assertThat(activeSessions).isEmpty();
        }
    }

    @Nested
    class RevokeOperations {

        @Test
        void shouldRevokeById() {
            SessionEntity session = initSession(initUser().getId());

            int affectedRows = sessionRepository.revokeById(session.getId());

            assertThat(affectedRows).isEqualTo(1);

            Optional<SessionEntity> fetched = sessionRepository.findById(session.getId());
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getIsRevoked()).isTrue();
        }

        @Test
        void shouldRevokeAllByUserId() {
            UUID userId = initUser().getId();
            initSession(userId);
            initSession(userId);

            UUID otherUserId = initUser().getId();
            initSession(otherUserId);

            int affectedRows = sessionRepository.revokeAllByUserId(userId);

            assertThat(affectedRows).isEqualTo(2);

            List<SessionEntity> userActiveSessions = sessionRepository.findActiveByUserId(userId);
            assertThat(userActiveSessions).isEmpty();

            List<SessionEntity> otherActiveSessions = sessionRepository.findActiveByUserId(otherUserId);
            assertThat(otherActiveSessions).hasSize(1);
        }
    }

}