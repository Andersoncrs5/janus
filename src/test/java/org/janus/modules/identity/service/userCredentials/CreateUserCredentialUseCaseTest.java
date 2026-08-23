package org.janus.modules.identity.service.userCredentials;

import org.janus.modules.identity.application.userCredentials.dto.CreateUserCredentialsDTO;
import org.janus.modules.identity.application.userCredentials.mapper.UserCredentialsMapper;
import org.janus.modules.identity.application.userCredentials.service.CreateUserCredentialUseCase;
import org.janus.modules.identity.domain.entity.UserCredentialsEntity;
import org.janus.modules.identity.ports.out.UserCredentialRepository;
import org.janus.shared.domain.enums.PasswordAlgorithmEnum;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.domain.security.PasswordEncoderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateUserCredentialUseCaseTest {

    @Mock
    private UserCredentialRepository repository;

    @Mock
    private UserCredentialsMapper mapper;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @InjectMocks
    private CreateUserCredentialUseCase useCase;

    private UUID userId;
    private CreateUserCredentialsDTO dto;
    private UserCredentialsEntity entity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        dto = new CreateUserCredentialsDTO("rawPassword123", PasswordAlgorithmEnum.ARGON2ID);
        entity = new UserCredentialsEntity();
    }

    @Test
    @DisplayName("Should create user credential successfully when valid data is provided")
    void shouldCreateUserCredentialSuccessfully() {
        String rawPassword = "rawPassword123";
        String hashedPassword = "$argon2id$v=19$m=65536,t=2,p=1$hashedPassword";

        when(mapper.toEntity(dto)).thenReturn(entity);
        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);
        when(repository.insert(any(UserCredentialsEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Result<UserCredentialsEntity> result = useCase.execute(dto, userId);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(userId, result.getData().getUserId());
        assertEquals(hashedPassword, result.getData().getPasswordHash());
        assertEquals(PasswordAlgorithmEnum.ARGON2ID.getValue(), result.getData().getAlgorithm());

        verify(mapper, times(1)).toEntity(dto);
        verify(passwordEncoder, times(1)).encode(rawPassword);
        verify(repository, times(1)).insert(entity);
    }

    @Test
    @DisplayName("Should return 409 conflict when user already has a credential")
    void shouldReturnConflictWhenUserAlreadyHasCredential() {
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(passwordEncoder.encode(any())).thenReturn("hashedPassword");

        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Key (user_id)=(...) already exists. Constraint: uk_user_id_credentials_user"
        );
        when(repository.insert(any())).thenThrow(exception);

        Result<UserCredentialsEntity> result = useCase.execute(dto, userId);

        assertFalse(result.isSuccess());
        assertEquals(409, result.getStatusCode());
        assertEquals("User already have one credential", result.getMessage().get());
        verify(repository, times(1)).insert(any());
    }

    @Test
    @DisplayName("Should return 404 not found when user does not exist")
    void shouldReturnNotFoundWhenUserDoesNotExist() {
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(passwordEncoder.encode(any())).thenReturn("hashedPassword");

        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Key (user_id)=(...) is not present in table user. Constraint: fk_user_credentials_user"
        );
        when(repository.insert(any())).thenThrow(exception);

        Result<UserCredentialsEntity> result = useCase.execute(dto, userId);

        assertFalse(result.isSuccess());
        assertEquals(404, result.getStatusCode());
        assertEquals("User not found", result.getMessage().get());
        verify(repository, times(1)).insert(any());
    }

    @Test
    @DisplayName("Should throw InternalServerErrorException on unexpected generic exception")
    void shouldThrowInternalServerErrorExceptionOnGenericError() {
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(passwordEncoder.encode(any())).thenReturn("hashedPassword");
        when(repository.insert(any())).thenThrow(new RuntimeException("Database connection timeout"));

        assertThrows(InternalServerErrorException.class, () -> useCase.execute(dto, userId));
        verify(repository, times(1)).insert(any());
    }
}