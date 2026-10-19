package org.janus.modules.authentication.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.janus.help.BaseTest;
import org.janus.modules.authentication.domain.entity.RefreshTokenEntity;
import org.janus.modules.authentication.domain.entity.SessionEntity;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
public class RefreshTokenRepositoryJdbcTest extends BaseTest {
    @Nested
    class Insert {
        @Test
        void shouldInsertTokenWithGeneratedIdWhenIdIsNull() {
            UserEntity user = initUser();
            SessionEntity session = initSession(user.getId());
            RefreshTokenEntity token = createSampleRefreshToken(session.getId(), user.getId(), "hash-123");
            token.setId(null);
            RefreshTokenEntity saved = refreshTokenRepository.insert(token);
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getVersion()).isEqualTo(0L);
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getSessionId()).isEqualTo(session.getId());
            assertThat(saved.getUserId()).isEqualTo(user.getId());
            Optional<RefreshTokenEntity> fetched = refreshTokenRepository.findByTokenHash("hash-123");
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getSessionId()).isEqualTo(session.getId());
        }

        @Test
        void shouldInsertTokenWithProvidedId() {
            UserEntity user = initUser();
            SessionEntity session = initSession(user.getId());
            UUID customId = UUID.randomUUID();
            RefreshTokenEntity token = createSampleRefreshToken(session.getId(), user.getId(), "hash-456");
            token.setId(customId);
            RefreshTokenEntity saved = refreshTokenRepository.insert(token);
            assertThat(saved.getId()).isEqualTo(customId);
            Optional<RefreshTokenEntity> fetched = refreshTokenRepository.findByTokenHash("hash-456");
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getId()).isEqualTo(customId);
        }
    }

    @Nested
    class Save {
        @Test
        void shouldInsertWhenTokenDoesNotExist() {
            UserEntity user = initUser();
            SessionEntity session = initSession(user.getId());
            RefreshTokenEntity token = createSampleRefreshToken(session.getId(), user.getId(), "hash-789");
            RefreshTokenEntity result = refreshTokenRepository.save(token);
            assertThat(result.getId()).isNotNull();
            assertThat(refreshTokenRepository.findByTokenHash("hash-789")).isPresent();
        }

        @Test
        void shouldUpdateWhenTokenAlreadyExists() {
            UserEntity user = initUser();
            SessionEntity session = initSession(user.getId());
            RefreshTokenEntity token = initRefreshToken(session.getId(), user.getId(), "hash-update");
            UUID replacementId = UUID.randomUUID();
            token.setIsUsed(true);
            token.setReplacedByTokenId(replacementId);
            RefreshTokenEntity updated = refreshTokenRepository.save(token);
            assertThat(updated.getIsUsed()).isTrue();
            Optional<RefreshTokenEntity> fetched = refreshTokenRepository.findByTokenHash("hash-update");
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getIsUsed()).isTrue();
            assertThat(fetched.get().getReplacedByTokenId()).isEqualTo(replacementId);
            assertThat(fetched.get().getVersion()).isEqualTo(1L);
        }

        @Test
        void shouldThrowExceptionWhenOptimisticLockFails() {
            UserEntity user = initUser();
            SessionEntity session = initSession(user.getId());
            RefreshTokenEntity token = initRefreshToken(session.getId(), user.getId(), "hash-lock");
            token.setVersion(99L);
            assertThatThrownBy(() -> refreshTokenRepository.save(token)).isInstanceOf(IllegalStateException.class).hasMessageContaining("Failed to update RefreshToken entity " + "(optimistic lock or deleted)");
        }
    }

    @Nested
    class FindByTokenHash {
        @Test
        void shouldReturnTokenWhenHashExists() {
            UserEntity user = initUser();
            SessionEntity session = initSession(user.getId());
            String hash = "hash-find-me";
            RefreshTokenEntity token = initRefreshToken(session.getId(), user.getId(), hash);
            Optional<RefreshTokenEntity> result = refreshTokenRepository.findByTokenHash(hash);
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(token.getId());
            assertThat(result.get().getTokenHash()).isEqualTo(hash);
        }

        @Test
        void shouldReturnEmptyWhenHashDoesNotExist() {
            Optional<RefreshTokenEntity> result = refreshTokenRepository.findByTokenHash("NON_EXISTENT_HASH");
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class RevokeOperations {
        @Test
        void shouldRevokeById() {
            UserEntity user = initUser();
            SessionEntity session = initSession(user.getId());
            RefreshTokenEntity token = initRefreshToken(session.getId(), user.getId(), "hash-revoke-id");
            int affectedRows = refreshTokenRepository.revokeById(token.getId());
            assertThat(affectedRows).isEqualTo(1);
            Optional<RefreshTokenEntity> fetched = refreshTokenRepository.findByTokenHash("hash-revoke-id");
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getIsRevoked()).isTrue();
        }

        @Test
        void shouldRevokeAllBySessionId() {
            UserEntity user = initUser();
            SessionEntity session = initSession(user.getId());
            SessionEntity otherSession = initSession(user.getId());
            initRefreshToken(session.getId(), user.getId(), "hash-session-1");
            initRefreshToken(session.getId(), user.getId(), "hash-session-2");
            initRefreshToken(otherSession.getId(), user.getId(), "hash-other-session");
            int affectedRows = refreshTokenRepository.revokeAllBySessionId(session.getId());
            assertThat(affectedRows).isEqualTo(2);
            assertThat(refreshTokenRepository.findByTokenHash("hash-session-1").orElseThrow().getIsRevoked()).isTrue();
            assertThat(refreshTokenRepository.findByTokenHash("hash-session-2").orElseThrow().getIsRevoked()).isTrue();
            assertThat(refreshTokenRepository.findByTokenHash("hash-other-session").orElseThrow().getIsRevoked()).isFalse();
        }

        @Test
        void shouldRevokeAllByUserId() {
            UserEntity user = initUser();
            UserEntity otherUser = initUser();
            SessionEntity session1 = initSession(user.getId());
            SessionEntity session2 = initSession(user.getId());
            SessionEntity otherSession = initSession(otherUser.getId());
            initRefreshToken(session1.getId(), user.getId(), "hash-user-1");
            initRefreshToken(session2.getId(), user.getId(), "hash-user-2");
            initRefreshToken(otherSession.getId(), otherUser.getId(), "hash-other-user");
            int affectedRows = refreshTokenRepository.revokeAllByUserId(user.getId());
            assertThat(affectedRows).isEqualTo(2);
            assertThat(refreshTokenRepository.findByTokenHash("hash-user-1").orElseThrow().getIsRevoked()).isTrue();
            assertThat(refreshTokenRepository.findByTokenHash("hash-user-2").orElseThrow().getIsRevoked()).isTrue();
            assertThat(refreshTokenRepository.findByTokenHash("hash-other-user").orElseThrow().getIsRevoked()).isFalse();
        }

        @Test
        void shouldRevokeFamilyBySessionId() {
            UserEntity user = initUser();
            SessionEntity session = initSession(user.getId());
            initRefreshToken(session.getId(), user.getId(), "hash-family-1");
            int affectedRows = refreshTokenRepository.revokeFamilyBySessionId(session.getId());
            assertThat(affectedRows).isEqualTo(1);
            assertThat(refreshTokenRepository.findByTokenHash("hash-family-1").orElseThrow().getIsRevoked()).isTrue();
        }
    }

    @Nested
    class ExistsActiveBySessionId {
        @Test
        void shouldReturnTrueWhenActiveTokenExists() {
            UserEntity user = initUser();
            SessionEntity session = initSession(user.getId());
            initRefreshToken(session.getId(), user.getId(), "hash-active");
            boolean exists = refreshTokenRepository.existsActiveBySessionId(session.getId());
            assertThat(exists).isTrue();
        }

        @Test
        void shouldReturnFalseWhenTokenIsRevoked() {
            UserEntity user = initUser();
            SessionEntity session = initSession(user.getId());
            RefreshTokenEntity token = createSampleRefreshToken(session.getId(), user.getId(), "hash-revoked");
            token.setIsRevoked(true);
            refreshTokenRepository.insert(token);
            boolean exists = refreshTokenRepository.existsActiveBySessionId(session.getId());
            assertThat(exists).isFalse();
        }

        @Test
        void shouldReturnFalseWhenTokenIsUsed() {
            UserEntity user = initUser();
            SessionEntity session = initSession(user.getId());
            RefreshTokenEntity token = createSampleRefreshToken(session.getId(), user.getId(), "hash-used");
            token.setIsUsed(true);
            refreshTokenRepository.insert(token);
            boolean exists = refreshTokenRepository.existsActiveBySessionId(session.getId());
            assertThat(exists).isFalse();
        }

        @Test
        void shouldReturnFalseWhenTokenIsExpired() {
            UserEntity user = initUser();
            SessionEntity session = initSession(user.getId());
            RefreshTokenEntity token = createSampleRefreshToken(session.getId(), user.getId(), "hash-expired");
            token.setExpiresAt(OffsetDateTime.now().minusMinutes(5)); /* * This intentionally violates the database constraint * expires_at > created_at. * * Therefore, if the schema enforces that constraint, * this fixture cannot be inserted as-is. * * The repository test should instead create a valid * historical record or the repository should allow * explicitly controlled created_at values. */
            token.setCreatedAt(OffsetDateTime.now().minusMinutes(10));
            refreshTokenRepository.insert(token);
            boolean exists = refreshTokenRepository.existsActiveBySessionId(session.getId());
            assertThat(exists).isFalse();
        }
    }

    @Nested
    class FindLatestBySessionId {
        @Test
        void shouldReturnMostRecentTokenForSession() throws InterruptedException {
            UserEntity user = initUser();
            SessionEntity session = initSession(user.getId());
            RefreshTokenEntity olderToken = createSampleRefreshToken(session.getId(), user.getId(), "hash-older");
            refreshTokenRepository.insert(olderToken);
            Thread.sleep(50);
            RefreshTokenEntity newerToken = createSampleRefreshToken(session.getId(), user.getId(), "hash-newer");
            refreshTokenRepository.insert(newerToken);
            Optional<RefreshTokenEntity> result = refreshTokenRepository.findLatestBySessionId(session.getId());
            assertThat(result).isPresent();
            assertThat(result.get().getTokenHash()).isEqualTo("hash-newer");
        }

        @Test
        void shouldReturnEmptyWhenNoTokensForSession() {
            UserEntity user = initUser();
            SessionEntity session = initSession(user.getId());
            Optional<RefreshTokenEntity> result = refreshTokenRepository.findLatestBySessionId(session.getId());
            assertThat(result).isEmpty();
        }
    }
}