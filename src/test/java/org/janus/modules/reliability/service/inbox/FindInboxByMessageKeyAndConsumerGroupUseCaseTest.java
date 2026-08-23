package org.janus.modules.reliability.service.inbox;

import org.janus.modules.reliability.application.service.inbox.FindInboxByMessageKeyAndConsumerGroupUseCase;
import org.janus.modules.reliability.domain.entity.InboxEntity;
import org.janus.modules.reliability.port.out.InboxRepository;
import org.janus.shared.domain.enums.InboxStatusEnum;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindInboxByMessageKeyAndConsumerGroupUseCaseTest {

    @Mock
    private InboxRepository repository;

    @InjectMocks
    private FindInboxByMessageKeyAndConsumerGroupUseCase useCase;

    private String messageKey;
    private String consumerGroup;
    private InboxEntity entity;

    @BeforeEach
    void setUp() {
        messageKey = "order-created-123";
        consumerGroup = "order-processing-group";

        entity = new InboxEntity();
        entity.setId(UUID.randomUUID());
        entity.setMessageKey(messageKey);
        entity.setConsumerGroup(consumerGroup);
        entity.setStatus(InboxStatusEnum.PROCESSING);
        entity.setResponseCode(200);
        entity.setResponsePayload("{\"status\":\"ok\"}");
    }

    @Test
    @DisplayName("Should return Optional with InboxEntity when record exists")
    void shouldReturnInboxEntityWhenRecordExists() {
        when(repository.findByMessageKeyAndConsumerGroup(messageKey, consumerGroup))
                .thenReturn(Optional.of(entity));

        Optional<InboxEntity> result = useCase.execute(messageKey, consumerGroup);

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
        assertEquals(messageKey, result.get().getMessageKey());
        assertEquals(consumerGroup, result.get().getConsumerGroup());

        verify(repository, times(1)).findByMessageKeyAndConsumerGroup(messageKey, consumerGroup);
    }

    @Test
    @DisplayName("Should return empty Optional when record does not exist")
    void shouldReturnEmptyOptionalWhenRecordDoesNotExist() {
        when(repository.findByMessageKeyAndConsumerGroup(messageKey, consumerGroup))
                .thenReturn(Optional.empty());

        Optional<InboxEntity> result = useCase.execute(messageKey, consumerGroup);

        assertTrue(result.isEmpty());
        verify(repository, times(1)).findByMessageKeyAndConsumerGroup(messageKey, consumerGroup);
    }

    @ParameterizedTest
    @CsvSource({
            "'', group-1",
            "'   ', group-1",
            "key-1, ''",
            "key-1, '   '"
    })
    @DisplayName("Should return empty Optional without querying repository when parameters are blank")
    void shouldReturnEmptyWhenParametersAreBlank(String key, String group) {
        Optional<InboxEntity> result = useCase.execute(key, group);

        assertTrue(result.isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should return empty Optional without querying repository when messageKey is null")
    void shouldReturnEmptyWhenMessageKeyIsNull() {
        Optional<InboxEntity> result = useCase.execute(null, consumerGroup);

        assertTrue(result.isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should return empty Optional without querying repository when consumerGroup is null")
    void shouldReturnEmptyWhenConsumerGroupIsNull() {
        Optional<InboxEntity> result = useCase.execute(messageKey, null);

        assertTrue(result.isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should throw InternalServerErrorException on unexpected repository error")
    void shouldThrowInternalServerErrorExceptionOnGenericError() {
        when(repository.findByMessageKeyAndConsumerGroup(messageKey, consumerGroup))
                .thenThrow(new RuntimeException("Database connection timeout"));

        assertThrows(
                InternalServerErrorException.class,
                () -> useCase.execute(messageKey, consumerGroup)
        );

        verify(repository, times(1)).findByMessageKeyAndConsumerGroup(messageKey, consumerGroup);
    }
}