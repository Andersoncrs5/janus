package org.janus.modules.authentication.services.session;

import org.janus.modules.authentication.application.service.session.FindActiveSessionsByUserIdUseCase;
import org.janus.modules.authentication.domain.entity.SessionEntity;
import org.janus.modules.authentication.port.out.SessionRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindActiveSessionsByUserIdUseCaseTest {

    @Mock
    private SessionRepository repository;

    @InjectMocks
    private FindActiveSessionsByUserIdUseCase useCase;

    private UUID userId;
    private SessionEntity sessionEntity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        sessionEntity = SessionEntity.builder()
                .id(UUID.randomUUID())
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
        @DisplayName("Should return 400 Bad Request when userId is null")
        void shouldReturn400WhenUserIdIsNull() {
            Result<List<SessionEntity>> result = useCase.execute(null);

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
        @DisplayName("Should return list of active sessions when active sessions exist for user")
        void shouldReturnActiveSessionsWhenFound() {
            List<SessionEntity> activeSessions = List.of(sessionEntity);
            when(repository.findActiveByUserId(userId)).thenReturn(activeSessions);

            Result<List<SessionEntity>> result = useCase.execute(userId);

            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertEquals(200, result.getStatusCode());
            assertNotNull(result.getValue());
            assertEquals(1, result.getValue().size());
            assertEquals(sessionEntity.getId(), result.getValue().get(0).getId());

            verify(repository, times(1)).findActiveByUserId(userId);
        }

        @Test
        @DisplayName("Should return empty list when no active sessions exist for user")
        void shouldReturnEmptyListWhenNoActiveSessionsFound() {
            when(repository.findActiveByUserId(userId)).thenReturn(Collections.emptyList());

            Result<List<SessionEntity>> result = useCase.execute(userId);

            assertNotNull(result);
            assertTrue(result.isSuccess());
            assertEquals(200, result.getStatusCode());
            assertNotNull(result.getValue());
            assertTrue(result.getValue().isEmpty());

            verify(repository, times(1)).findActiveByUserId(userId);
        }
    }
}