package org.janus.modules.authentication.services.session;

import org.janus.modules.authentication.application.service.session.DeleteSessionByIdUseCase;
import org.janus.modules.authentication.port.out.SessionRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteSessionByIdUseCaseTest {

    @Mock
    private SessionRepository repository;

    @InjectMocks
    private DeleteSessionByIdUseCase useCase;

    @Nested
    @DisplayName("Execute - Delete Session By ID")
    class Execute {

        @Test
        @DisplayName("Should delete session successfully when exactly one record is deleted")
        void shouldDeleteSessionSuccessfullyWhenOneRecordIsDeleted() {
            UUID sessionId = UUID.randomUUID();
            when(repository.deleteById(sessionId)).thenReturn(1);

            Result<Void> result = useCase.execute(sessionId);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).deleteById(sessionId);
        }

        @Test
        @DisplayName("Should return not found when no record is deleted")
        void shouldReturnNotFoundWhenNoRecordIsDeleted() {
            UUID sessionId = UUID.randomUUID();
            when(repository.deleteById(sessionId)).thenReturn(0);

            Result<Void> result = useCase.execute(sessionId);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage().isPresent()).isTrue();
            assertThat(result.getMessage().get()).isEqualTo("Session not found");
            verify(repository).deleteById(sessionId);
        }

        @Test
        @DisplayName("Should return failure when more than one record is deleted")
        void shouldReturnFailureWhenMoreThanOneRecordIsDeleted() {
            UUID sessionId = UUID.randomUUID();
            when(repository.deleteById(sessionId)).thenReturn(2);

            Result<Void> result = useCase.execute(sessionId);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage().isPresent()).isTrue();
            assertThat(result.getMessage().get()).contains("More than one session was deleted");
            verify(repository).deleteById(sessionId);
        }
    }
}