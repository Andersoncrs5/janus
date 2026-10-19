package org.janus.modules.identity.service.userCredentials;

import org.janus.modules.identity.application.userCredentials.dto.UpdateUserCredentialsDTO;
import org.janus.modules.identity.application.userCredentials.mapper.UserCredentialsMapper;
import org.janus.modules.identity.application.userCredentials.service.UpdateUserCredentialsUseCase;
import org.janus.modules.identity.domain.entity.UserCredentialsEntity;
import org.janus.modules.identity.ports.out.UserCredentialRepository;
import org.janus.shared.domain.enums.PasswordAlgorithmEnum;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.domain.security.PasswordEncoderPort;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateUserCredentialsUseCase")
class UpdateUserCredentialsUseCaseTest {

    @Mock
    private UserCredentialRepository repository;

    @Mock
    private UserCredentialsMapper mapper;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @InjectMocks
    private UpdateUserCredentialsUseCase useCase;

    private UUID userId;
    private UpdateUserCredentialsDTO dto;
    private UserCredentialsEntity existingEntity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        dto = new UpdateUserCredentialsDTO(
                "newRawPassword123",
                PasswordAlgorithmEnum.ARGON2ID
        );

        existingEntity = new UserCredentialsEntity();
        existingEntity.setUserId(userId);
        existingEntity.setPasswordHash("oldHashedPassword");
        existingEntity.setAlgorithm(
                PasswordAlgorithmEnum.ARGON2ID.getValue()
        );
    }

    @Nested
    @DisplayName("Validation Scenarios")
    class ValidationScenarios {

        @Test
        @DisplayName("Should return 400 when user ID is null")
        void shouldReturnBadRequestWhenUserIdIsNull() {
            Result<UserCredentialsEntity> result =
                    useCase.execute(null, dto);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError())
                    .isEqualTo("User ID is required");

            verifyNoInteractions(repository, mapper, passwordEncoder);
        }

        @Test
        @DisplayName("Should return 400 when DTO is null")
        void shouldReturnBadRequestWhenDtoIsNull() {
            Result<UserCredentialsEntity> result =
                    useCase.execute(userId, null);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError())
                    .isEqualTo("User credentials payload is required");

            verifyNoInteractions(repository, mapper, passwordEncoder);
        }

        @Test
        @DisplayName("Should return 400 when password is null")
        void shouldReturnBadRequestWhenPasswordIsNull() {
            UpdateUserCredentialsDTO invalidDto =
                    new UpdateUserCredentialsDTO(
                            null,
                            PasswordAlgorithmEnum.ARGON2ID
                    );

            Result<UserCredentialsEntity> result =
                    useCase.execute(userId, invalidDto);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError())
                    .isEqualTo("The password/hash is required");

            verifyNoInteractions(repository, mapper, passwordEncoder);
        }

        @Test
        @DisplayName("Should return 400 when password is blank")
        void shouldReturnBadRequestWhenPasswordIsBlank() {
            UpdateUserCredentialsDTO invalidDto =
                    new UpdateUserCredentialsDTO(
                            "   ",
                            PasswordAlgorithmEnum.ARGON2ID
                    );

            Result<UserCredentialsEntity> result =
                    useCase.execute(userId, invalidDto);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getFirstError())
                    .isEqualTo("The password/hash is required");

            verifyNoInteractions(repository, mapper, passwordEncoder);
        }
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should update credentials successfully with valid data")
        void shouldUpdateUserCredentialsSuccessfully() {
            String rawPassword = "newRawPassword123";
            String hashedPassword =
                    "$argon2id$v=19$m=65536,t=2,p=1$newHashedPassword";

            when(repository.findByUserId(userId))
                    .thenReturn(Optional.of(existingEntity));

            when(passwordEncoder.encode(rawPassword))
                    .thenReturn(hashedPassword);

            when(repository.save(any(UserCredentialsEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Result<UserCredentialsEntity> result =
                    useCase.execute(userId, dto);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData().getUserId()).isEqualTo(userId);
            assertThat(result.getData().getPasswordHash())
                    .isEqualTo(hashedPassword);
            assertThat(result.getData().getAlgorithm())
                    .isEqualTo(PasswordAlgorithmEnum.ARGON2ID.getValue());

            verify(repository).findByUserId(userId);
            verify(passwordEncoder).encode(rawPassword);
            verify(mapper).updateEntityFromDto(dto, existingEntity);
            verify(repository).save(existingEntity);
        }

        @Test
        @DisplayName("Should preserve existing algorithm when DTO algorithm is null")
        void shouldPreserveExistingAlgorithmWhenDtoAlgorithmIsNull() {
            UpdateUserCredentialsDTO updateDto =
                    new UpdateUserCredentialsDTO(
                            "newRawPassword123",
                            null
                    );

            when(repository.findByUserId(userId))
                    .thenReturn(Optional.of(existingEntity));

            when(passwordEncoder.encode(updateDto.passwordHash()))
                    .thenReturn("newHashedPassword");

            when(repository.save(existingEntity))
                    .thenReturn(existingEntity);

            Result<UserCredentialsEntity> result =
                    useCase.execute(userId, updateDto);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getAlgorithm())
                    .isEqualTo(PasswordAlgorithmEnum.ARGON2ID.getValue());

            verify(repository).save(existingEntity);
        }

        @Test
        @DisplayName("Should use ARGON2ID when existing algorithm is null")
        void shouldUseDefaultAlgorithmWhenExistingAlgorithmIsNull() {
            existingEntity.setAlgorithm(null);

            UpdateUserCredentialsDTO updateDto =
                    new UpdateUserCredentialsDTO(
                            "newRawPassword123",
                            null
                    );

            when(repository.findByUserId(userId))
                    .thenReturn(Optional.of(existingEntity));

            when(passwordEncoder.encode(updateDto.passwordHash()))
                    .thenReturn("newHashedPassword");

            when(repository.save(existingEntity))
                    .thenReturn(existingEntity);

            Result<UserCredentialsEntity> result =
                    useCase.execute(userId, updateDto);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getAlgorithm())
                    .isEqualTo(PasswordAlgorithmEnum.ARGON2ID.getValue());
        }
    }

    @Nested
    @DisplayName("Not Found Scenarios")
    class NotFoundScenarios {

        @Test
        @DisplayName("Should return 404 when user credentials do not exist")
        void shouldReturnNotFoundWhenUserCredentialsDoNotExist() {
            when(repository.findByUserId(userId))
                    .thenReturn(Optional.empty());

            Result<UserCredentialsEntity> result =
                    useCase.execute(userId, dto);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(404);
            assertThat(result.getFirstError())
                    .isEqualTo("User credentials not found");

            verify(repository).findByUserId(userId);
            verifyNoInteractions(mapper, passwordEncoder);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Unexpected Error Scenarios")
    class UnexpectedErrorScenarios {

        @Test
        @DisplayName("Should throw InternalServerErrorException when repository save fails")
        void shouldThrowInternalServerErrorExceptionOnGenericError() {
            when(repository.findByUserId(userId))
                    .thenReturn(Optional.of(existingEntity));

            when(passwordEncoder.encode(dto.passwordHash()))
                    .thenReturn("newHashedPassword");

            when(repository.save(existingEntity))
                    .thenThrow(new RuntimeException("Database error"));

            assertThatThrownBy(() -> useCase.execute(userId, dto))
                    .isInstanceOf(InternalServerErrorException.class)
                    .hasMessageContaining("Database error");

            verify(repository).findByUserId(userId);
            verify(passwordEncoder).encode(dto.passwordHash());
            verify(mapper).updateEntityFromDto(dto, existingEntity);
            verify(repository).save(existingEntity);
        }
    }
}