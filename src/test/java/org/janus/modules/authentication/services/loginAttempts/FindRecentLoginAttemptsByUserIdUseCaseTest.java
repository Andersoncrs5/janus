package org.janus.modules.authentication.services.loginAttempts;

import org.janus.modules.authentication.application.service.loginAttempts.FindRecentLoginAttemptsByUserIdUseCase;
import org.janus.modules.authentication.domain.entity.LoginAttemptEntity;
import org.janus.modules.authentication.port.out.LoginAttemptRepository;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindRecentLoginAttemptsByUserIdUseCaseTest {

    @Mock
    private LoginAttemptRepository repository;

    @InjectMocks
    private FindRecentLoginAttemptsByUserIdUseCase useCase;

    @Nested
    @DisplayName("Validation Scenarios")
    class ValidationScenarios {

        @Test
        @DisplayName("Should return 400 Bad Request when userId is null")
        void shouldReturn400WhenUserIdIsNull() {
            Result<List<LoginAttemptEntity>> result = useCase.execute(null, 10);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("User ID cannot be null");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when limit is less than or equal to zero")
        void shouldReturn400WhenLimitIsInvalid() {
            UUID userId = UUID.randomUUID();

            Result<List<LoginAttemptEntity>> result = useCase.execute(userId, 0);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("Limit must be greater than zero");

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should successfully return recent login attempts for user")
        void shouldReturnRecentLoginAttemptsSuccessfully() {
            UUID userId = UUID.randomUUID();
            int limit = 5;

            LoginAttemptEntity attempt1 = new LoginAttemptEntity();
            attempt1.setUserId(userId);

            LoginAttemptEntity attempt2 = new LoginAttemptEntity();
            attempt2.setUserId(userId);

            List<LoginAttemptEntity> expectedAttempts = List.of(attempt1, attempt2);

            when(repository.findRecentByUserId(userId, limit)).thenReturn(expectedAttempts);

            Result<List<LoginAttemptEntity>> result = useCase.execute(userId, limit);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue()).hasSize(2).isEqualTo(expectedAttempts);

            verify(repository).findRecentByUserId(userId, limit);
        }

        @Test
        @DisplayName("Should return empty list when no login attempts found")
        void shouldReturnEmptyListWhenNoAttemptsFound() {
            UUID userId = UUID.randomUUID();
            int limit = 10;

            when(repository.findRecentByUserId(userId, limit)).thenReturn(Collections.emptyList());

            Result<List<LoginAttemptEntity>> result = useCase.execute(userId, limit);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue()).isEmpty();

            verify(repository).findRecentByUserId(userId, limit);
        }
    }
}