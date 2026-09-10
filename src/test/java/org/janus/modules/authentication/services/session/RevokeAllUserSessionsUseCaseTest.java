package org.janus.modules.authentication.services.session;

import org.janus.modules.authentication.application.service.session.RevokeAllUserSessionsUseCase;
import org.janus.modules.authentication.port.out.SessionRepository;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevokeAllUserSessionsUseCaseTest {

    @Mock
    private SessionRepository repository;

    @InjectMocks
    private RevokeAllUserSessionsUseCase useCase;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Validation Scenarios")
    class ValidationScenarios {

        @Test
        @DisplayName("Should return 400 Bad Request when userId is null")
        void shouldReturn400WhenUserIdIsNull() {
            Result<Integer> result = useCase.execute(null);

            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(400, result.getStatusCode());
            assertTrue(result.getFirstError().contains("User ID cannot be null"));
            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should return count of revoked sessions when user has active sessions")
        void shouldRevokeAllSessionsSuccessfully() {
            int expectedRevokedCount = 3;
            when(repository.revokeAllByUserId(userId)).thenReturn(expectedRevokedCount);

            Result<Integer> result = useCase.execute(userId);

            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertEquals(200, result.getStatusCode());
            assertEquals(expectedRevokedCount, result.getValue());

            verify(repository, times(1)).revokeAllByUserId(userId);
        }

        @Test
        @DisplayName("Should return 0 when user has no active sessions to revoke")
        void shouldReturnZeroWhenNoActiveSessionsFound() {
            when(repository.revokeAllByUserId(userId)).thenReturn(0);

            Result<Integer> result = useCase.execute(userId);

            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertEquals(200, result.getStatusCode());
            assertEquals(0, result.getValue());

            verify(repository, times(1)).revokeAllByUserId(userId);
        }
    }

    @Nested
    @DisplayName("Exception Scenarios")
    class ExceptionScenarios {

        @Test
        @DisplayName("Should delegate to DatabaseConstraintHandler on DataIntegrityViolationException")
        void shouldHandleDataIntegrityViolationException() {
            when(repository.revokeAllByUserId(userId))
                    .thenThrow(new DataIntegrityViolationException("Constraint violation error"));

            Result<Integer> result = useCase.execute(userId);

            assertNotNull(result);
            assertFalse(result.isSuccess());
            verify(repository, times(1)).revokeAllByUserId(userId);
        }

        @Test
        @DisplayName("Should throw InternalServerErrorException on generic exception")
        void shouldThrowInternalServerErrorExceptionOnGenericError() {
            when(repository.revokeAllByUserId(userId))
                    .thenThrow(new RuntimeException("Database timeout"));

            InternalServerErrorException exception = assertThrows(
                    InternalServerErrorException.class,
                    () -> useCase.execute(userId)
            );

            assertEquals("Database timeout", exception.getMessage());
            verify(repository, times(1)).revokeAllByUserId(userId);
        }
    }
}