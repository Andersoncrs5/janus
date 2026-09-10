package org.janus.modules.bootstrap.service.user;

import org.janus.modules.bootstrap.gateway.BootstrapInBoundGateway;
import org.janus.modules.identity.application.user.dto.CreateUserDTO;
import org.janus.modules.identity.application.user.service.user.CreateUserUseCase;
import org.janus.modules.identity.application.user.service.user.ExistsUserByEmailUseCase;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.gateway.user.UserOutboundGateway;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.properties.BootstrapProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateUserMasterUseCaseTest {

    @Mock
    private BootstrapInBoundGateway gateway;

    @Mock
    private UserOutboundGateway userOutboundGateway;

    @Mock
    private BootstrapProperties properties;

    @Mock
    private BootstrapProperties.MasterConfig masterConfig;

    @Mock
    private ExistsUserByEmailUseCase existsUserByEmailUseCase;

    @Mock
    private CreateUserUseCase createUserUseCase;

    @InjectMocks
    private CreateUserMasterUseCase useCase;

    private final String email = "admin@gmail.com";
    private final String username = "admin";
    private final String fullName = "System Administrator";
    private final String password = "MasterPassword123!";

    @BeforeEach
    void setUp() {
        lenient().when(properties.master()).thenReturn(masterConfig);
        lenient().when(masterConfig.email()).thenReturn(email);
        lenient().when(masterConfig.username()).thenReturn(username);
        lenient().when(masterConfig.fullName()).thenReturn(fullName);
        lenient().when(masterConfig.password()).thenReturn(password);

        lenient().when(gateway.userOutboundGateway()).thenReturn(userOutboundGateway);

        // 3. Agora você pode encadear com segurança
        lenient().when(userOutboundGateway.existsUserByEmailUseCase()).thenReturn(existsUserByEmailUseCase);
        lenient().when(userOutboundGateway.createUserUseCase()).thenReturn(createUserUseCase);
    }



    @Test
    @DisplayName("Should create master user successfully when user does not exist")
    void shouldCreateMasterUserSuccessfully() {
        UserEntity createdUser = new UserEntity();
        createdUser.setId(UUID.randomUUID());
        createdUser.setEmail(email);

        when(existsUserByEmailUseCase.execute(email)).thenReturn(Result.success(false));
        when(createUserUseCase.execute(any(CreateUserDTO.class))).thenReturn(Result.success(createdUser));

        useCase.execute();

        ArgumentCaptor<CreateUserDTO> dtoCaptor = ArgumentCaptor.forClass(CreateUserDTO.class);
        verify(createUserUseCase, times(1)).execute(dtoCaptor.capture());

        CreateUserDTO capturedDto = dtoCaptor.getValue();
        assertEquals(email, capturedDto.getEmail());
        assertEquals(username, capturedDto.getUsername());
        assertEquals(fullName, capturedDto.getFullName());
        assertEquals(password, capturedDto.getPassword());

        verify(existsUserByEmailUseCase, times(1)).execute(email);
    }

    @Test
    @DisplayName("Should skip creation when master user already exists")
    void shouldSkipCreationWhenUserAlreadyExists() {
        when(existsUserByEmailUseCase.execute(email)).thenReturn(Result.success(true));

        useCase.execute();

        verify(existsUserByEmailUseCase, times(1)).execute(email);
        verify(createUserUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("Should abort execution when check user existence fails")
    void shouldAbortWhenExistsCheckFails() {
        when(existsUserByEmailUseCase.execute(email)).thenReturn(
                Result.failure("Database connection error", 500)
        );

        useCase.execute();

        verify(existsUserByEmailUseCase, times(1)).execute(email);
        verify(createUserUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("Should handle failure when creation process fails")
    void shouldHandleFailureWhenCreationFails() {
        when(existsUserByEmailUseCase.execute(email)).thenReturn(Result.success(false));
        when(createUserUseCase.execute(any(CreateUserDTO.class))).thenReturn(Result.failure("Error creating user", 500));

        useCase.execute();

        verify(existsUserByEmailUseCase, times(1)).execute(email);
        verify(createUserUseCase, times(1)).execute(any(CreateUserDTO.class));
    }
}