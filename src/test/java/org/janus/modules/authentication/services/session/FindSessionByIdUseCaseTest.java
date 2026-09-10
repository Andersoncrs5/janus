package org.janus.modules.authentication.services.session;

import org.janus.modules.authentication.application.service.session.FindSessionByIdUseCase;
import org.janus.modules.authentication.domain.entity.SessionEntity;
import org.janus.modules.authentication.port.out.SessionRepository;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindSessionByIdUseCaseTest {

    @Mock
    private SessionRepository repository;

    @InjectMocks
    private FindSessionByIdUseCase useCase;

    @Nested
    @DisplayName("Execute - Find Session By ID")
    class Execute {

        @Test
        @DisplayName("Should return session successfully when session exists")
        void shouldReturnSessionSuccessfullyWhenSessionExists() {
            UUID sessionId = UUID.randomUUID();
            SessionEntity expectedSession = new SessionEntity();
            expectedSession.setId(sessionId);

            when(repository.findById(sessionId)).thenReturn(Optional.of(expectedSession));

            Result<SessionEntity> result = useCase.execute(sessionId);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData().getId()).isEqualTo(sessionId);
            verify(repository).findById(sessionId);
        }

        @Test
        @DisplayName("Should return not found when session does not exist")
        void shouldReturnNotFoundWhenSessionDoesNotExist() {
            UUID sessionId = UUID.randomUUID();

            when(repository.findById(sessionId)).thenReturn(Optional.empty());

            Result<SessionEntity> result = useCase.execute(sessionId);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage().isPresent()).isTrue();
            assertThat(result.getMessage().get()).isEqualTo("Session not found");
            verify(repository).findById(sessionId);
        }
    }
}