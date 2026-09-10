package org.janus.modules.authentication.services.session;

import org.janus.modules.authentication.application.service.session.RevokeSessionByIdUseCase;
import org.janus.modules.authentication.domain.entity.SessionEntity;
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

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevokeSessionByIdUseCaseTest {

    @Mock
    private SessionRepository repository;

    @InjectMocks
    private RevokeSessionByIdUseCase useCase;

    private UUID sessionId;
    private UUID userId;
    private SessionEntity sessionEntity;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        userId = UUID.randomUUID();

        sessionEntity = SessionEntity.builder()
                .id(sessionId)
                .userId(userId)
                .ipAddress("127.0.0.1")
                .userAgent("Mozilla/5.0")
                .isRevoked(false)
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build();
    }

    @Nested
    @DisplayName("Validation Scenarios")
    class ValidationScenarios {

        @Test
        @DisplayName("Should return 400 Bad Request when sessionId is null")
        void shouldReturn400WhenSessionIdIsNull() {
            Result<Void> result = useCase.execute(null, userId);

            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(400, result.getStatusCode());
            assertTrue(result.getFirstError().contains("Session ID cannot be null"));
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when userId is null")
        void shouldReturn400WhenUserIdIsNull() {
            Result<Void> result = useCase.execute(sessionId, null);

            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(400, result.getStatusCode());
            assertTrue(result.getFirstError().contains("User ID cannot be null"));
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should return 404 Not Found when session does not exist")
        void shouldReturn404WhenSessionNotFound() {
            when(repository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.empty());

            Result<Void> result = useCase.execute(sessionId, userId);

            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(404, result.getStatusCode());
            assertTrue(result.getFirstError().contains("Session not found"));
            verify(repository, times(1)).findBySessionIdAndUserId(sessionId, userId);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when session is already revoked")
        void shouldReturn400WhenSessionAlreadyRevoked() {
            sessionEntity.setIsRevoked(true);
            when(repository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.of(sessionEntity));

            Result<Void> result = useCase.execute(sessionId, userId);

            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(400, result.getStatusCode());
            assertTrue(result.getFirstError().contains("Session is already revoked"));
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should revoke session successfully and return 204 No Content")
        void shouldRevokeSessionSuccessfully() {
            when(repository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.of(sessionEntity));
            when(repository.save(sessionEntity)).thenReturn(sessionEntity);

            Result<Void> result = useCase.execute(sessionId, userId);

            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertEquals(204, result.getStatusCode());
            assertTrue(sessionEntity.getIsRevoked());

            verify(repository, times(1)).findBySessionIdAndUserId(sessionId, userId);
            verify(repository, times(1)).save(sessionEntity);
        }
    }

    @Nested
    @DisplayName("Exception Handling Scenarios")
    class ExceptionScenarios {

        @Test
        @DisplayName("Should delegate to DatabaseConstraintHandler on DataIntegrityViolationException")
        void shouldHandleDataIntegrityViolationException() {
            when(repository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.of(sessionEntity));
            when(repository.save(sessionEntity)).thenThrow(new DataIntegrityViolationException("Database constraint failure"));

            Result<Void> result = useCase.execute(sessionId, userId);

            assertNotNull(result);
            assertFalse(result.isSuccess());
            verify(repository, times(1)).save(sessionEntity);
        }

        @Test
        @DisplayName("Should throw InternalServerErrorException on generic exception")
        void shouldThrowInternalServerErrorExceptionOnGenericError() {
            when(repository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.of(sessionEntity));
            when(repository.save(sessionEntity)).thenThrow(new RuntimeException("Connection closed"));

            InternalServerErrorException exception = assertThrows(
                    InternalServerErrorException.class,
                    () -> useCase.execute(sessionId, userId)
            );

            assertEquals("Connection closed", exception.getMessage());
        }
    }
}