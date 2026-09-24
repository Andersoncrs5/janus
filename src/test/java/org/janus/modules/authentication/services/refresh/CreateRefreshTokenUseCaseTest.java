package org.janus.modules.authentication.services.refresh;

import org.janus.modules.authentication.application.dto.refreshToken.request.CreateRefreshTokenDTO;
import org.janus.modules.authentication.application.mapper.RefreshTokenMapper;
import org.janus.modules.authentication.application.service.refreshToken.CreateRefreshTokenUseCase;
import org.janus.modules.authentication.domain.entity.RefreshTokenEntity;
import org.janus.modules.authentication.port.out.RefreshTokenRepository;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.properties.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateRefreshTokenUseCaseTest {

    @Mock
    private RefreshTokenRepository repository;

    @Mock
    private RefreshTokenMapper mapper;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JwtProperties properties;

    @InjectMocks
    private CreateRefreshTokenUseCase useCase;

    private CreateRefreshTokenDTO dto;
    private UUID sessionId;
    private UUID userId;
    private long mockExpiresAt;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        dto = new CreateRefreshTokenDTO(sessionId, userId);
        mockExpiresAt = 2592000L;
    }

    @Nested
    @DisplayName("Validation Scenarios")
    class ValidationScenarios {

        @Test
        @DisplayName("Should return 400 Bad Request when DTO is null")
        void shouldReturn400WhenDtoIsNull() {
            Result<RefreshTokenEntity> result = useCase.execute(null);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("Refresh token data cannot be null");

            verifyNoInteractions(repository, mapper, properties);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when sessionId is null")
        void shouldReturn400WhenSessionIdIsNull() {
            CreateRefreshTokenDTO invalidDto = new CreateRefreshTokenDTO(null, userId);

            Result<RefreshTokenEntity> result = useCase.execute(invalidDto);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("Session ID cannot be null");

            verifyNoInteractions(repository, mapper, properties);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when userId is null")
        void shouldReturn400WhenUserIdIsNull() {
            CreateRefreshTokenDTO invalidDto = new CreateRefreshTokenDTO(sessionId, null);

            Result<RefreshTokenEntity> result = useCase.execute(invalidDto);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("User ID cannot be null");

            verifyNoInteractions(repository, mapper, properties);
        }
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should create refresh token successfully using mapper and verify execution order")
        void shouldCreateRefreshTokenSuccessfully() {
            RefreshTokenEntity mappedEntity = RefreshTokenEntity.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .build();

            when(mapper.toEntity(dto)).thenReturn(mappedEntity);
            when(properties.exp().refresh()).thenReturn(mockExpiresAt);
            when(repository.insert(any(RefreshTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Result<RefreshTokenEntity> result = useCase.execute(dto);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(201);

            RefreshTokenEntity saved = result.getValue();
            assertThat(saved).isNotNull();
            assertThat(saved.getSessionId()).isEqualTo(sessionId);
            assertThat(saved.getUserId()).isEqualTo(userId);
            assertThat(saved.getIsUsed()).isFalse();
            assertThat(saved.getIsRevoked()).isFalse();
            assertThat(saved.getTokenHash()).isNotBlank();

            InOrder inOrder = inOrder(mapper, properties.exp(), repository);
            inOrder.verify(mapper).toEntity(dto);
            inOrder.verify(properties.exp()).refresh();
            inOrder.verify(repository).insert(saved);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should create refresh token when mapper returns null and verify execution order")
        void shouldCreateRefreshTokenWhenMapperReturnsNull() {
            when(mapper.toEntity(dto)).thenReturn(null);
            when(properties.exp().refresh()).thenReturn(mockExpiresAt);
            when(repository.insert(any(RefreshTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Result<RefreshTokenEntity> result = useCase.execute(dto);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(201);

            RefreshTokenEntity saved = result.getValue();
            assertThat(saved).isNotNull();
            assertThat(saved.getSessionId()).isEqualTo(sessionId);
            assertThat(saved.getUserId()).isEqualTo(userId);
            assertThat(saved.getIsUsed()).isFalse();
            assertThat(saved.getIsRevoked()).isFalse();
            assertThat(saved.getTokenHash()).isNotBlank();

            InOrder inOrder = inOrder(mapper, properties.exp(), repository);
            inOrder.verify(mapper).toEntity(dto);
            inOrder.verify(properties.exp()).refresh();
            inOrder.verify(repository).insert(any(RefreshTokenEntity.class));
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Data Integrity Exception Scenarios")
    class DataIntegrityScenarios {

        @BeforeEach
        void setupDefaults() {
            when(mapper.toEntity(dto)).thenReturn(new RefreshTokenEntity());
            when(properties.exp().refresh()).thenReturn(mockExpiresAt);
        }

        @Test
        @DisplayName("Should return 404 Not Found when fk_refresh_tokens_session fails")
        void shouldReturn404WhenSessionNotFound() {
            String errorMsg = "Constraint failure: fk_refresh_tokens_session";
            when(repository.insert(any())).thenThrow(new DataIntegrityViolationException(errorMsg));

            Result<RefreshTokenEntity> result = useCase.execute(dto);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(404);
            assertThat(result.getFirstError()).contains("Session not found with id: '" + sessionId + "'");

            InOrder inOrder = inOrder(mapper, properties.exp(), repository);
            inOrder.verify(mapper).toEntity(dto);
            inOrder.verify(properties.exp()).refresh();
            inOrder.verify(repository).insert(any(RefreshTokenEntity.class));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should return 404 Not Found when fk_refresh_tokens_user fails")
        void shouldReturn404WhenUserNotFound() {
            String errorMsg = "Constraint failure: fk_refresh_tokens_user";
            when(repository.insert(any())).thenThrow(new DataIntegrityViolationException(errorMsg));

            Result<RefreshTokenEntity> result = useCase.execute(dto);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(404);
            assertThat(result.getFirstError()).contains("User not found with id: '" + userId + "'");

            InOrder inOrder = inOrder(mapper, properties.exp(), repository);
            inOrder.verify(mapper).toEntity(dto);
            inOrder.verify(properties.exp()).refresh();
            inOrder.verify(repository).insert(any(RefreshTokenEntity.class));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should return 409 Conflict when uk_refresh_tokens_token_hash fails")
        void shouldReturn409WhenTokenHashExists() {
            String errorMsg = "Constraint failure: uk_refresh_tokens_token_hash";
            when(repository.insert(any())).thenThrow(new DataIntegrityViolationException(errorMsg));

            Result<RefreshTokenEntity> result = useCase.execute(dto);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(409);
            assertThat(result.getFirstError()).contains("Refresh token hash already exists");

            InOrder inOrder = inOrder(mapper, properties.exp(), repository);
            inOrder.verify(mapper).toEntity(dto);
            inOrder.verify(properties.exp()).refresh();
            inOrder.verify(repository).insert(any(RefreshTokenEntity.class));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should return 400 Bad Request when ck_refresh_tokens_token_hash_not_empty fails")
        void shouldReturn400WhenTokenHashEmpty() {
            String errorMsg = "Constraint failure: ck_refresh_tokens_token_hash_not_empty";
            when(repository.insert(any())).thenThrow(new DataIntegrityViolationException(errorMsg));

            Result<RefreshTokenEntity> result = useCase.execute(dto);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("Token hash cannot be empty");

            InOrder inOrder = inOrder(mapper, properties.exp(), repository);
            inOrder.verify(mapper).toEntity(dto);
            inOrder.verify(properties.exp()).refresh();
            inOrder.verify(repository).insert(any(RefreshTokenEntity.class));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should return 400 Bad Request when ck_refresh_tokens_expires_after_created fails")
        void shouldReturn400WhenExpiresBeforeCreated() {
            String errorMsg = "Constraint failure: ck_refresh_tokens_expires_after_created";
            when(repository.insert(any())).thenThrow(new DataIntegrityViolationException(errorMsg));

            Result<RefreshTokenEntity> result = useCase.execute(dto);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("Expiration date (expiresAt) must be in the future relative to creation time");

            InOrder inOrder = inOrder(mapper, properties.exp(), repository);
            inOrder.verify(mapper).toEntity(dto);
            inOrder.verify(properties.exp()).refresh();
            inOrder.verify(repository).insert(any(RefreshTokenEntity.class));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should return 400 Bad Request when ck_refresh_tokens_version fails")
        void shouldReturn400WhenVersionIsNegative() {
            String errorMsg = "Constraint failure: ck_refresh_tokens_version";
            when(repository.insert(any())).thenThrow(new DataIntegrityViolationException(errorMsg));

            Result<RefreshTokenEntity> result = useCase.execute(dto);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("Refresh token version cannot be negative");

            InOrder inOrder = inOrder(mapper, properties.exp(), repository);
            inOrder.verify(mapper).toEntity(dto);
            inOrder.verify(properties.exp()).refresh();
            inOrder.verify(repository).insert(any(RefreshTokenEntity.class));
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Unexpected Error Scenarios")
    class UnexpectedErrorScenarios {

        @Test
        @DisplayName("Should throw InternalServerErrorException when repository throws unexpected exception")
        void shouldThrowInternalServerErrorExceptionOnGenericError() {
            when(mapper.toEntity(dto)).thenReturn(new RefreshTokenEntity());
            when(properties.exp().refresh()).thenReturn(mockExpiresAt);
            when(repository.insert(any())).thenThrow(new RuntimeException("Database offline"));

            assertThatThrownBy(() -> useCase.execute(dto))
                    .isInstanceOf(InternalServerErrorException.class)
                    .hasMessageContaining("Database offline");

            InOrder inOrder = inOrder(mapper, properties.exp(), repository);
            inOrder.verify(mapper).toEntity(dto);
            inOrder.verify(properties.exp()).refresh();
            inOrder.verify(repository).insert(any(RefreshTokenEntity.class));
            inOrder.verifyNoMoreInteractions();
        }
    }
}