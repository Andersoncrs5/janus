package org.janus.modules.identity.service.user;

import org.janus.modules.identity.application.user.dto.request.CreateUserDTO;
import org.janus.modules.identity.application.user.mapper.UserMapper;
import org.janus.modules.identity.application.user.service.CreateUserUseCase;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.ports.out.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateUserUseCase")
class CreateUserUseCaseTest {

    @InjectMocks
    private CreateUserUseCase createUserUseCase;

    @Mock
    private UserRepository repository;

    @Mock
    private UserMapper mapper;

    private CreateUserDTO dto;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        dto = new CreateUserDTO();
        dto.setEmail("test@janus.org");
        dto.setUsername("janus_user");
        dto.setFullName("Janus User");

        user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("test@janus.org")
                .username("janus_user")
                .fullName("Janus User")
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("Success")
    class Success {

        @Test
        @DisplayName("should create user successfully")
        void shouldCreateUserSuccessfully() {
            when(mapper.toEntity(dto)).thenReturn(user);
            when(repository.insert(user)).thenReturn(user);

            Result<UserEntity> result = createUserUseCase.execute(dto);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isSameAs(user);

            verify(mapper).toEntity(dto);
            verify(repository).insert(user);
        }

        @Test
        @DisplayName("should normalize email before inserting user")
        void shouldNormalizeEmailBeforeInsert() {
            dto.setEmail("  TEST@JANUS.ORG  ");
            user.setEmail("  TEST@JANUS.ORG  ");

            when(mapper.toEntity(dto)).thenReturn(user);
            when(repository.insert(user)).thenReturn(user);

            createUserUseCase.execute(dto);

            assertThat(user.getEmail()).isEqualTo("test@janus.org");
            verify(repository).insert(user);
        }

        @Test
        @DisplayName("should handle null email gracefully during normalization")
        void shouldHandleNullEmailDuringNormalization() {
            dto.setEmail(null);
            user.setEmail(null);

            when(mapper.toEntity(dto)).thenReturn(user);
            when(repository.insert(user)).thenReturn(user);

            Result<UserEntity> result = createUserUseCase.execute(dto);

            assertThat(result.isSuccess()).isTrue();
            assertThat(user.getEmail()).isNull();
            verify(repository).insert(user);
        }
    }

    @Nested
    @DisplayName("Invalid input")
    class InvalidInput {

        @Test
        @DisplayName("should return bad request when payload is null")
        void shouldReturnBadRequestWhenPayloadIsNull() {
            Result<UserEntity> result = createUserUseCase.execute(null);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).isEqualTo("User payload cannot be null");

