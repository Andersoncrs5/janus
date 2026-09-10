package org.janus.modules.authentication.services.session;

import org.janus.modules.authentication.application.dto.session.request.CreateSessionDTO;
import org.janus.modules.authentication.application.mapper.SessionMapper;
import org.janus.modules.authentication.application.service.session.CreateSessionUseCase;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateSessionUseCaseTest {

    @Mock
    private SessionRepository repository;

    @Mock
    private SessionMapper mapper;

    @InjectMocks
    private CreateSessionUseCase useCase;

    private CreateSessionDTO createSessionDTO;
    private SessionEntity sessionEntity;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();

        createSessionDTO = new CreateSessionDTO(
                userId,
                "127.0.0.1",
                "Mozilla/5.0",
                60L
        );

        sessionEntity = SessionEntity.builder()
                .userId(userId)
                .ipAddress("127.0.0.1")
                .userAgent("Mozilla/5.0")
                .isRevoked(false)
                .build();
    }

    @Nested
    @DisplayName("Validation Scenarios")
    class ValidationScenarios {

        @Test
        @DisplayName("Should return 400 Bad Request when DTO is null")
        void shouldReturn400WhenDtoIsNull() {
            // Act
            Result<SessionEntity> result = useCase.execute(null);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(400, result.getStatusCode());
            assertTrue(result.getFirstError().contains("Session data cannot be null"));
            verifyNoInteractions(mapper, repository);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when userId is null")
        void shouldReturn400WhenUserIdIsNull() {
            // Arrange
            CreateSessionDTO invalidDto = new CreateSessionDTO(
                    null,
                    "127.0.0.1",
                    "Mozilla/5.0",
                    60L
            );

            // Act
            Result<SessionEntity> result = useCase.execute(invalidDto);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(400, result.getStatusCode());
            assertTrue(result.getFirstError().contains("User ID cannot be null"));
            verifyNoInteractions(mapper, repository);
        }

    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should successfully create a session")
        void shouldCreateSessionSuccessfully() {
            // Arrange
            when(mapper.toEntity(createSessionDTO)).thenReturn(sessionEntity);
            when(repository.insert(sessionEntity)).thenReturn(sessionEntity);

            // Act
            Result<SessionEntity> result = useCase.execute(createSessionDTO);

            // Assert
            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertEquals(201, result.getStatusCode());
            assertEquals(sessionEntity, result.getValue());
            assertNotNull(sessionEntity.getExpiresAt());

            verify(mapper, times(1)).toEntity(createSessionDTO);
            verify(repository, times(1)).insert(sessionEntity);
        }

        @Test
        @DisplayName("Should set isRevoked to false when mapped entity has null isRevoked")
        void shouldSetIsRevokedToFalseWhenNull() {
            // Arrange
            SessionEntity entityWithNullRevoked = SessionEntity.builder()
                    .userId(createSessionDTO.userId())
                    .isRevoked(null)
                    .build();

            when(mapper.toEntity(createSessionDTO)).thenReturn(entityWithNullRevoked);
            when(repository.insert(entityWithNullRevoked)).thenReturn(entityWithNullRevoked);

            // Act
            Result<SessionEntity> result = useCase.execute(createSessionDTO);

            // Assert
            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertFalse(entityWithNullRevoked.getIsRevoked());
            verify(repository, times(1)).insert(entityWithNullRevoked);
        }
    }

    @Nested
    @DisplayName("Data Integrity Exception Scenarios")
    class DataIntegrityScenarios {

        @Test
        @DisplayName("Should return 404 Not Found when foreign key user constraint fails (fk_sessions_user)")
        void shouldReturn404WhenUserDoesNotExist() {
            // Arrange
            String errorMessage = "Key (user_id)=(...) is not present in table users. Constraint: fk_sessions_user";
            when(mapper.toEntity(createSessionDTO)).thenReturn(sessionEntity);
            when(repository.insert(sessionEntity))
                    .thenThrow(new DataIntegrityViolationException(errorMessage));

            // Act
            Result<SessionEntity> result = useCase.execute(createSessionDTO);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(404, result.getStatusCode());
            assertTrue(result.getFirstError().contains("User not found with id: '" + createSessionDTO.userId() + "'"));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when check constraint ck_sessions_expires_after_created fails")
        void shouldReturn400WhenExpiresBeforeCreated() {
            // Arrange
            String errorMessage = "Constraint violation: ck_sessions_expires_after_created";
            when(mapper.toEntity(createSessionDTO)).thenReturn(sessionEntity);
            when(repository.insert(sessionEntity))
                    .thenThrow(new DataIntegrityViolationException(errorMessage));

            // Act
            Result<SessionEntity> result = useCase.execute(createSessionDTO);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(400, result.getStatusCode());
            assertTrue(result.getFirstError().contains("Expiration date (expiresAt) must be in the future relative to creation time"));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when check constraint ck_sessions_version fails")
        void shouldReturn400WhenVersionIsNegative() {
            // Arrange
            String errorMessage = "Constraint violation: ck_sessions_version";
            when(mapper.toEntity(createSessionDTO)).thenReturn(sessionEntity);
            when(repository.insert(sessionEntity))
                    .thenThrow(new DataIntegrityViolationException(errorMessage));

            // Act
            Result<SessionEntity> result = useCase.execute(createSessionDTO);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
            assertEquals(400, result.getStatusCode());
            assertTrue(result.getFirstError().contains("Session version cannot be negative"));
        }

        @Test
        @DisplayName("Should delegate to DatabaseConstraintHandler when exception message is null")
        void shouldDelegateToHandlerWhenMessageIsNull() {
            // Arrange
            when(mapper.toEntity(createSessionDTO)).thenReturn(sessionEntity);
            when(repository.insert(sessionEntity))
                    .thenThrow(new DataIntegrityViolationException(null));

            // Act
            Result<SessionEntity> result = useCase.execute(createSessionDTO);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
        }

        @Test
        @DisplayName("Should delegate to DatabaseConstraintHandler on unknown constraint")
        void shouldDelegateToHandlerOnUnknownConstraint() {
            // Arrange
            when(mapper.toEntity(createSessionDTO)).thenReturn(sessionEntity);
            when(repository.insert(sessionEntity))
                    .thenThrow(new DataIntegrityViolationException("some_other_constraint"));

            // Act
            Result<SessionEntity> result = useCase.execute(createSessionDTO);

            // Assert
            assertNotNull(result);
            assertFalse(result.isSuccess());
        }
    }

    @Nested
    @DisplayName("Unexpected Error Scenarios")
    class UnexpectedErrorScenarios {

        @Test
        @DisplayName("Should throw InternalServerErrorException when generic exception occurs")
        void shouldThrowInternalServerErrorExceptionOnGenericFailure() {
            // Arrange
            when(mapper.toEntity(createSessionDTO)).thenReturn(sessionEntity);
            when(repository.insert(sessionEntity))
                    .thenThrow(new RuntimeException("Database connection timeout"));

            // Act & Assert
            InternalServerErrorException exception = assertThrows(
                    InternalServerErrorException.class,
                    () -> useCase.execute(createSessionDTO)
            );

            assertEquals("Database connection timeout", exception.getMessage());
        }
    }
}