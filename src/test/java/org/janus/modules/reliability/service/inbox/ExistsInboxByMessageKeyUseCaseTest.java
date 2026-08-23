package org.janus.modules.reliability.service.inbox;

import org.janus.modules.reliability.application.service.inbox.ExistsInboxByMessageKeyUseCase;
import org.janus.modules.reliability.port.out.InboxRepository;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExistsInboxByMessageKeyUseCaseTest {

    @Mock
    private InboxRepository repository;

    @InjectMocks
    private ExistsInboxByMessageKeyUseCase useCase;

    private String messageKey;

    @BeforeEach
    void setUp() {
        messageKey = "order-created-123";
    }

    @Test
    @DisplayName("Should return Result.success(true) when inbox record exists")
    void shouldReturnSuccessTrueWhenRecordExists() {
        when(repository.existsByMessageKey(messageKey)).thenReturn(true);

        Result<Boolean> result = useCase.execute(messageKey);

        assertTrue(result.isSuccess());
        assertEquals(200, result.getStatusCode());
        assertTrue(result.getData());
        verify(repository, times(1)).existsByMessageKey(messageKey);
    }

    @Test
    @DisplayName("Should return Result.success(false) when inbox record does not exist")
    void shouldReturnSuccessFalseWhenRecordDoesNotExist() {
        when(repository.existsByMessageKey(messageKey)).thenReturn(false);

        Result<Boolean> result = useCase.execute(messageKey);

        assertTrue(result.isSuccess());
        assertEquals(200, result.getStatusCode());
        assertFalse(result.getData());
        verify(repository, times(1)).existsByMessageKey(messageKey);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Should return Result.failure(400) when messageKey is null, empty or blank")
    void shouldReturnFailureWhenMessageKeyIsInvalid(String invalidKey) {
        Result<Boolean> result = useCase.execute(invalidKey);

        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertEquals(400, result.getStatusCode());
        assertEquals("Message key cannot be null or empty", result.getFirstError());
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should throw InternalServerErrorException on unexpected repository error")
    void shouldThrowInternalServerErrorExceptionOnGenericError() {
        when(repository.existsByMessageKey(messageKey))
                .thenThrow(new RuntimeException("Database connection failure"));

        assertThrows(
                InternalServerErrorException.class,
                () -> useCase.execute(messageKey)
        );

        verify(repository, times(1)).existsByMessageKey(messageKey);
    }
}