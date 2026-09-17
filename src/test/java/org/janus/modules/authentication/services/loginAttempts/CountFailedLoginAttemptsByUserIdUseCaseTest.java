package org.janus.modules.authentication.services.loginAttempts;

import org.janus.modules.authentication.application.service.loginAttempts.CountFailedLoginAttemptsByUserIdUseCase;
import org.janus.modules.authentication.port.out.LoginAttemptRepository;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CountFailedLoginAttemptsByUserIdUseCaseTest {

    @Mock
    private LoginAttemptRepository repository;

    @InjectMocks
    private CountFailedLoginAttemptsByUserIdUseCase useCase;

    @Nested
    @DisplayName("Validation Scenarios")
    class ValidationScenarios {

        @Test
        @DisplayName("Should return 400 Bad Request when userId is null")
        void shouldReturn400WhenUserIdIsNull() {
            OffsetDateTime since = OffsetDateTime.now();

            Result<Long> result = useCase.execute(null, since);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("User ID cannot be null");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when since is null")
        void shouldReturn400WhenSinceIsNull() {
            UUID userId = UUID.randomUUID();

            Result<Long> result = useCase.execute(userId, null);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("Since timestamp cannot be null");

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should successfully return count of failed login attempts")
        void shouldReturnCountOfFailedLoginAttemptsSuccessfully() {
            UUID userId = UUID.randomUUID();
            OffsetDateTime since = OffsetDateTime.now().minusHours(1);
            long expectedCount = 3L;

            when(repository.countFailedAttemptsByUserId(userId, since)).thenReturn(expectedCount);

            Result<Long> result = useCase.execute(userId, since);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue()).isEqualTo(expectedCount);

            verify(repository).countFailedAttemptsByUserId(userId, since);
        }

        @Test
        @DisplayName("Should return zero when no failed attempts exist")
        void shouldReturnZeroWhenNoFailedAttemptsExist() {
            UUID userId = UUID.randomUUID();
            OffsetDateTime since = OffsetDateTime.now().minusHours(1);

            when(repository.countFailedAttemptsByUserId(userId, since)).thenReturn(0L);

            Result<Long> result = useCase.execute(userId, since);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue()).isEqualTo(0L);

            verify(repository).countFailedAttemptsByUserId(userId, since);
        }
    }
}