package org.janus.modules.identity.service.user;

import org.janus.modules.identity.application.user.dto.UpdateUserDTO;
import org.janus.modules.identity.application.user.mapper.UserMapper;
import org.janus.modules.identity.application.user.service.user.UpdateUserUseCase;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateUserUseCaseTest {

    @InjectMocks
    private UpdateUserUseCase updateUserUseCase;

    @Mock
    private UserRepository repository;

    @Mock
    private UserMapper mapper;

    private UUID userId;
    private UserEntity existingEntity;
    private UpdateUserDTO updateDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        existingEntity = UserEntity.builder()
                .id(userId)
                .email("test@janus.org")
                .username("janus_user")
                .fullName("Janus User")
                .isActive(true)
                .build();

        updateDTO = new UpdateUserDTO();
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessCases {

        @Test
        @DisplayName("Should update user successfully when ID exists")
        void shouldUpdateUserSuccessfully() {
            when(repository.findById(userId)).thenReturn(Optional.of(existingEntity));
            doNothing().when(mapper).updateEntityFromDto(updateDTO, existingEntity);
            when(repository.save(existingEntity)).thenReturn(existingEntity);

            Result<UserEntity> result = updateUserUseCase.execute(userId, updateDTO);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData().getId()).isEqualTo(userId);

            verify(repository).findById(userId);
            verify(mapper).updateEntityFromDto(updateDTO, existingEntity);
            verify(repository).save(existingEntity);
            verifyNoMoreInteractions(repository, mapper);
        }
    }

    @Nested
    @DisplayName("Not Found Scenarios")
    class NotFoundCases {

        @Test
        @DisplayName("Should return HTTP 404 failure when user ID does not exist")
        void shouldReturnNotFoundWhenUserDoesNotExist() {
            when(repository.findById(userId)).thenReturn(Optional.empty());

            Result<UserEntity> result = updateUserUseCase.execute(userId, updateDTO);

            assertThat(result).isNotNull();
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(404);
            assertThat(result.getFirstError()).isEqualTo("User not found");

            verify(repository).findById(userId);
            verifyNoInteractions(mapper);
            verifyNoMoreInteractions(repository);
        }
    }

    @Nested
    @DisplayName("Duplicate Key Scenarios")
    class DuplicateKeyCases {

        @Test
        @DisplayName("Should return HTTP 409 conflict when email already exists on update")
        void shouldReturnConflictWhenEmailAlreadyExists() {
            DataIntegrityViolationException exception = new DataIntegrityViolationException(
                    "Key (email)=(test@janus.org) already exists. Constraint: uk_email_user"
            );

            when(repository.findById(userId)).thenReturn(Optional.of(existingEntity));
            doNothing().when(mapper).updateEntityFromDto(updateDTO, existingEntity);
            when(repository.save(existingEntity)).thenThrow(exception);

            Result<UserEntity> result = updateUserUseCase.execute(userId, updateDTO);

            assertThat(result).isNotNull();
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getFirstError()).isEqualTo("Email already exists");

            verify(repository).findById(userId);
            verify(mapper).updateEntityFromDto(updateDTO, existingEntity);
            verify(repository).save(existingEntity);
            verifyNoMoreInteractions(repository, mapper);
        }

        @Test
        @DisplayName("Should return HTTP 409 conflict when username already exists on update")
        void shouldReturnConflictWhenUsernameAlreadyExists() {
            DataIntegrityViolationException exception = new DataIntegrityViolationException(
                    "Key (username)=(janus_user) already exists. Constraint: uk_username_user"
            );

            when(repository.findById(userId)).thenReturn(Optional.of(existingEntity));
            doNothing().when(mapper).updateEntityFromDto(updateDTO, existingEntity);
            when(repository.save(existingEntity)).thenThrow(exception);

            Result<UserEntity> result = updateUserUseCase.execute(userId, updateDTO);

            assertThat(result).isNotNull();
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getFirstError()).isEqualTo("Username already exists");

            verify(repository).findById(userId);
            verify(mapper).updateEntityFromDto(updateDTO, existingEntity);
            verify(repository).save(existingEntity);
            verifyNoMoreInteractions(repository, mapper);
        }
    }

    @Nested
    @DisplayName("Constraint Handler Fallback Scenarios")
    class ConstraintHandlerCases {

        @Test
        @DisplayName("Should fallback to DatabaseConstraintHandler when exception message is null")
        void shouldHandleNullMessageInDataIntegrityException() {
            DataIntegrityViolationException exception = new DataIntegrityViolationException(null);

            when(repository.findById(userId)).thenReturn(Optional.of(existingEntity));
            doNothing().when(mapper).updateEntityFromDto(updateDTO, existingEntity);
            when(repository.save(existingEntity)).thenThrow(exception);

            Result<UserEntity> result = updateUserUseCase.execute(userId, updateDTO);

            assertThat(result).isNotNull();
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(400);

            verify(repository).findById(userId);
            verify(mapper).updateEntityFromDto(updateDTO, existingEntity);
            verify(repository).save(existingEntity);
            verifyNoMoreInteractions(repository, mapper);
        }

        @Test
        @DisplayName("Should fallback to DatabaseConstraintHandler on generic integrity exception")
        void shouldHandleGenericDataIntegrityViolation() {
            DataIntegrityViolationException exception = new DataIntegrityViolationException(
                    "foreign key constraint failed"
            );

            when(repository.findById(userId)).thenReturn(Optional.of(existingEntity));
            doNothing().when(mapper).updateEntityFromDto(updateDTO, existingEntity);
            when(repository.save(existingEntity)).thenThrow(exception);

            Result<UserEntity> result = updateUserUseCase.execute(userId, updateDTO);

            assertThat(result).isNotNull();
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(400);

            verify(repository).findById(userId);
            verify(mapper).updateEntityFromDto(updateDTO, existingEntity);
            verify(repository).save(existingEntity);
            verifyNoMoreInteractions(repository, mapper);
        }
    }

    @Nested
    @DisplayName("Unexpected Error Scenarios")
    class UnexpectedErrorCases {

        @Test
        @DisplayName("Should throw InternalServerErrorException on unexpected error")
        void shouldThrowInternalServerErrorExceptionOnUnexpectedError() {
            when(repository.findById(userId)).thenReturn(Optional.of(existingEntity));
            doNothing().when(mapper).updateEntityFromDto(updateDTO, existingEntity);
            when(repository.save(existingEntity)).thenThrow(new RuntimeException("Unexpected DB Connection Failure"));

            assertThatThrownBy(() -> updateUserUseCase.execute(userId, updateDTO))
                    .isInstanceOf(InternalServerErrorException.class)
                    .hasMessageContaining("Unexpected DB Connection Failure");

            verify(repository).findById(userId);
            verify(mapper).updateEntityFromDto(updateDTO, existingEntity);
            verify(repository).save(existingEntity);
            verifyNoMoreInteractions(repository, mapper);
        }
    }
}