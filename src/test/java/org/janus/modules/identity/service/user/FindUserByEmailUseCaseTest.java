package org.janus.modules.identity.service.user;

import org.janus.modules.identity.application.user.service.user.FindUserByEmailUseCase;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.ports.out.UserRepository;
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
class FindUserByEmailUseCaseTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private FindUserByEmailUseCase useCase;

    private String email;
    private UserEntity userEntity;

    @BeforeEach
    void setUp() {
        email = "user@janus.com";
        userEntity = new UserEntity();
        userEntity.setId(UUID.randomUUID());
        userEntity.setEmail(email);
        userEntity.setUsername("janususer");
        userEntity.setFullName("Janus User");
    }

    @Test
    @DisplayName("Should return user successfully when email exists")
    void shouldReturnUserSuccessfullyWhenEmailExists() {
        when(repository.findByEmail(email)).thenReturn(Optional.of(userEntity));

        Result<UserEntity> result = useCase.execute(email);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(email, result.getData().getEmail());
        assertEquals(userEntity.getId(), result.getData().getId());

        verify(repository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("Should return not found when email does not exist")
    void shouldReturnNotFoundWhenEmailDoesNotExist() {
        when(repository.findByEmail(email)).thenReturn(Optional.empty());

        Result<UserEntity> result = useCase.execute(email);

        assertFalse(result.isSuccess());
        assertEquals(404, result.getStatusCode());
        assertTrue(result.getMessage().isPresent());
        assertEquals("User not found", result.getMessage().get());

        verify(repository, times(1)).findByEmail(email);
    }
}