package org.janus.modules.identity.service.userCredentials;

import org.janus.modules.identity.application.userCredentials.service.GetUserCredentialsByUserIdService;
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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetUserCredentialsByUserIdServiceTest {

    @Mock
    private UserCredentialRepository repository;

    @InjectMocks
    private GetUserCredentialsByUserIdService service;

    private UUID userId;
    private UserCredentialsEntity entity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        entity = new UserCredentialsEntity();
        entity.setUserId(userId);
    }

    @Test
    @DisplayName("Should return user credentials successfully when found")
    void shouldReturnUserCredentialsSuccessfullyWhenFound() {
        when(repository.findByUserId(userId)).thenReturn(Optional.of(entity));

        Result<UserCredentialsEntity> result = service.execute(userId);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(userId, result.getData().getUserId());
        verify(repository, times(1)).findByUserId(userId);
    }

    @Test
    @DisplayName("Should return 404 not found when user credentials do not exist")
    void shouldReturnNotFoundWhenUserCredentialsDoNotExist() {
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        Result<UserCredentialsEntity> result = service.execute(userId);

        assertFalse(result.isSuccess());
        assertEquals(404, result.getStatusCode());
        assertTrue(result.getMessage().isPresent());
        assertEquals("User credentials not found", result.getMessage().get());
        verify(repository, times(1)).findByUserId(userId);
    }
}