package org.janus.modules.authentication.services.refresh;

import org.janus.modules.authentication.application.service.refreshToken.FindRefreshTokenByHashUseCase;
import org.janus.modules.authentication.domain.entity.RefreshTokenEntity;
import org.janus.modules.authentication.port.out.RefreshTokenRepository;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindRefreshTokenByHashUseCaseTest {

    @Mock
    private RefreshTokenRepository repository;

    @InjectMocks
    private FindRefreshTokenByHashUseCase useCase;

    private String validTokenHash;
    private RefreshTokenEntity mockEntity;

    @BeforeEach
    void setUp() {
        validTokenHash = "validBase64TokenHashSample123";
        mockEntity = RefreshTokenEntity.builder()
                .sessionId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .tokenHash(validTokenHash)
                .isUsed(false)
                .isRevoked(false)
                .expiresAt(OffsetDateTime.now().plusDays(30))
                .build();
    }

    @Nested
    @DisplayName("Validation Scenarios")
    class ValidationScenarios {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should return 400 Bad Request when tokenHash is null, empty or blank")
        void shouldReturn400WhenTokenHashIsInvalid(String invalidHash) {
            Result<RefreshTokenEntity> result = useCase.execute(invalidHash);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("Token hash cannot be null or empty");

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should return 200 OK with RefreshTokenEntity when tokenHash exists and verify execution order")
        void shouldReturnRefreshTokenWhenHashExists() {
            when(repository.findByTokenHash(validTokenHash)).thenReturn(Optional.of(mockEntity));

            Result<RefreshTokenEntity> result = useCase.execute(validTokenHash);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(200);
            assertThat(result.getValue()).isEqualTo(mockEntity);

            InOrder inOrder = inOrder(repository);
            inOrder.verify(repository).findByTokenHash(validTokenHash);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Not Found Scenarios")
    class NotFoundScenarios {

        @Test
        @DisplayName("Should return 404 Not Found when tokenHash does not exist and verify execution order")
        void shouldReturn404WhenHashDoesNotExist() {
            when(repository.findByTokenHash(validTokenHash)).thenReturn(Optional.empty());

            Result<RefreshTokenEntity> result = useCase.execute(validTokenHash);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(404);
            assertThat(result.getFirstError()).contains("Refresh token not found for hash: '" + validTokenHash + "'");

            InOrder inOrder = inOrder(repository);
            inOrder.verify(repository).findByTokenHash(validTokenHash);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Unexpected Error Scenarios")
    class UnexpectedErrorScenarios {

        @Test
        @DisplayName("Should throw InternalServerErrorException when repository throws unexpected exception")
        void shouldThrowInternalServerErrorExceptionOnGenericError() {
            when(repository.findByTokenHash(validTokenHash)).thenThrow(new RuntimeException("Database timeout"));

            assertThatThrownBy(() -> useCase.execute(validTokenHash))
                    .isInstanceOf(InternalServerErrorException.class)
                    .hasMessageContaining("Database timeout");

            InOrder inOrder = inOrder(repository);
            inOrder.verify(repository).findByTokenHash(validTokenHash);
            inOrder.verifyNoMoreInteractions();
        }
    }
}