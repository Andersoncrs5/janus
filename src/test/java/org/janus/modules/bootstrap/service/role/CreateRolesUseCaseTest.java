package org.janus.modules.bootstrap.service.role;

import org.janus.modules.authorization.application.dto.role.request.CreateRoleDTO;
import org.janus.modules.authorization.application.service.role.CreateRoleUseCase;
import org.janus.modules.authorization.application.service.role.ExistsRoleByNameUseCase;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.bootstrap.gateway.BootstrapInBoundGateway;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateRolesUseCaseTest {

    @Mock
    private BootstrapInBoundGateway gateway;

    @Mock
    private BootstrapProperties properties;

    @Mock
    private ExistsRoleByNameUseCase existsRoleByNameUseCase;

    @Mock
    private CreateRoleUseCase createRoleUseCase;

    @InjectMocks
    private CreateRolesUseCase useCase;

    private BootstrapProperties.RoleConfig adminRoleConfig;
    private BootstrapProperties.RoleConfig userRoleConfig;

    @BeforeEach
    void setUp() {
        adminRoleConfig = mock(BootstrapProperties.RoleConfig.class);
        lenient().when(adminRoleConfig.name()).thenReturn("Administrator");
        lenient().when(adminRoleConfig.description()).thenReturn("System Administrator Role");
        lenient().when(adminRoleConfig.slug()).thenReturn("admin");

        userRoleConfig = mock(BootstrapProperties.RoleConfig.class);
        lenient().when(userRoleConfig.name()).thenReturn("Regular User");
        lenient().when(userRoleConfig.description()).thenReturn("Standard User Role");
        lenient().when(userRoleConfig.slug()).thenReturn("user");

        lenient().when(gateway.existsRoleByNameUseCase()).thenReturn(existsRoleByNameUseCase);
        lenient().when(gateway.createRoleUseCase()).thenReturn(createRoleUseCase);
    }

    @Test
    @DisplayName("Should return early when roles list is null")
    void shouldReturnEarlyWhenRolesListIsNull() {
        when(properties.roles()).thenReturn(null);

        useCase.execute();

        verifyNoInteractions(gateway);
    }

    @Test
    @DisplayName("Should return early when roles list is empty")
    void shouldReturnEarlyWhenRolesListIsEmpty() {
        when(properties.roles()).thenReturn(Collections.emptyList());

        useCase.execute();

        verifyNoInteractions(gateway);
    }

    @Test
    @DisplayName("Should create roles successfully when none exist")
    void shouldCreateRolesSuccessfully() {
        when(properties.roles()).thenReturn(List.of(adminRoleConfig, userRoleConfig));

        when(existsRoleByNameUseCase.execute("Administrator")).thenReturn(Result.success(false));
        when(existsRoleByNameUseCase.execute("Regular User")).thenReturn(Result.success(false));

        RoleEntity adminEntity = new RoleEntity();
        adminEntity.setId(UUID.randomUUID());
        RoleEntity userEntity = new RoleEntity();
        userEntity.setId(UUID.randomUUID());

        when(createRoleUseCase.execute(any(CreateRoleDTO.class)))
                .thenReturn(Result.success(adminEntity))
                .thenReturn(Result.success(userEntity));

        useCase.execute();

        ArgumentCaptor<CreateRoleDTO> captor = ArgumentCaptor.forClass(CreateRoleDTO.class);
        verify(createRoleUseCase, times(2)).execute(captor.capture());

        List<CreateRoleDTO> capturedDtos = captor.getAllValues();
        assertEquals("Administrator", capturedDtos.get(0).name());
        assertEquals("admin", capturedDtos.get(0).slug());
        assertEquals("Regular User", capturedDtos.get(1).name());
        assertEquals("user", capturedDtos.get(1).slug());
    }

    @Test
    @DisplayName("Should skip role creation when role already exists")
    void shouldSkipRoleCreationWhenRoleExists() {
        when(properties.roles()).thenReturn(List.of(adminRoleConfig));
        when(existsRoleByNameUseCase.execute("Administrator")).thenReturn(Result.success(true));

        useCase.execute();

        verify(existsRoleByNameUseCase, times(1)).execute("Administrator");
        verify(createRoleUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("Should continue loop when checking existence fails for a role")
    void shouldContinueWhenExistenceCheckFails() {
        when(properties.roles()).thenReturn(List.of(adminRoleConfig, userRoleConfig));
        when(existsRoleByNameUseCase.execute("Administrator")).thenReturn(Result.failure("Database error", 500));
        when(existsRoleByNameUseCase.execute("Regular User")).thenReturn(Result.success(false));

        RoleEntity userEntity = new RoleEntity();
        userEntity.setId(UUID.randomUUID());
        when(createRoleUseCase.execute(any(CreateRoleDTO.class))).thenReturn(Result.success(userEntity));

        useCase.execute();

        verify(existsRoleByNameUseCase, times(1)).execute("Administrator");
        verify(existsRoleByNameUseCase, times(1)).execute("Regular User");
        verify(createRoleUseCase, times(1)).execute(any(CreateRoleDTO.class));
    }

    @Test
    @DisplayName("Should continue loop when role creation fails")
    void shouldContinueWhenRoleCreationFails() {
        when(properties.roles()).thenReturn(List.of(adminRoleConfig, userRoleConfig));
        when(existsRoleByNameUseCase.execute(anyString())).thenReturn(Result.success(false));

        RoleEntity userEntity = new RoleEntity();
        userEntity.setId(UUID.randomUUID());

        when(createRoleUseCase.execute(any(CreateRoleDTO.class)))
                .thenReturn(Result.failure("Constraint error", 500))
                .thenReturn(Result.success(userEntity));

        useCase.execute();

        verify(createRoleUseCase, times(2)).execute(any(CreateRoleDTO.class));
    }
}