            verifyNoInteractions(mapper, repository);
        }
    }

    @Nested
    @DisplayName("Unique constraints")
    class UniqueConstraints {

        @Test
        @DisplayName("should return conflict when email already exists")
        void shouldReturnConflictWhenEmailAlreadyExists() {
            when(mapper.toEntity(dto)).thenReturn(user);
            when(repository.insert(user)).thenThrow(
                    new DataIntegrityViolationException(
                            "ERROR: duplicate key value violates unique constraint \"uk_email_user\""
                    )
            );

            Result<UserEntity> result = createUserUseCase.execute(dto);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(409);
            assertThat(result.getFirstError()).isEqualTo("Email: 'test@janus.org' already exists");

            verify(repository).insert(user);
        }

        @Test
        @DisplayName("should return conflict when username already exists")
        void shouldReturnConflictWhenUsernameAlreadyExists() {
            when(mapper.toEntity(dto)).thenReturn(user);
            when(repository.insert(user)).thenThrow(
                    new DataIntegrityViolationException(
                            "ERROR: duplicate key value violates unique constraint \"uk_username_user\""
                    )
            );

            Result<UserEntity> result = createUserUseCase.execute(dto);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(409);
            assertThat(result.getFirstError()).isEqualTo("Username: 'janus_user' already exists");

            verify(repository).insert(user);
        }

        @Test
        @DisplayName("should handle active email unique index")
        void shouldHandleActiveEmailUniqueIndex() {
            when(mapper.toEntity(dto)).thenReturn(user);
            when(repository.insert(user)).thenThrow(
                    new DataIntegrityViolationException(
                            "duplicate key value violates unique constraint \"uk_users_active_email\""
                    )
            );

            Result<UserEntity> result = createUserUseCase.execute(dto);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(409);
            assertThat(result.getFirstError()).isEqualTo("Email: 'test@janus.org' already exists");
        }

        @Test
        @DisplayName("should handle active username unique index")
        void shouldHandleActiveUsernameUniqueIndex() {
            when(mapper.toEntity(dto)).thenReturn(user);
            when(repository.insert(user)).thenThrow(
                    new DataIntegrityViolationException(
                            "duplicate key value violates unique constraint \"uk_users_active_username\""
                    )
            );

            Result<UserEntity> result = createUserUseCase.execute(dto);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(409);
            assertThat(result.getFirstError()).isEqualTo("Username: 'janus_user' already exists");
        }
    }

    @Nested
    @DisplayName("Check constraints")
    class CheckConstraints {

        @Test
        @DisplayName("should reject non lowercase email")
        void shouldRejectNonLowercaseEmail() {
            assertConstraintFailure("ck_users_email_lowercase", "Email should be lowercase", 400);
        }

        @Test
        @DisplayName("should reject empty email")
        void shouldRejectEmptyEmail() {
            assertConstraintFailure("ck_users_email_not_empty", "Email should be defined", 400);
        }

        @Test
        @DisplayName("should reject empty username")
        void shouldRejectEmptyUsername() {
            assertConstraintFailure("ck_users_username_not_empty", "Username should be defined", 400);
        }

        @Test
        @DisplayName("should reject username containing spaces")
        void shouldRejectUsernameContainingSpaces() {
            assertConstraintFailure("ck_users_username_no_spaces", "Username cannot contain spaces", 400);
        }

        @Test
        @DisplayName("should reject empty full name")
        void shouldRejectEmptyFullName() {
            assertConstraintFailure("ck_users_full_name_not_empty", "Full name should be defined", 400);
        }

        @Test
        @DisplayName("should reject negative failed login attempts")
        void shouldRejectNegativeFailedLoginAttempts() {
            assertConstraintFailure("ck_users_failed_login_attempts", "Failed login attempts cannot be negative", 400);
        }

        @Test
        @DisplayName("should reject negative version")
        void shouldRejectNegativeVersion() {
            assertConstraintFailure("ck_users_version", "Version cannot be negative", 400);
        }

        private void assertConstraintFailure(String constraint, String expectedMessage, int expectedStatus) {
            when(mapper.toEntity(dto)).thenReturn(user);
            when(repository.insert(user)).thenThrow(
                    new DataIntegrityViolationException("violates check constraint \"" + constraint + "\"")
            );

            Result<UserEntity> result = createUserUseCase.execute(dto);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(expectedStatus);
            assertThat(result.getFirstError()).isEqualTo(expectedMessage);
        }
    }

    @Nested
    @DisplayName("Wrapped exceptions")
    class WrappedExceptions {

        @Test
        @DisplayName("should find integrity exception in cause chain")
        void shouldFindIntegrityExceptionInCauseChain() {
            when(mapper.toEntity(dto)).thenReturn(user);

            DataIntegrityViolationException cause = new DataIntegrityViolationException(
                    "duplicate key value violates unique constraint \"uk_email_user\""
            );
            RuntimeException wrapped = new RuntimeException("Transaction failed", cause);

            when(repository.insert(user)).thenThrow(wrapped);

            Result<UserEntity> result = createUserUseCase.execute(dto);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(409);
            assertThat(result.getFirstError()).isEqualTo("Email: 'test@janus.org' already exists");
        }

        @Test
        @DisplayName("should handle deeply nested integrity exception")
        void shouldHandleDeeplyNestedIntegrityException() {
            when(mapper.toEntity(dto)).thenReturn(user);

            DataIntegrityViolationException integrityException = new DataIntegrityViolationException(
                    "violates check constraint \"ck_users_version\""
            );
            RuntimeException levelTwo = new RuntimeException("Level two", integrityException);
            RuntimeException levelOne = new RuntimeException("Level one", levelTwo);

            when(repository.insert(user)).thenThrow(levelOne);

            Result<UserEntity> result = createUserUseCase.execute(dto);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).isEqualTo("Version cannot be negative");
        }
    }

    @Nested
    @DisplayName("Generic database errors")
    class GenericDatabaseErrors {

        @Test
        @DisplayName("should delegate null message to DatabaseConstraintHandler")
        void shouldDelegateNullMessageToConstraintHandler() {
            when(mapper.toEntity(dto)).thenReturn(user);
            when(repository.insert(user)).thenThrow(new DataIntegrityViolationException(null));

            Result<UserEntity> result = createUserUseCase.execute(dto);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError()).isEqualTo("Database integrity error");
        }

        @Test
        @DisplayName("should delegate unknown integrity violation")
        void shouldDelegateUnknownIntegrityViolation() {
            when(mapper.toEntity(dto)).thenReturn(user);
            when(repository.insert(user)).thenThrow(
                    new DataIntegrityViolationException("foreign key constraint failure")
            );

            Result<UserEntity> result = createUserUseCase.execute(dto);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getFirstError()).contains("Database integrity error");
        }
    }

    @Nested
    @DisplayName("Unexpected errors")
    class UnexpectedErrors {

        @Test
        @DisplayName("should throw InternalServerErrorException")
        void shouldThrowInternalServerErrorException() {
            when(mapper.toEntity(dto)).thenReturn(user);
            when(repository.insert(user)).thenThrow(
                    new RuntimeException("Unexpected database connection failure")
            );

            assertThatThrownBy(() -> createUserUseCase.execute(dto))
                    .isInstanceOf(InternalServerErrorException.class)
                    .hasMessageContaining("Error executing INSERT for table: users");
        }
    }
}