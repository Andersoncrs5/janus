package org.janus.modules.reliability.service.inbox;

import org.janus.modules.reliability.application.service.inbox.FindInboxByIdUseCase;
import org.janus.modules.reliability.domain.entity.InboxEntity;
import org.janus.modules.reliability.port.out.InboxRepository;
import org.janus.shared.domain.enums.InboxStatusEnum;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindInboxByIdUseCaseTest {

    @Mock
    private InboxRepository repository;

    @InjectMocks
    private FindInboxByIdUseCase useCase;

    private UUID id;
    private InboxEntity entity;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        entity = new InboxEntity();
        entity.setId(id);
        entity.setMessageKey("order-created-123");
        entity.setConsumerGroup("order-processing-group");
        entity.setStatus(org.janus.shared.domain.enums.InboxStatusEnum.PROCESSING);
    }

    @Test
    @DisplayName("Should return Result.success with entity when ID exists")
    void shouldReturnSuccessWithEntityWhenIdExists() {
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        Result<InboxEntity> result = useCase.execute(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getStatusCode()).isEqualTo(200);
        assertThat(result.getData()).isNotNull().isEqualTo(entity);
        assertThat(result.getData().getId()).isEqualTo(id);

        verify(repository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Should return Result.notFound (404) when ID does not exist")
    void shouldReturnNotFoundWhenIdDoesNotExist() {
        when(repository.findById(id)).thenReturn(Optional.empty());

        Result<InboxEntity> result = useCase.execute(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getStatusCode()).isEqualTo(404);
        assertThat(result.getFirstError()).isEqualTo("Inbox record not found with ID: " + id);

        verify(repository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Should return Result.failure (400) when ID is null")
    void shouldReturnFailureWhenIdIsNull() {
        Result<InboxEntity> result = useCase.execute(null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getStatusCode()).isEqualTo(400);
        assertThat(result.getFirstError()).isEqualTo("ID cannot be null");

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should throw InternalServerErrorException on unexpected repository error")
    void shouldThrowInternalServerErrorExceptionOnGenericError() {
        when(repository.findById(id)).thenThrow(new RuntimeException("Database timeout"));

        assertThatThrownBy(() -> useCase.execute(id))
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessageContaining("Error searching inbox record by ID: " + id);

        verify(repository, times(1)).findById(id);
    }
}