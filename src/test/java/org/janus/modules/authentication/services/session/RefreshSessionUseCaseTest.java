package org.janus.modules.authentication.services.session;

import org.janus.modules.authentication.application.dto.session.request.RefreshSessionDTO;
import org.janus.modules.authentication.application.service.session.RefreshSessionUseCase;
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
class RefreshSessionUseCaseTest {

    @Mock
    private SessionRepository repository;

    @InjectMocks
    private RefreshSessionUseCase useCase;

    private UUID sessionId;
    private UUID userId;
    private RefreshSessionDTO validDto;
    private SessionEntity sessionEntity;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        validDto = new RefreshSessionDTO(sessionId, userId, 30L);

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
        @DisplayName("Should return 400 Bad Request when DTO is null")
        void shouldReturn400WhenDtoIsNull() {
            Result<SessionEntity> result = useCase.execute(null);

            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(400, result.getStatusCode());
            assertTrue(result.getFirstError().contains("Session DTO cannot be null"));
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when sessionId is null")
        void shouldReturn400WhenSessionIdIsNull() {
            RefreshSessionDTO dto = new RefreshSessionDTO(null, userId, 30L);

            Result<SessionEntity> result = useCase.execute(dto);

            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(400, result.getStatusCode());
            assertTrue(result.getFirstError().contains("Session ID cannot be null"));
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when userId is null")
        void shouldReturn400WhenUserIdIsNull() {
            RefreshSessionDTO dto = new RefreshSessionDTO(sessionId, null, 30L);

            Result<SessionEntity> result = useCase.execute(dto);

            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(400, result.getStatusCode());
            assertTrue(result.getFirstError().contains("User ID cannot be null"));
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when durationInMinutes is invalid")
        void shouldReturn400WhenDurationIsInvalid() {
            RefreshSessionDTO dto = new RefreshSessionDTO(sessionId, userId, 0L);

            Result<SessionEntity> result = useCase.execute(dto);

            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(400, result.getStatusCode());
            assertTrue(result.getFirstError().contains("Duration in minutes must be greater than zero"));
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should return 404 Not Found when session does not exist")
        void shouldReturn404WhenSessionNotFound() {
            when(repository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.empty());

            Result<SessionEntity> result = useCase.execute(validDto);

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

            Result<SessionEntity> result = useCase.execute(validDto);

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
        @DisplayName("Should refresh session duration successfully and return 200 OK")
        void shouldRefreshSessionSuccessfully() {
            when(repository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.of(sessionEntity));
            when(repository.save(sessionEntity)).thenReturn(sessionEntity);

            Result<SessionEntity> result = useCase.execute(validDto);

            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertEquals(200, result.getStatusCode());
            assertNotNull(result.getValue());

            verify(repository, times(1)).findBySessionIdAndUserId(sessionId, userId);
            verify(repository, times(1)).save(sessionEntity);
        }
    }

    @Nested
    @DisplayName("Exception Scenarios")
    class ExceptionScenarios {

        @Test
        @DisplayName("Should delegate to DatabaseConstraintHandler on DataIntegrityViolationException")
        void shouldHandleDataIntegrityViolationException() {
            when(repository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.of(sessionEntity));
            when(repository.save(sessionEntity)).thenThrow(new DataIntegrityViolationException("Constraint violation"));

            Result<SessionEntity> result = useCase.execute(validDto);

            assertNotNull(result);
            assertFalse(result.isSuccess());
            verify(repository, times(1)).save(sessionEntity);
        }

        @Test
        @DisplayName("Should throw InternalServerErrorException on generic exception")
        void shouldThrowInternalServerErrorExceptionOnGenericError() {
            when(repository.findBySessionIdAndUserId(sessionId, userId)).thenReturn(Optional.of(sessionEntity));
            when(repository.save(sessionEntity)).thenThrow(new RuntimeException("Database error"));

            InternalServerErrorException exception = assertThrows(
                    InternalServerErrorException.class,
                    () -> useCase.execute(validDto)
            );

            assertEquals("Database error", exception.getMessage());
            verify(repository, times(1)).save(sessionEntity);
        }
    }
}