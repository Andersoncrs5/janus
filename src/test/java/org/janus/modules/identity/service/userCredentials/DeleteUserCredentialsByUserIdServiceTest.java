package org.janus.modules.identity.service.userCredentials;

import org.janus.modules.identity.application.userCredentials.service.DeleteUserCredentialsByUserIdService;
import org.janus.modules.identity.domain.entity.UserCredentialsEntity;
import org.janus.modules.identity.ports.out.UserCredentialRepository;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteUserCredentialsByUserIdServiceTest {

    @Mock
    private UserCredentialRepository repository;

    @InjectMocks
    private DeleteUserCredentialsByUserIdService service;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should delete user credentials successfully when user exists")
    void shouldDeleteUserCredentialsSuccessfully() {
        when(repository.deleteByUserId(userId)).thenReturn(true);

        Result<UserCredentialsEntity> result = service.execute(userId);

        assertTrue(result.isSuccess());
        verify(repository, times(1)).deleteByUserId(userId);
    }

    @Test
    @DisplayName("Should return 404 not found when user credentials do not exist")
    void shouldReturnNotFoundWhenUserCredentialsDoNotExist() {
        when(repository.deleteByUserId(userId)).thenReturn(false);

        Result<UserCredentialsEntity> result = service.execute(userId);

        assertFalse(result.isSuccess());
        assertEquals(404, result.getStatusCode());
        assertThat(result.getMessage().isPresent()).isTrue();
        assertEquals("User not found", result.getMessage().get());
        verify(repository, times(1)).deleteByUserId(userId);
    }
}