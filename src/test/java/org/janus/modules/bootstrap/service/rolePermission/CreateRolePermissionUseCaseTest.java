package org.janus.modules.bootstrap.service.rolePermission;

import org.janus.modules.authorization.application.dto.rolePermission.request.CreateRolePermissionDTO;
import org.janus.modules.authorization.application.service.permission.FindPermissionByNameUseCase;
import org.janus.modules.authorization.application.service.role.FindRoleByNameUseCase;
import org.janus.modules.authorization.application.service.rolePermission.CreateRolePermissionUseCase;
import org.janus.modules.authorization.application.service.rolePermission.ExistsRolePermissionByRoleIdAndPermissionIdUseCase;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.modules.authorization.gateway.permission.PermissionOutboundGateway;
import org.janus.modules.authorization.gateway.role.RoleOutboundGateway;
import org.janus.modules.authorization.gateway.rolePermission.RolePermissionOutboundGateway;
import org.janus.modules.bootstrap.gateway.BootstrapInBoundGateway;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.properties.BootstrapProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateRolePermissionUseCaseTest {

    @Mock
    private BootstrapInBoundGateway gateway;

    @Mock
    private BootstrapProperties properties;

    @Mock
    private RoleOutboundGateway roleOutboundGateway;

    @Mock
    private PermissionOutboundGateway permissionOutboundGateway;

    @Mock
    private RolePermissionOutboundGateway rolePermissionOutboundGateway;

    @Mock
    private FindRoleByNameUseCase findRoleByNameUseCase;

    @Mock
    private FindPermissionByNameUseCase findPermissionByNameUseCase;

    @Mock
    private ExistsRolePermissionByRoleIdAndPermissionIdUseCase existsRolePermissionUseCase;

    @Mock
    private CreateRolePermissionUseCase createRolePermissionUseCase;

    @InjectMocks
    private CreateRolePermissionUseCaseBootstrap useCase;

    private BootstrapProperties.PermissionConfig permissionConfig;
    private RoleEntity masterRole;
    private PermissionEntity permission;

    @BeforeEach
    void setUp() {
        permissionConfig = mock(BootstrapProperties.PermissionConfig.class);
        lenient().when(permissionConfig.name()).thenReturn("Create User");

        masterRole = new RoleEntity();
        masterRole.setId(UUID.randomUUID());

        permission = new PermissionEntity();
        permission.setId(UUID.randomUUID());
        permission.setName("Create User");

        lenient().when(gateway.roleOutboundGateway()).thenReturn(roleOutboundGateway);
        lenient().when(gateway.permissionOutboundGateway()).thenReturn(permissionOutboundGateway);
        lenient().when(gateway.rolePermissionOutboundGateway()).thenReturn(rolePermissionOutboundGateway);

        lenient().when(roleOutboundGateway.findRoleByNameUseCase()).thenReturn(findRoleByNameUseCase);
        lenient().when(permissionOutboundGateway.findPermissionByNameUseCase()).thenReturn(findPermissionByNameUseCase);
        lenient().when(rolePermissionOutboundGateway.existsRolePermissionByRoleIdAndPermissionIdUseCase()).thenReturn(existsRolePermissionUseCase);
        lenient().when(rolePermissionOutboundGateway.createRolePermissionUseCase()).thenReturn(createRolePermissionUseCase);
    }

    @Nested
    @DisplayName("Validation & Early Return Scenarios")
    class EarlyReturnScenarios {

        @Test
        @DisplayName("Should return early when permissions list is null")
        void shouldReturnEarlyWhenPermissionsListIsNull() {
            when(properties.permissions()).thenReturn(null);

            useCase.execute();

            verifyNoInteractions(gateway);
        }

        @Test
        @DisplayName("Should return early when permissions list is empty")
        void shouldReturnEarlyWhenPermissionsListIsEmpty() {
            when(properties.permissions()).thenReturn(Collections.emptyList());

            useCase.execute();

            verifyNoInteractions(gateway);
        }

        @Test
        @DisplayName("Should return early when 'MASTER' role is not found")
        void shouldReturnEarlyWhenMasterRoleIsNotFound() {
            when(properties.permissions()).thenReturn(List.of(permissionConfig));
            when(findRoleByNameUseCase.execute("MASTER")).thenReturn(Result.badRequest("Role not found"));

            useCase.execute();

            verify(gateway, never()).permissionOutboundGateway();
            verify(gateway, never()).rolePermissionOutboundGateway();
        }
    }

    @Nested
    @DisplayName("Execution Scenarios")
    class ExecutionScenarios {

        @BeforeEach
        void setupSuccessRole() {
            when(properties.permissions()).thenReturn(List.of(permissionConfig));
            when(findRoleByNameUseCase.execute("MASTER")).thenReturn(Result.ok(masterRole));
        }

        @Test
        @DisplayName("Should successfully link permission to 'MASTER' role")
        void shouldLinkPermissionSuccessfully() {
            when(findPermissionByNameUseCase.execute("Create User")).thenReturn(Result.ok(permission));
            when(existsRolePermissionUseCase.execute(masterRole.getId(), permission.getId())).thenReturn(Result.ok(false));

            RolePermissionEntity rolePermissionEntity = new RolePermissionEntity();
            when(createRolePermissionUseCase.execute(any(CreateRolePermissionDTO.class), isNull()))
                    .thenReturn(Result.ok(rolePermissionEntity));

            useCase.execute();

            ArgumentCaptor<CreateRolePermissionDTO> captor = ArgumentCaptor.forClass(CreateRolePermissionDTO.class);
            verify(createRolePermissionUseCase, times(1)).execute(captor.capture(), isNull());

            CreateRolePermissionDTO capturedDto = captor.getValue();
            assertThat(capturedDto.roleId()).isEqualTo(masterRole.getId());
            assertThat(capturedDto.permissionId()).isEqualTo(permission.getId());
        }

        @Test
        @DisplayName("Should skip assignment when permission is not found in database")
        void shouldSkipWhenPermissionIsNotFound() {
            when(findPermissionByNameUseCase.execute("Create User")).thenReturn(Result.badRequest("Permission missing"));

            useCase.execute();

            verify(gateway, never()).rolePermissionOutboundGateway();
        }

        @Test
        @DisplayName("Should skip assignment when checking for existing relation fails")
        void shouldSkipWhenRelationCheckFails() {
            when(findPermissionByNameUseCase.execute("Create User")).thenReturn(Result.ok(permission));
            when(existsRolePermissionUseCase.execute(masterRole.getId(), permission.getId()))
                    .thenReturn(Result.badRequest("Database Error"));

            useCase.execute();

            verify(createRolePermissionUseCase, never()).execute(any(), any());
        }

        @Test
        @DisplayName("Should skip assignment when permission is already linked to the role")
        void shouldSkipWhenPermissionIsAlreadyAssigned() {
            when(findPermissionByNameUseCase.execute("Create User")).thenReturn(Result.ok(permission));
            when(existsRolePermissionUseCase.execute(masterRole.getId(), permission.getId())).thenReturn(Result.ok(true));

            useCase.execute();

            verify(createRolePermissionUseCase, never()).execute(any(), any());
        }

        @Test
        @DisplayName("Should log error and continue when creation of role-permission link fails")
        void shouldLogAndContinueWhenRolePermissionCreationFailed() {
            when(findPermissionByNameUseCase.execute("Create User")).thenReturn(Result.ok(permission));
            when(existsRolePermissionUseCase.execute(masterRole.getId(), permission.getId())).thenReturn(Result.ok(false));

            when(createRolePermissionUseCase.execute(any(CreateRolePermissionDTO.class), isNull()))
                    .thenReturn(Result.badRequest("Constraint violation"));

            useCase.execute();

            verify(createRolePermissionUseCase, times(1)).execute(any(CreateRolePermissionDTO.class), isNull());
        }
    }
}