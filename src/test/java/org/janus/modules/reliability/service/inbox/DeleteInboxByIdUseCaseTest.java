package org.janus.modules.reliability.service.inbox;

import org.janus.modules.reliability.application.service.inbox.DeleteInboxByIdUseCase;
import org.janus.modules.reliability.port.out.InboxRepository;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteInboxByIdUseCaseTest {

    @Mock
    private InboxRepository repository;

    @InjectMocks
    private DeleteInboxByIdUseCase useCase;

    private UUID id;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should return Result.success when deletion is successful")
    void shouldReturnSuccessWhenDeletionIsSuccessful() {
        when(repository.deleteById(id)).thenReturn(1);

        Result<Void> result = useCase.execute(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getStatusCode()).isEqualTo(200);

        verify(repository, times(1)).deleteById(id);
    }

    @Test
    @DisplayName("Should return Result.notFound (404) when record to delete does not exist")
    void shouldReturnNotFoundWhenRecordDoesNotExist() {
        when(repository.deleteById(id)).thenReturn(0);

        Result<Void> result = useCase.execute(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getStatusCode()).isEqualTo(404);
        assertThat(result.getFirstError()).isEqualTo("Inbox record not found for deletion with ID: " + id);

        verify(repository, times(1)).deleteById(id);
    }

    @Test
    @DisplayName("Should return Result.failure (400) when ID is null")
    void shouldReturnFailureWhenIdIsNull() {
        Result<Void> result = useCase.execute(null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getStatusCode()).isEqualTo(400);
        assertThat(result.getFirstError()).isEqualTo("ID cannot be null");

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should throw InternalServerErrorException on unexpected repository error")
    void shouldThrowInternalServerErrorExceptionOnGenericError() {
        when(repository.deleteById(id)).thenThrow(new RuntimeException("Database timeout"));

        assertThatThrownBy(() -> useCase.execute(id))
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessageContaining("Error deleting inbox record by ID: " + id);

        verify(repository, times(1)).deleteById(id);
    }
}