package org.janus.modules.authentication.services.refresh;

import org.janus.modules.authentication.application.service.refreshToken.RevokeAllTokenByUserIdUseCase;
import org.janus.modules.authentication.port.out.RefreshTokenRepository;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevokeAllTokenByUserIdUseCaseTest {

    @Mock
    private RefreshTokenRepository repository;

    @InjectMocks
    private RevokeAllTokenByUserIdUseCase useCase;

    @Nested
    @DisplayName("Validation Scenarios")
    class ValidationScenarios {

        @Test
        @DisplayName("Should return 400 Bad Request when userId is null")
        void shouldReturn400WhenUserIdIsNull() {
            Result<Integer> result = useCase.execute(null);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("User ID cannot be null");

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should revoke all refresh tokens successfully for given userId")
        void shouldRevokeAllTokensSuccessfully() {
            UUID userId = UUID.randomUUID();
            int expectedRevokedCount = 3;

            when(repository.revokeAllByUserId(userId)).thenReturn(expectedRevokedCount);

            Result<Integer> result = useCase.execute(userId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue()).isEqualTo(expectedRevokedCount);

            verify(repository).revokeAllByUserId(userId);
        }

        @Test
        @DisplayName("Should return success with zero when user has no active refresh tokens")
        void shouldReturnSuccessWhenNoTokensFound() {
            UUID userId = UUID.randomUUID();

            when(repository.revokeAllByUserId(userId)).thenReturn(0);

            Result<Integer> result = useCase.execute(userId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue()).isEqualTo(0);

            verify(repository).revokeAllByUserId(userId);
        }
    }
}