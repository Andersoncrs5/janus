package org.janus.modules.authentication.repository;

import io.quarkus.test.junit.QuarkusTest;
import org.janus.help.BaseTest;
import org.janus.modules.authentication.domain.entity.LoginAttemptEntity;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class LoginAttemptRepositoryJDBCTest extends BaseTest {
    @Nested
    class Insert {
        @Test
        void shouldInsertLoginAttemptWithGeneratedIdWhenIdIsNull() {
            UserEntity user = initUser();
            LoginAttemptEntity attempt = createSampleLoginAttempt(user.getId(), user.getEmail(), false);
            attempt.setId(null);
            LoginAttemptEntity saved = loginAttemptRepository.insert(attempt);
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUserId()).isEqualTo(user.getId());
            assertThat(saved.getEmailAttempted()).isEqualTo(user.getEmail());
            assertThat(saved.getSuccess()).isFalse();
            List<LoginAttemptEntity> attempts = loginAttemptRepository.findRecentByUserId(user.getId(), 10);
            assertThat(attempts).anyMatch(item -> item.getId().equals(saved.getId()));
        }

        @Test
        void shouldInsertLoginAttemptWithProvidedId() {
            UserEntity user = initUser();
            UUID customId = UUID.randomUUID();
            LoginAttemptEntity attempt = createSampleLoginAttempt(user.getId(), user.getEmail(), false);
            attempt.setId(customId);
            LoginAttemptEntity saved = loginAttemptRepository.insert(attempt);
            assertThat(saved.getId()).isEqualTo(customId);
            List<LoginAttemptEntity> attempts = loginAttemptRepository.findRecentByUserId(user.getId(), 10);
            assertThat(attempts).anyMatch(item -> item.getId().equals(customId));
        }

        @Test
        void shouldInsertSuccessfulLoginAttempt() {
            UserEntity user = initUser();
            LoginAttemptEntity attempt = createSampleLoginAttempt(user.getId(), user.getEmail(), true);
            attempt.setFailureReason(null);
            LoginAttemptEntity saved = loginAttemptRepository.insert(attempt);
            assertThat(saved.getSuccess()).isTrue();
            assertThat(saved.getFailureReason()).isNull();
        }

        @Test
        void shouldInsertFailedLoginAttemptWithFailureReason() {
            UserEntity user = initUser();
            LoginAttemptEntity attempt = createSampleLoginAttempt(user.getId(), user.getEmail(), false);
            attempt.setFailureReason("INVALID_CREDENTIALS");
            LoginAttemptEntity saved = loginAttemptRepository.insert(attempt);
            assertThat(saved.getSuccess()).isFalse();
            assertThat(saved.getFailureReason()).isEqualTo("INVALID_CREDENTIALS");
        }
    }

    @Nested
    class CountFailedAttemptsByUserId {
        @Test
        void shouldCountOnlyFailedAttemptsForUser() {
            UserEntity user = initUser();
            insertAttempt(user.getId(), user.getEmail(), false);
            insertAttempt(user.getId(), user.getEmail(), false);
            insertAttempt(user.getId(), user.getEmail(), true);
            UserEntity otherUser = initUser();
            insertAttempt(otherUser.getId(), otherUser.getEmail(), false);
            long count = loginAttemptRepository.countFailedAttemptsByUserId(user.getId(), OffsetDateTime.now().minusMinutes(5));
            assertThat(count).isEqualTo(2);
        }

        @Test
        void shouldReturnZeroWhenUserHasNoFailedAttempts() {
            UserEntity user = initUser();
            insertAttempt(user.getId(), user.getEmail(), true);
            long count = loginAttemptRepository.countFailedAttemptsByUserId(user.getId(), OffsetDateTime.now().minusMinutes(5));
            assertThat(count).isZero();
        }

        @Test
        void shouldCountOnlyAttemptsAfterSince() {
            UserEntity user = initUser();
            LoginAttemptEntity oldAttempt = createSampleLoginAttempt(user.getId(), user.getEmail(), false);
            oldAttempt.setCreatedAt(OffsetDateTime.now().minusHours(2));
            loginAttemptRepository.insert(oldAttempt);
            insertAttempt(user.getId(), user.getEmail(), false);
            long count = loginAttemptRepository.countFailedAttemptsByUserId(user.getId(), OffsetDateTime.now().minusMinutes(30));
            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    class CountFailedAttemptsByIpAddress {
        @Test
        void shouldCountFailedAttemptsForIpAddress() {
            UserEntity user = initUser();
            insertAttempt(user.getId(), user.getEmail(), false, "192.168.1.100");
            insertAttempt(user.getId(), user.getEmail(), false, "192.168.1.100");
            insertAttempt(user.getId(), user.getEmail(), true, "192.168.1.100");
            insertAttempt(user.getId(), user.getEmail(), false, "10.0.0.1");
            long count = loginAttemptRepository.countFailedAttemptsByIpAddress("192.168.1.100", OffsetDateTime.now().minusMinutes(5));
            assertThat(count).isEqualTo(2);
        }

        @Test
        void shouldReturnZeroForBlankIpAddress() {
            long count = loginAttemptRepository.countFailedAttemptsByIpAddress(" ", OffsetDateTime.now().minusMinutes(5));
            assertThat(count).isZero();
        }

        @Test
        void shouldReturnZeroForNullIpAddress() {
            long count = loginAttemptRepository.countFailedAttemptsByIpAddress(null, OffsetDateTime.now().minusMinutes(5));
            assertThat(count).isZero();
        }
    }

    @Nested
    class CountFailedAttemptsByEmail {
        @Test
        void shouldCountFailedAttemptsIgnoringEmailCase() {
            UserEntity user = initUser();
            insertAttempt(user.getId(), user.getEmail().toLowerCase(), false);
            long count = loginAttemptRepository.countFailedAttemptsByEmail(user.getEmail().toUpperCase(), OffsetDateTime.now().minusMinutes(5));
            assertThat(count).isEqualTo(1);
        }

        @Test
        void shouldReturnZeroWhenEmailHasNoFailedAttempts() {
            UserEntity user = initUser();
            insertAttempt(user.getId(), user.getEmail(), true);
            long count = loginAttemptRepository.countFailedAttemptsByEmail(user.getEmail(), OffsetDateTime.now().minusMinutes(5));
            assertThat(count).isZero();
        }

        @Test
        void shouldReturnZeroForBlankEmail() {
            long count = loginAttemptRepository.countFailedAttemptsByEmail(" ", OffsetDateTime.now().minusMinutes(5));
            assertThat(count).isZero();
        }

        @Test
        void shouldReturnZeroForNullEmail() {
            long count = loginAttemptRepository.countFailedAttemptsByEmail(null, OffsetDateTime.now().minusMinutes(5));
            assertThat(count).isZero();
        }
    }

    @Nested
    class FindRecentByUserId {
        @Test
        void shouldReturnRecentAttemptsForUser() {
            UserEntity user = initUser();
            LoginAttemptEntity first = insertAttempt(user.getId(), user.getEmail(), false);
            LoginAttemptEntity second = insertAttempt(user.getId(), user.getEmail(), true);
            List<LoginAttemptEntity> result = loginAttemptRepository.findRecentByUserId(user.getId(), 10);
            assertThat(result).hasSize(2).extracting(LoginAttemptEntity::getId).containsExactly(second.getId(), first.getId());
        }

        @Test
        void shouldRespectLimit() {
            UserEntity user = initUser();
            insertAttempt(user.getId(), user.getEmail(), false);
            insertAttempt(user.getId(), user.getEmail(), false);
            insertAttempt(user.getId(), user.getEmail(), false);
            List<LoginAttemptEntity> result = loginAttemptRepository.findRecentByUserId(user.getId(), 2);
            assertThat(result).hasSize(2);
        }

        @Test
        void shouldReturnEmptyListWhenUserHasNoAttempts() {
            List<LoginAttemptEntity> result = loginAttemptRepository.findRecentByUserId(UUID.randomUUID(), 10);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindRecentByIpAddress {
        @Test
        void shouldReturnRecentAttemptsForIpAddress() {
            UserEntity user = initUser();
            insertAttempt(user.getId(), user.getEmail(), false, "192.168.1.10");
            insertAttempt(user.getId(), user.getEmail(), true, "192.168.1.10");
            insertAttempt(user.getId(), user.getEmail(), false, "10.0.0.1");
            List<LoginAttemptEntity> result = loginAttemptRepository.findRecentByIpAddress("192.168.1.10", 10);
            assertThat(result).hasSize(2).allMatch(attempt -> attempt.getIpAddress().equals("192.168.1.10"));
        }

        @Test
        void shouldRespectLimit() {
            UserEntity user = initUser();
            insertAttempt(user.getId(), user.getEmail(), false, "192.168.1.20");
            insertAttempt(user.getId(), user.getEmail(), false, "192.168.1.20");
            insertAttempt(user.getId(), user.getEmail(), false, "192.168.1.20");
            List<LoginAttemptEntity> result = loginAttemptRepository.findRecentByIpAddress("192.168.1.20", 2);
            assertThat(result).hasSize(2);
        }

        @Test
        void shouldReturnEmptyListForBlankIpAddress() {
            List<LoginAttemptEntity> result = loginAttemptRepository.findRecentByIpAddress(" ", 10);
            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyListForNullIpAddress() {
            List<LoginAttemptEntity> result = loginAttemptRepository.findRecentByIpAddress(null, 10);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindRecentByEmail {
        @Test
        void shouldReturnRecentAttemptsIgnoringEmailCase() {
            UserEntity user = initUser();
            insertAttempt(user.getId(), user.getEmail(), false);
            List<LoginAttemptEntity> result = loginAttemptRepository.findRecentByEmail(user.getEmail().toUpperCase(), 10);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getEmailAttempted()).isEqualTo(user.getEmail());
        }

        @Test
        void shouldRespectLimit() {
            UserEntity user = initUser();
            insertAttempt(user.getId(), user.getEmail(), false);
            insertAttempt(user.getId(), user.getEmail(), false);
            insertAttempt(user.getId(), user.getEmail(), false);
            List<LoginAttemptEntity> result = loginAttemptRepository.findRecentByEmail(user.getEmail(), 2);
            assertThat(result).hasSize(2);
        }

        @Test
        void shouldReturnEmptyListForUnknownEmail() {
            List<LoginAttemptEntity> result = loginAttemptRepository.findRecentByEmail("unknown@example.com", 10);
            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyListForBlankEmail() {
            List<LoginAttemptEntity> result = loginAttemptRepository.findRecentByEmail(" ", 10);
            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyListForNullEmail() {
            List<LoginAttemptEntity> result = loginAttemptRepository.findRecentByEmail(null, 10);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class ExistsSuccessfulAttemptByUserIdSince {
        @Test
        void shouldReturnTrueWhenSuccessfulAttemptExists() {
            UserEntity user = initUser();
            insertAttempt(user.getId(), user.getEmail(), true);
            boolean exists = loginAttemptRepository.existsSuccessfulAttemptByUserIdSince(user.getId(), OffsetDateTime.now().minusMinutes(5));
            assertThat(exists).isTrue();
        }

        @Test
        void shouldReturnFalseWhenOnlyFailedAttemptsExist() {
            UserEntity user = initUser();
            insertAttempt(user.getId(), user.getEmail(), false);
            boolean exists = loginAttemptRepository.existsSuccessfulAttemptByUserIdSince(user.getId(), OffsetDateTime.now().minusMinutes(5));
            assertThat(exists).isFalse();
        }

        @Test
        void shouldReturnFalseWhenSuccessfulAttemptIsOlderThanSince() {
            UserEntity user = initUser();
            LoginAttemptEntity attempt = createSampleLoginAttempt(user.getId(), user.getEmail(), true);
            attempt.setCreatedAt(OffsetDateTime.now().minusHours(2));
            loginAttemptRepository.insert(attempt);
            boolean exists = loginAttemptRepository.existsSuccessfulAttemptByUserIdSince(user.getId(), OffsetDateTime.now().minusMinutes(30));
            assertThat(exists).isFalse();
        }

        @Test
        void shouldReturnFalseWhenUserHasNoAttempts() {
            boolean exists = loginAttemptRepository.existsSuccessfulAttemptByUserIdSince(UUID.randomUUID(), OffsetDateTime.now().minusMinutes(5));
            assertThat(exists).isFalse();
        }
    }

    private LoginAttemptEntity insertAttempt(UUID userId, String email, boolean success) {
        return insertAttempt(userId, email, success, "192.168.1.1");
    }

    private LoginAttemptEntity insertAttempt(UUID userId, String email, boolean success, String ipAddress) {
        LoginAttemptEntity attempt = createSampleLoginAttempt(userId, email, success);
        attempt.setIpAddress(ipAddress);
        return loginAttemptRepository.insert(attempt);
    }
}