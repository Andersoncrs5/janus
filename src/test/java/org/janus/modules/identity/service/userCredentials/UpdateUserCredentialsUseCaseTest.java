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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
        dto = new UpdateUserCredentialsDTO("newRawPassword123", PasswordAlgorithmEnum.ARGON2ID);

        existingEntity = new UserCredentialsEntity();
        existingEntity.setUserId(userId);
        existingEntity.setPasswordHash("oldHashedPassword");
        existingEntity.setAlgorithm(PasswordAlgorithmEnum.ARGON2ID.getValue());
    }

    @Test
    @DisplayName("Should update user credentials successfully when valid data is provided")
    void shouldUpdateUserCredentialsSuccessfully() {
        String newRawPassword = "newRawPassword123";
        String newHashedPassword = "$argon2id$v=19$m=65536,t=2,p=1$newHashedPassword";

        when(repository.findByUserId(userId)).thenReturn(Optional.of(existingEntity));
        when(passwordEncoder.encode(newRawPassword)).thenReturn(newHashedPassword);
        when(repository.save(any(UserCredentialsEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Result<UserCredentialsEntity> result = useCase.execute(userId, dto);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(userId, result.getData().getUserId());
        assertEquals(newHashedPassword, result.getData().getPasswordHash());

        verify(repository, times(1)).findByUserId(userId);
        verify(passwordEncoder, times(1)).encode(newRawPassword);
        verify(mapper, times(1)).updateEntityFromDto(dto, existingEntity);
        verify(repository, times(1)).save(existingEntity);
    }

    @Test
    @DisplayName("Should return 404 not found when user credentials do not exist")
    void shouldReturnNotFoundWhenUserCredentialsDoNotExist() {
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        Result<UserCredentialsEntity> result = useCase.execute(userId, dto);

        assertFalse(result.isSuccess());
        assertEquals(404, result.getStatusCode());
        assertEquals("User credentials not found", result.getFirstError());

        verify(repository, times(1)).findByUserId(userId);
        verify(passwordEncoder, never()).encode(any());
        verify(mapper, never()).updateEntityFromDto(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InternalServerErrorException on unexpected generic exception")
    void shouldThrowInternalServerErrorExceptionOnGenericError() {
        when(repository.findByUserId(userId)).thenReturn(Optional.of(existingEntity));
        when(passwordEncoder.encode(any())).thenReturn("newHashedPassword");
        when(repository.save(any())).thenThrow(new RuntimeException("Database error"));

        assertThrows(InternalServerErrorException.class, () -> useCase.execute(userId, dto));

        verify(repository, times(1)).findByUserId(userId);
        verify(mapper, times(1)).updateEntityFromDto(dto, existingEntity);
        verify(repository, times(1)).save(any());
    }
}
