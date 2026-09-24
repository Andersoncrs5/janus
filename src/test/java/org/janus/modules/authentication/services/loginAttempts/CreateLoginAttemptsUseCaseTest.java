package org.janus.modules.authentication.services.loginAttempts;

import org.janus.modules.authentication.application.dto.loginAttempts.request.CreateLoginAttemptDTO;
import org.janus.modules.authentication.application.mapper.LoginAttemptsMapper;
import org.janus.modules.authentication.application.service.loginAttempts.CreateLoginAttemptsUseCase;
import org.janus.modules.authentication.domain.entity.LoginAttemptEntity;
import org.janus.modules.authentication.port.out.LoginAttemptRepository;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class CreateLoginAttemptsUseCaseTest {

    @Mock
    private LoginAttemptRepository repository;

    @Mock
    private LoginAttemptsMapper mapper;

    @InjectMocks
    private CreateLoginAttemptsUseCase useCase;

    private CreateLoginAttemptDTO dto;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        dto = mock(CreateLoginAttemptDTO.class);
    }

    @Nested
    @DisplayName("Validation Scenarios")
    class ValidationScenarios {

        @Test
        @DisplayName("Should return 400 Bad Request when DTO is null")
        void shouldReturn400WhenDtoIsNull() {
            Result<LoginAttemptEntity> result = useCase.execute(null, userId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("Login Attempt data cannot be null");

            verifyNoInteractions(repository, mapper);
        }
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should successfully create a login attempt and trim/lowercase email")
        void shouldCreateLoginAttemptAndNormalizeEmailSuccessfully() {
            LoginAttemptEntity mappedEntity = new LoginAttemptEntity();
            mappedEntity.setEmailAttempted("  USER@EXAMPLE.COM  ");

            when(mapper.toEntity(dto)).thenReturn(mappedEntity);
            when(repository.insert(mappedEntity)).thenReturn(mappedEntity);

            Result<LoginAttemptEntity> result = useCase.execute(dto, userId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(201);
            assertThat(result.getValue()).isNotNull();
            assertThat(result.getValue().getUserId()).isEqualTo(userId);
            assertThat(result.getValue().getEmailAttempted()).isEqualTo("user@example.com");

            InOrder inOrder = inOrder(mapper, repository);
            inOrder.verify(mapper).toEntity(dto);
            inOrder.verify(repository).insert(mappedEntity);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should successfully create a login attempt when email is null")
        void shouldCreateLoginAttemptWhenEmailIsNull() {
            LoginAttemptEntity mappedEntity = new LoginAttemptEntity();
            mappedEntity.setEmailAttempted(null);

            when(mapper.toEntity(dto)).thenReturn(mappedEntity);
            when(repository.insert(mappedEntity)).thenReturn(mappedEntity);

            Result<LoginAttemptEntity> result = useCase.execute(dto, userId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(201);
            assertThat(result.getValue()).isNotNull();
            assertThat(result.getValue().getUserId()).isEqualTo(userId);
            assertThat(result.getValue().getEmailAttempted()).isNull();

            InOrder inOrder = inOrder(mapper, repository);
            inOrder.verify(mapper).toEntity(dto);
            inOrder.verify(repository).insert(mappedEntity);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Data Integrity Exception Scenarios")
    class DataIntegrityScenarios {

        private LoginAttemptEntity entity;

        @BeforeEach
        void setupDefaults() {
            entity = new LoginAttemptEntity();
            when(mapper.toEntity(dto)).thenReturn(entity);
        }

        @Test
        @DisplayName("Should return 404 Not Found when fk_login_attempts_user constraint fails")
        void shouldReturn404WhenUserNotFound() {
            String errorMsg = "Constraint failure: fk_login_attempts_user";
            when(repository.insert(any())).thenThrow(new DataIntegrityViolationException(errorMsg));

            Result<LoginAttemptEntity> result = useCase.execute(dto, userId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(404);
            assertThat(result.getFirstError()).contains("User not found with id: '" + userId + "'");
        }

        @Test
        @DisplayName("Should return 400 Bad Request when ck_login_attempts_email_not_empty constraint fails")
        void shouldReturn400WhenEmailEmpty() {
            String errorMsg = "Constraint failure: ck_login_attempts_email_not_empty";
            when(repository.insert(any())).thenThrow(new DataIntegrityViolationException(errorMsg));

            Result<LoginAttemptEntity> result = useCase.execute(dto, userId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("Email attempted cannot be empty or blank");
        }

        @Test
        @DisplayName("Should return 400 Bad Request when ck_login_attempts_email_lowercase constraint fails")
        void shouldReturn400WhenEmailNotLowercase() {
            String errorMsg = "Constraint failure: ck_login_attempts_email_lowercase";
            when(repository.insert(any())).thenThrow(new DataIntegrityViolationException(errorMsg));

            Result<LoginAttemptEntity> result = useCase.execute(dto, userId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("Email attempted must be in lowercase");
        }

        @Test
        @DisplayName("Should return 400 Bad Request when ck_login_attempts_identifier_required constraint fails")
        void shouldReturn400WhenIdentifierRequired() {
            String errorMsg = "Constraint failure: ck_login_attempts_identifier_required";
            when(repository.insert(any())).thenThrow(new DataIntegrityViolationException(errorMsg));

            Result<LoginAttemptEntity> result = useCase.execute(dto, userId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("At least user ID or email attempted must be provided");
        }

        @Test
        @DisplayName("Should return 400 Bad Request when ck_login_attempts_failure_reason constraint fails")
        void shouldReturn400WhenFailureReasonInvalid() {
            String errorMsg = "Constraint failure: ck_login_attempts_failure_reason";
            when(repository.insert(any())).thenThrow(new DataIntegrityViolationException(errorMsg));

            Result<LoginAttemptEntity> result = useCase.execute(dto, userId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("Failure reason is required for failed attempts and must be null for successful ones");
        }

        @Test
        @DisplayName("Should return 400 Bad Request when ck_login_attempts_ip_not_empty constraint fails")
        void shouldReturn400WhenIpEmpty() {
            String errorMsg = "Constraint failure: ck_login_attempts_ip_not_empty";
            when(repository.insert(any())).thenThrow(new DataIntegrityViolationException(errorMsg));

            Result<LoginAttemptEntity> result = useCase.execute(dto, userId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("IP address cannot be empty or blank");
        }

        @Test
        @DisplayName("Should return 400 Bad Request when ck_login_attempts_user_agent_not_empty constraint fails")
        void shouldReturn400WhenUserAgentEmpty() {
            String errorMsg = "Constraint failure: ck_login_attempts_user_agent_not_empty";
            when(repository.insert(any())).thenThrow(new DataIntegrityViolationException(errorMsg));

            Result<LoginAttemptEntity> result = useCase.execute(dto, userId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).contains("User agent cannot be empty or blank");
        }

        @Test
        @DisplayName("Should delegate to DatabaseConstraintHandler when exception message is null")
        void shouldDelegateToHandlerWhenMessageIsNull() {
            when(repository.insert(any())).thenThrow(new DataIntegrityViolationException(null));

            Result<LoginAttemptEntity> result = useCase.execute(dto, userId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("Should delegate to DatabaseConstraintHandler on unknown constraint")
        void shouldDelegateToHandlerOnUnknownConstraint() {
            when(repository.insert(any())).thenThrow(new DataIntegrityViolationException("some_other_constraint"));

            Result<LoginAttemptEntity> result = useCase.execute(dto, userId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
        }
    }

    @Nested
    @DisplayName("Unexpected Error Scenarios")
    class UnexpectedErrorScenarios {

        @Test
        @DisplayName("Should throw InternalServerErrorException when generic runtime exception occurs")
        void shouldThrowInternalServerErrorExceptionOnGenericFailure() {
            LoginAttemptEntity entity = new LoginAttemptEntity();
            when(mapper.toEntity(dto)).thenReturn(entity);
            when(repository.insert(any())).thenThrow(new RuntimeException("Database timeout"));

            assertThatThrownBy(() -> useCase.execute(dto, userId))
                    .isInstanceOf(InternalServerErrorException.class)
                    .hasMessageContaining("Error executing INSERT for table: login_attempts");
        }
    }
}