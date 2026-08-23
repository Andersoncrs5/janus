package org.janus.modules.reliability.service.inbox;

import org.janus.modules.reliability.application.dto.inbox.request.CreateInboxDTO;
import org.janus.modules.reliability.application.mapper.InboxMapper;
import org.janus.modules.reliability.application.service.inbox.CreateInboxUseCase;
import org.janus.modules.reliability.domain.entity.InboxEntity;
import org.janus.modules.reliability.port.out.InboxRepository;
import org.janus.shared.domain.enums.InboxStatusEnum;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateInboxUseCaseTest {

    @Mock
    private InboxRepository repository;

    @Mock
    private InboxMapper mapper;

    @InjectMocks
    private CreateInboxUseCase useCase;

    private CreateInboxDTO dto;
    private InboxEntity entity;

    @BeforeEach
    void setUp() {
        dto = new CreateInboxDTO(
                "order-created-key-123",
                "order-processing-group",
                InboxStatusEnum.PROCESSING,
                "{\"status\":\"success\"}",
                200
        );

        entity = new InboxEntity();
        entity.setId(UUID.randomUUID());
        entity.setMessageKey(dto.messageKey());
        entity.setConsumerGroup(dto.consumerGroup());
        entity.setStatus(dto.status());
        entity.setResponsePayload(dto.responsePayload());
        entity.setResponseCode(dto.responseCode());
    }

    @Test
    @DisplayName("Should create inbox record successfully when valid data is provided")
    void shouldCreateInboxSuccessfully() {
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.insert(entity)).thenReturn(entity);

        Result<InboxEntity> result = useCase.execute(dto);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(entity.getId(), result.getData().getId());
        assertEquals("order-created-key-123", result.getData().getMessageKey());
        assertEquals("order-processing-group", result.getData().getConsumerGroup());

        verify(mapper, times(1)).toEntity(dto);
        verify(repository, times(1)).insert(entity);
    }

    @Test
    @DisplayName("Should return 409 conflict when unique constraint uk_inbox_message_group is violated")
    void shouldReturnConflictWhenUniqueConstraintIsViolated() {
        when(mapper.toEntity(dto)).thenReturn(entity);

        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Key (message_key, consumer_group)=(order-created-key-123, order-processing-group) already exists. Constraint: uk_inbox_message_group"
        );
        when(repository.insert(any())).thenThrow(exception);

        Result<InboxEntity> result = useCase.execute(dto);

        assertFalse(result.isSuccess());
        assertEquals(409, result.getStatusCode());
        assertTrue(result.getMessage().isPresent());
        assertEquals(
                "Inbox already exists with message key: 'order-created-key-123' and group: 'order-processing-group'",
                result.getMessage().get()
        );

        verify(mapper, times(1)).toEntity(dto);
        verify(repository, times(1)).insert(entity);
    }

    @Test
    @DisplayName("Should handle generic DataIntegrityViolationException when exception message is null")
    void shouldHandleDataIntegrityViolationWhenMessageIsNull() {
        when(mapper.toEntity(dto)).thenReturn(entity);

        DataIntegrityViolationException exception = new DataIntegrityViolationException(null);
        when(repository.insert(any())).thenThrow(exception);

        Result<InboxEntity> result = useCase.execute(dto);

        assertFalse(result.isSuccess());
        verify(mapper, times(1)).toEntity(dto);
        verify(repository, times(1)).insert(entity);
    }

    @Test
    @DisplayName("Should handle generic DataIntegrityViolationException when constraint is unknown")
    void shouldHandleDataIntegrityViolationForUnknownConstraint() {
        when(mapper.toEntity(dto)).thenReturn(entity);

        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Value too long for column 'message_key'. Constraint: fk_other_table"
        );
        when(repository.insert(any())).thenThrow(exception);

        Result<InboxEntity> result = useCase.execute(dto);

        assertFalse(result.isSuccess());
        verify(mapper, times(1)).toEntity(dto);
        verify(repository, times(1)).insert(entity);
    }

    @Test
    @DisplayName("Should throw InternalServerErrorException on unexpected generic exception")
    void shouldThrowInternalServerErrorExceptionOnGenericError() {
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.insert(any())).thenThrow(new RuntimeException("Database connection timeout"));

        assertThrows(InternalServerErrorException.class, () -> useCase.execute(dto));

        verify(mapper, times(1)).toEntity(dto);
        verify(repository, times(1)).insert(entity);
    }
}