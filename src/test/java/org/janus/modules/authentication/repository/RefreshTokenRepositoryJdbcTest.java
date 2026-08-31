package org.janus.modules.authentication.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.janus.help.BaseTest;
import org.janus.modules.authentication.domain.entity.RefreshTokenEntity;
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
            RefreshTokenEntity token = createSampleRefreshToken(UUID.randomUUID(), UUID.randomUUID(), "hash-123");
            token.setId(null);

            RefreshTokenEntity saved = refreshTokenRepository.insert(token);

            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getVersion()).isEqualTo(0L);
            assertThat(saved.getCreatedAt()).isNotNull();

            Optional<RefreshTokenEntity> fetched = refreshTokenRepository.findByTokenHash("hash-123");
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getSessionId()).isEqualTo(token.getSessionId());
        }

        @Test
        void shouldInsertTokenWithProvidedId() {
            RefreshTokenEntity token = createSampleRefreshToken(UUID.randomUUID(), UUID.randomUUID(), "hash-456");
            UUID customId = UUID.randomUUID();
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
            RefreshTokenEntity token = createSampleRefreshToken(UUID.randomUUID(), UUID.randomUUID(), "hash-789");

            RefreshTokenEntity result = refreshTokenRepository.save(token);

            assertThat(result.getId()).isNotNull();
            assertThat(refreshTokenRepository.findByTokenHash("hash-789")).isPresent();
        }

        @Test
        void shouldUpdateWhenTokenAlreadyExists() {
            RefreshTokenEntity token = initRefreshToken(UUID.randomUUID(), UUID.randomUUID(), "hash-update");

            UUID replacementId = UUID.randomUUID();
            token.setIsUsed(true);
            token.setReplacedByTokenId(replacementId);

            RefreshTokenEntity updated = refreshTokenRepository.save(token);

            assertThat(updated.getIsUsed()).isTrue();
            assertThat(updated.getVersion()).isEqualTo(1L);

            Optional<RefreshTokenEntity> fetched = refreshTokenRepository.findByTokenHash("hash-update");
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getIsUsed()).isTrue();
            assertThat(fetched.get().getReplacedByTokenId()).isEqualTo(replacementId);
            assertThat(fetched.get().getVersion()).isEqualTo(1L);
        }

        @Test
        void shouldThrowExceptionWhenOptimisticLockFails() {
            RefreshTokenEntity token = initRefreshToken(UUID.randomUUID(), UUID.randomUUID(), "hash-lock");

            token.setVersion(99L); // Simulando versão desatualizada

            assertThatThrownBy(() -> refreshTokenRepository.save(token))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to update RefreshToken entity (optimistic lock or deleted)");
        }
    }

    @Nested
    class FindByTokenHash {

        @Test
        void shouldReturnTokenWhenHashExists() {
            String hash = "hash-find-me";
            RefreshTokenEntity token = initRefreshToken(UUID.randomUUID(), UUID.randomUUID(), hash);

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
            RefreshTokenEntity token = initRefreshToken(UUID.randomUUID(), UUID.randomUUID(), "hash-revoke-id");

            int affectedRows = refreshTokenRepository.revokeById(token.getId());

            assertThat(affectedRows).isEqualTo(1);
            Optional<RefreshTokenEntity> fetched = refreshTokenRepository.findByTokenHash("hash-revoke-id");
            assertThat(fetched).isPresent();
            assertThat(fetched.get().getIsRevoked()).isTrue();
        }

        @Test
        void shouldRevokeAllBySessionId() {
            UUID sessionId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            initRefreshToken(sessionId, userId, "hash-session-1");
            initRefreshToken(sessionId, userId, "hash-session-2");
            initRefreshToken(UUID.randomUUID(), userId, "hash-other-session");

            int affectedRows = refreshTokenRepository.revokeAllBySessionId(sessionId);

            assertThat(affectedRows).isEqualTo(2);
            assertThat(refreshTokenRepository.findByTokenHash("hash-session-1").get().getIsRevoked()).isTrue();
            assertThat(refreshTokenRepository.findByTokenHash("hash-session-2").get().getIsRevoked()).isTrue();
            assertThat(refreshTokenRepository.findByTokenHash("hash-other-session").get().getIsRevoked()).isFalse();
        }

        @Test
        void shouldRevokeAllByUserId() {
            UUID userId = UUID.randomUUID();

            initRefreshToken(UUID.randomUUID(), userId, "hash-user-1");
            initRefreshToken(UUID.randomUUID(), userId, "hash-user-2");
            initRefreshToken(UUID.randomUUID(), UUID.randomUUID(), "hash-other-user");

            int affectedRows = refreshTokenRepository.revokeAllByUserId(userId);

            assertThat(affectedRows).isEqualTo(2);
            assertThat(refreshTokenRepository.findByTokenHash("hash-user-1").get().getIsRevoked()).isTrue();
            assertThat(refreshTokenRepository.findByTokenHash("hash-user-2").get().getIsRevoked()).isTrue();
            assertThat(refreshTokenRepository.findByTokenHash("hash-other-user").get().getIsRevoked()).isFalse();
        }

        @Test
        void shouldRevokeFamilyBySessionId() {
            UUID sessionId = UUID.randomUUID();
            initRefreshToken(sessionId, UUID.randomUUID(), "hash-family-1");

            int affectedRows = refreshTokenRepository.revokeFamilyBySessionId(sessionId);

            assertThat(affectedRows).isEqualTo(1);
            assertThat(refreshTokenRepository.findByTokenHash("hash-family-1").get().getIsRevoked()).isTrue();
        }
    }

    @Nested
    class ExistsActiveBySessionId {

        @Test
        void shouldReturnTrueWhenActiveTokenExists() {
            UUID sessionId = UUID.randomUUID();
            initRefreshToken(sessionId, UUID.randomUUID(), "hash-active");

            boolean exists = refreshTokenRepository.existsActiveBySessionId(sessionId);

            assertThat(exists).isTrue();
        }

        @Test
        void shouldReturnFalseWhenTokenIsRevoked() {
            UUID sessionId = UUID.randomUUID();
            RefreshTokenEntity token = createSampleRefreshToken(sessionId, UUID.randomUUID(), "hash-revoked");
            token.setIsRevoked(true);
            refreshTokenRepository.insert(token);

            boolean exists = refreshTokenRepository.existsActiveBySessionId(sessionId);

            assertThat(exists).isFalse();
        }

        @Test
        void shouldReturnFalseWhenTokenIsUsed() {
            UUID sessionId = UUID.randomUUID();
            RefreshTokenEntity token = createSampleRefreshToken(sessionId, UUID.randomUUID(), "hash-used");
            token.setIsUsed(true);
            refreshTokenRepository.insert(token);

            boolean exists = refreshTokenRepository.existsActiveBySessionId(sessionId);

            assertThat(exists).isFalse();
        }

        @Test
        void shouldReturnFalseWhenTokenIsExpired() {
            UUID sessionId = UUID.randomUUID();
            RefreshTokenEntity token = createSampleRefreshToken(sessionId, UUID.randomUUID(), "hash-expired");
            token.setExpiresAt(OffsetDateTime.now().minusMinutes(5)); // Expirou há 5 minutos
            refreshTokenRepository.insert(token);

            boolean exists = refreshTokenRepository.existsActiveBySessionId(sessionId);

            assertThat(exists).isFalse();
        }
    }

    @Nested
    class FindLatestBySessionId {

        @Test
        void shouldReturnMostRecentTokenForSession() throws InterruptedException {
            UUID sessionId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            RefreshTokenEntity olderToken = createSampleRefreshToken(sessionId, userId, "hash-older");
            refreshTokenRepository.insert(olderToken);

            Thread.sleep(50);

            RefreshTokenEntity newerToken = createSampleRefreshToken(sessionId, userId, "hash-newer");
            refreshTokenRepository.insert(newerToken);

            Optional<RefreshTokenEntity> result = refreshTokenRepository.findLatestBySessionId(sessionId);

            assertThat(result).isPresent();
            assertThat(result.get().getTokenHash()).isEqualTo("hash-newer");
        }

        @Test
        void shouldReturnEmptyWhenNoTokensForSession() {
            Optional<RefreshTokenEntity> result = refreshTokenRepository.findLatestBySessionId(UUID.randomUUID());

            assertThat(result).isEmpty();
        }
    }
}