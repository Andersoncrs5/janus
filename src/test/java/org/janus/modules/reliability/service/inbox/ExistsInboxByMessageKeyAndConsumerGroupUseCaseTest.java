package org.janus.modules.reliability.service.inbox;

import org.janus.modules.reliability.application.service.inbox.ExistsInboxByMessageKeyAndConsumerGroupUseCase;
import org.janus.modules.reliability.port.out.InboxRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExistsInboxByMessageKeyAndConsumerGroupUseCaseTest {

    @Mock
    private InboxRepository repository;

    @InjectMocks
    private ExistsInboxByMessageKeyAndConsumerGroupUseCase useCase;

    private String messageKey;
    private String consumerGroup;

    @BeforeEach
    void setUp() {
        messageKey = "order-created-123";
        consumerGroup = "order-processing-group";
    }

    @Test
    @DisplayName("Should return true when inbox record exists")
    void shouldReturnTrueWhenRecordExists() {
        when(repository.existsByMessageKeyAndConsumerGroup(messageKey, consumerGroup))
                .thenReturn(true);

        boolean exists = useCase.execute(messageKey, consumerGroup);

        assertTrue(exists);
        verify(repository, times(1)).existsByMessageKeyAndConsumerGroup(messageKey, consumerGroup);
    }

    @Test
    @DisplayName("Should return false when inbox record does not exist")
    void shouldReturnFalseWhenRecordDoesNotExist() {
        when(repository.existsByMessageKeyAndConsumerGroup(messageKey, consumerGroup))
                .thenReturn(false);

        boolean exists = useCase.execute(messageKey, consumerGroup);

        assertFalse(exists);
        verify(repository, times(1)).existsByMessageKeyAndConsumerGroup(messageKey, consumerGroup);
    }

    @ParameterizedTest
    @CsvSource({
            "'', group-1",
            "'   ', group-1",
            "key-1, ''",
            "key-1, '   '"
    })
    @DisplayName("Should return false without querying repository when parameters are blank")
    void shouldReturnFalseWhenParametersAreBlank(String key, String group) {
        boolean exists = useCase.execute(key, group);

        assertFalse(exists);
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should return false without querying repository when messageKey is null")
    void shouldReturnFalseWhenMessageKeyIsNull() {
        boolean exists = useCase.execute(null, consumerGroup);

        assertFalse(exists);
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should return false without querying repository when consumerGroup is null")
    void shouldReturnFalseWhenConsumerGroupIsNull() {
        boolean exists = useCase.execute(messageKey, null);

        assertFalse(exists);
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should throw InternalServerErrorException on unexpected repository error")
    void shouldThrowInternalServerErrorExceptionOnGenericError() {
        when(repository.existsByMessageKeyAndConsumerGroup(messageKey, consumerGroup))
                .thenThrow(new RuntimeException("Database connection timeout"));

        assertThrows(
                InternalServerErrorException.class,
                () -> useCase.execute(messageKey, consumerGroup)
        );

        verify(repository, times(1)).existsByMessageKeyAndConsumerGroup(messageKey, consumerGroup);
    }
}