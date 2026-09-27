package org.janus.modules.authentication.services.refresh;

import org.janus.modules.authentication.application.service.refreshToken.DeleteRefreshTokenById;
import org.janus.modules.authentication.port.out.RefreshTokenRepository;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteRefreshTokenByIdTest {

    @Mock
    private RefreshTokenRepository repository;

    @InjectMocks
    private DeleteRefreshTokenById useCase;

    private UUID tokenId;

    @BeforeEach
    void setUp() {
        tokenId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Validation Scenarios")
    class ValidationScenarios {

        @Test
        @DisplayName("Should return 400 Bad Request when ID is null")
        void shouldReturn400WhenIdIsNull() {
            Result<Void> result = useCase.execute(null);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("Refresh token ID cannot be null");

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should delete refresh token successfully when 1 record is deleted and verify execution order")
        void shouldDeleteRefreshTokenSuccessfully() {
            when(repository.deleteById(tokenId)).thenReturn(1);

            Result<Void> result = useCase.execute(tokenId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(204);

            InOrder inOrder = inOrder(repository);
            inOrder.verify(repository).deleteById(tokenId);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Not Found Scenarios")
    class NotFoundScenarios {

        @Test
        @DisplayName("Should return 404 Not Found when token does not exist (0 records deleted) and verify execution order")
        void shouldReturn404WhenTokenNotFound() {
            when(repository.deleteById(tokenId)).thenReturn(0);

            Result<Void> result = useCase.execute(tokenId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(404);
            assertThat(result.getFirstError()).contains("Refresh token not found with id: '" + tokenId + "'");

            InOrder inOrder = inOrder(repository);
            inOrder.verify(repository).deleteById(tokenId);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Multiple Deletion Scenarios")
    class MultipleDeletionScenarios {

        @Test
        @DisplayName("Should return 400 Bad Request when 2 tokens are deleted and verify execution order")
        void shouldReturn400WhenMultipleTokensDeleted() {
            when(repository.deleteById(tokenId)).thenReturn(2);

            Result<Void> result = useCase.execute(tokenId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("More one Refresh token deleted");

            InOrder inOrder = inOrder(repository);
            inOrder.verify(repository).deleteById(tokenId);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Unexpected Error Scenarios")
    class UnexpectedErrorScenarios {

        @Test
        @DisplayName("Should throw InternalServerErrorException when repository throws unexpected exception")
        void shouldThrowInternalServerErrorExceptionOnGenericError() {
            when(repository.deleteById(tokenId)).thenThrow(new RuntimeException("Database error"));

            assertThatThrownBy(() -> useCase.execute(tokenId))
                    .isInstanceOf(InternalServerErrorException.class)
                    .hasMessageContaining("Database error");

            InOrder inOrder = inOrder(repository);
            inOrder.verify(repository).deleteById(tokenId);
            inOrder.verifyNoMoreInteractions();
        }
    }
}