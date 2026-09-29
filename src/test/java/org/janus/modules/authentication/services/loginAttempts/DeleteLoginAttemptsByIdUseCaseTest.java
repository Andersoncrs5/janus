package org.janus.modules.authentication.services.loginAttempts;

import org.janus.modules.authentication.application.service.loginAttempts.DeleteLoginAttemptsByIdUseCase;
import org.janus.modules.authentication.port.out.LoginAttemptRepository;
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
class DeleteLoginAttemptsByIdUseCaseTest {

    @Mock
    private LoginAttemptRepository repository;

    @InjectMocks
    private DeleteLoginAttemptsByIdUseCase useCase;

    @Nested
    @DisplayName("Execute - Delete Login Attempt By ID")
    class Execute {

        @Test
        @DisplayName("Should delete login attempt successfully when exactly one record is deleted")
        void shouldDeleteLoginAttemptSuccessfullyWhenOneRecordIsDeleted() {
            UUID id = UUID.randomUUID();
            when(repository.deleteById(id)).thenReturn(1);

            Result<Void> result = useCase.execute(id);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).deleteById(id);
        }

        @Test
        @DisplayName("Should return not found when no record is deleted")
        void shouldReturnNotFoundWhenNoRecordIsDeleted() {
            UUID id = UUID.randomUUID();
            when(repository.deleteById(id)).thenReturn(0);

            Result<Void> result = useCase.execute(id);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage().isPresent()).isTrue();
            assertThat(result.getMessage().get()).isEqualTo("Login attempt not found");
            verify(repository).deleteById(id);
        }

        @Test
        @DisplayName("Should return failure when more than one record is deleted")
        void shouldReturnFailureWhenMoreThanOneRecordIsDeleted() {
            UUID id = UUID.randomUUID();
            when(repository.deleteById(id)).thenReturn(2);

            Result<Void> result = useCase.execute(id);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage().isPresent()).isTrue();
            assertThat(result.getMessage().get()).contains("More than one login attempt was deleted");
            verify(repository).deleteById(id);
        }
    }
}