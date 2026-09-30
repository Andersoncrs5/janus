package org.janus.modules.bootstrap.service.rolePermission;


import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.modules.authorization.infrastructure.out.PermissionRepository;
import org.janus.modules.authorization.infrastructure.out.RolePermissionRepository;
import org.janus.modules.authorization.infrastructure.out.RoleRepository;
import org.janus.shared.domain.enums.rolePermission.PermissionEffectEnum;
import org.janus.shared.domain.exception.InternalServerErrorException;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateRolePermissionUseCaseTest {

    @Mock
    private BootstrapProperties properties;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @InjectMocks
    private CreateRolePermissionUseCaseBootstrap useCase;

    private BootstrapProperties.PermissionConfig permissionConfig;
    private RoleEntity masterRole;
    private PermissionEntity permission;

    @BeforeEach
    void setUp() {
        permissionConfig =
                mock(BootstrapProperties.PermissionConfig.class);

        lenient()
                .when(permissionConfig.name())
                .thenReturn("Create User");

        masterRole = new RoleEntity();
        masterRole.setId(UUID.randomUUID());
        masterRole.setName("MASTER");

        permission = new PermissionEntity();
        permission.setId(UUID.randomUUID());
        permission.setName("Create User");
    }

    @Nested
    @DisplayName("Validation & Early Return Scenarios")
    class EarlyReturnScenarios {

        @Test
        @DisplayName("Should return early when permissions list is null")
        void shouldReturnEarlyWhenPermissionsListIsNull() {

            when(properties.permissions())
                    .thenReturn(null);

            useCase.execute();

            verify(properties)
                    .permissions();

            verifyNoInteractions(
                    roleRepository,
                    permissionRepository,
                    rolePermissionRepository
            );
        }

    }

    @Nested
    @DisplayName("Master Role Scenarios")
    class MasterRoleScenarios {

        @Test
        @DisplayName("Should throw InternalServerErrorException when MASTER role does not exist")
        void shouldThrowWhenMasterRoleDoesNotExist() {

            when(properties.permissions())
                    .thenReturn(List.of(permissionConfig));

            when(roleRepository.findByName("MASTER"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> useCase.execute()
            )
                    .isInstanceOf(InternalServerErrorException.class)
                    .hasMessage(
                            "MASTER role not found during bootstrap"
                    );

            verify(roleRepository)
                    .findByName("MASTER");

            verifyNoInteractions(
                    permissionRepository,
                    rolePermissionRepository
            );
        }

        @Test
        @DisplayName("Should find MASTER role before processing permissions")
        void shouldFindMasterRoleBeforeProcessingPermissions() {

            when(properties.permissions())
                    .thenReturn(List.of(permissionConfig));

            when(roleRepository.findByName("MASTER"))
                    .thenReturn(Optional.of(masterRole));

            when(permissionRepository.findByName("Create User"))
                    .thenReturn(Optional.of(permission));

            when(rolePermissionRepository
                    .existsByRoleIdAndPermissionId(
                            masterRole.getId(),
                            permission.getId()
                    ))
                    .thenReturn(true);

            useCase.execute();

            var order = inOrder(
                    roleRepository,
                    permissionRepository,
                    rolePermissionRepository
            );

            order.verify(roleRepository)
                    .findByName("MASTER");

            order.verify(permissionRepository)
                    .findByName("Create User");

            order.verify(rolePermissionRepository)
                    .existsByRoleIdAndPermissionId(
                            masterRole.getId(),
                            permission.getId()
                    );
        }
    }

    @Nested
    @DisplayName("Permission Scenarios")
    class PermissionScenarios {

        @BeforeEach
        void setupMasterRole() {

            when(properties.permissions())
                    .thenReturn(List.of(permissionConfig));

            when(roleRepository.findByName("MASTER"))
                    .thenReturn(Optional.of(masterRole));
        }

        @Test
        @DisplayName("Should throw InternalServerErrorException when permission does not exist")
        void shouldThrowWhenPermissionDoesNotExist() {

            when(permissionRepository.findByName("Create User"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> useCase.execute()
            )
                    .isInstanceOf(InternalServerErrorException.class)
                    .hasMessage(
                            "Permission 'Create User' not found during bootstrap"
                    );

            verify(permissionRepository)
                    .findByName("Create User");

            verify(rolePermissionRepository, never())
                    .existsByRoleIdAndPermissionId(any(), any());

            verify(rolePermissionRepository, never())
                    .insert(any());
        }

        @Test
        @DisplayName("Should skip permission when relation already exists")
        void shouldSkipPermissionWhenRelationAlreadyExists() {

            when(permissionRepository.findByName("Create User"))
                    .thenReturn(Optional.of(permission));

            when(rolePermissionRepository
                    .existsByRoleIdAndPermissionId(
                            masterRole.getId(),
                            permission.getId()
                    ))
                    .thenReturn(true);

            useCase.execute();

            verify(rolePermissionRepository)
                    .existsByRoleIdAndPermissionId(
                            masterRole.getId(),
                            permission.getId()
                    );

            verify(rolePermissionRepository, never())
                    .insert(any(RolePermissionEntity.class));
        }
    }

    @Nested
    @DisplayName("Role-Permission Creation Scenarios")
    class RolePermissionCreationScenarios {

        @BeforeEach
        void setupMasterRoleAndPermission() {

            when(properties.permissions())
                    .thenReturn(List.of(permissionConfig));

            when(roleRepository.findByName("MASTER"))
                    .thenReturn(Optional.of(masterRole));

            when(permissionRepository.findByName("Create User"))
                    .thenReturn(Optional.of(permission));

            when(rolePermissionRepository
                    .existsByRoleIdAndPermissionId(
                            masterRole.getId(),
                            permission.getId()
                    ))
                    .thenReturn(false);
        }

        @Test
        @DisplayName("Should create role-permission relation successfully")
        void shouldCreateRolePermissionSuccessfully() {

            RolePermissionEntity inserted =
                    new RolePermissionEntity();

            inserted.setId(UUID.randomUUID());

            when(rolePermissionRepository
                    .insert(any(RolePermissionEntity.class)))
                    .thenReturn(inserted);

            useCase.execute();

            ArgumentCaptor<RolePermissionEntity> captor =
                    ArgumentCaptor.forClass(
                            RolePermissionEntity.class
                    );

            verify(rolePermissionRepository)
                    .insert(captor.capture());

            RolePermissionEntity rolePermission =
                    captor.getValue();

            assertThat(rolePermission.getRoleId())
                    .isEqualTo(masterRole.getId());

            assertThat(rolePermission.getPermissionId())
                    .isEqualTo(permission.getId());

            assertThat(rolePermission.getEffect())
                    .isEqualTo(PermissionEffectEnum.ALLOW);

            assertThat(rolePermission.getConditions())
                    .isNull();

            assertThat(rolePermission.getExpiresAt())
                    .isNull();

            assertThat(rolePermission.getAssignedBy())
                    .isNull();
        }

        @Test
        @DisplayName("Should not insert more than once for a single permission")
        void shouldInsertOnlyOnceForSinglePermission() {

            when(rolePermissionRepository
                    .insert(any(RolePermissionEntity.class)))
                    .thenAnswer(invocation ->
                            invocation.getArgument(0));

            useCase.execute();

            verify(rolePermissionRepository, times(1))
                    .insert(any(RolePermissionEntity.class));
        }
    }

    @Nested
    @DisplayName("Multiple Permissions Scenarios")
    class MultiplePermissionsScenarios {

        private BootstrapProperties.PermissionConfig secondPermissionConfig;
        private PermissionEntity secondPermission;

        @BeforeEach
        void setUpMultiplePermissions() {

            secondPermissionConfig =
                    mock(BootstrapProperties.PermissionConfig.class);

            when(secondPermissionConfig.name())
                    .thenReturn("Read Users");

            secondPermission =
                    new PermissionEntity();

            secondPermission.setId(UUID.randomUUID());
            secondPermission.setName("Read Users");

            when(properties.permissions())
                    .thenReturn(
                            List.of(
                                    permissionConfig,
                                    secondPermissionConfig
                            )
                    );

            when(roleRepository.findByName("MASTER"))
                    .thenReturn(Optional.of(masterRole));

            when(permissionRepository.findByName("Create User"))
                    .thenReturn(Optional.of(permission));

            when(permissionRepository.findByName("Read Users"))
                    .thenReturn(Optional.of(secondPermission));
        }

        @Test
        @DisplayName("Should link all permissions that are not already assigned")
        void shouldLinkAllUnassignedPermissions() {

            when(rolePermissionRepository
                    .existsByRoleIdAndPermissionId(
                            masterRole.getId(),
                            permission.getId()
                    ))
                    .thenReturn(false);

            when(rolePermissionRepository
                    .existsByRoleIdAndPermissionId(
                            masterRole.getId(),
                            secondPermission.getId()
                    ))
                    .thenReturn(false);

            when(rolePermissionRepository
                    .insert(any(RolePermissionEntity.class)))
                    .thenAnswer(invocation ->
                            invocation.getArgument(0));

            useCase.execute();

            verify(rolePermissionRepository, times(2))
                    .insert(any(RolePermissionEntity.class));
        }

        @Test
        @DisplayName("Should only link permissions that are not already assigned")
        void shouldOnlyLinkUnassignedPermissions() {

            when(rolePermissionRepository
                    .existsByRoleIdAndPermissionId(
                            masterRole.getId(),
                            permission.getId()
                    ))
                    .thenReturn(true);

            when(rolePermissionRepository
                    .existsByRoleIdAndPermissionId(
                            masterRole.getId(),
                            secondPermission.getId()
                    ))
                    .thenReturn(false);

            when(rolePermissionRepository
                    .insert(any(RolePermissionEntity.class)))
                    .thenAnswer(invocation ->
                            invocation.getArgument(0));

            useCase.execute();

            verify(rolePermissionRepository, times(1))
                    .insert(any(RolePermissionEntity.class));
        }

        @Test
        @DisplayName("Should process permissions in configuration order")
        void shouldProcessPermissionsInConfigurationOrder() {

            when(rolePermissionRepository
                    .existsByRoleIdAndPermissionId(
                            any(),
                            any()
                    ))
                    .thenReturn(false);

            when(rolePermissionRepository
                    .insert(any(RolePermissionEntity.class)))
                    .thenAnswer(invocation ->
                            invocation.getArgument(0));

            useCase.execute();

            var order = inOrder(
                    permissionRepository,
                    rolePermissionRepository
            );

            order.verify(permissionRepository)
                    .findByName("Create User");

            order.verify(rolePermissionRepository)
                    .existsByRoleIdAndPermissionId(
                            masterRole.getId(),
                            permission.getId()
                    );

            order.verify(rolePermissionRepository)
                    .insert(any(RolePermissionEntity.class));

            order.verify(permissionRepository)
                    .findByName("Read Users");

            order.verify(rolePermissionRepository)
                    .existsByRoleIdAndPermissionId(
                            masterRole.getId(),
                            secondPermission.getId()
                    );

            order.verify(rolePermissionRepository)
                    .insert(any(RolePermissionEntity.class));
        }
    }

    @Nested
    @DisplayName("Unexpected Error Scenarios")
    class UnexpectedErrorScenarios {

        @Test
        @DisplayName("Should propagate exception when MASTER role lookup fails")
        void shouldPropagateExceptionWhenMasterRoleLookupFails() {

            when(properties.permissions())
                    .thenReturn(List.of(permissionConfig));

            when(roleRepository.findByName("MASTER"))
                    .thenThrow(
                            new RuntimeException(
                                    "Database connection failure"
                            )
                    );

            assertThatThrownBy(
                    () -> useCase.execute()
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(
                            "Database connection failure"
                    );

            verifyNoInteractions(
                    permissionRepository,
                    rolePermissionRepository
            );
        }

        @Test
        @DisplayName("Should propagate exception when permission lookup fails")
        void shouldPropagateExceptionWhenPermissionLookupFails() {

            when(properties.permissions())
                    .thenReturn(List.of(permissionConfig));

            when(roleRepository.findByName("MASTER"))
                    .thenReturn(Optional.of(masterRole));

            when(permissionRepository.findByName("Create User"))
                    .thenThrow(
                            new RuntimeException(
                                    "Database connection failure"
                            )
                    );

            assertThatThrownBy(
                    () -> useCase.execute()
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(
                            "Database connection failure"
                    );

            verify(roleRepository)
                    .findByName("MASTER");

            verifyNoInteractions(
                    rolePermissionRepository
            );
        }

        @Test
        @DisplayName("Should propagate exception when relation existence check fails")
        void shouldPropagateExceptionWhenRelationCheckFails() {

            when(properties.permissions())
                    .thenReturn(List.of(permissionConfig));

            when(roleRepository.findByName("MASTER"))
                    .thenReturn(Optional.of(masterRole));

            when(permissionRepository.findByName("Create User"))
                    .thenReturn(Optional.of(permission));

            when(rolePermissionRepository
                    .existsByRoleIdAndPermissionId(
                            masterRole.getId(),
                            permission.getId()
                    ))
                    .thenThrow(
                            new RuntimeException(
                                    "Database connection failure"
                            )
                    );

            assertThatThrownBy(
                    () -> useCase.execute()
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(
                            "Database connection failure"
                    );

            verify(rolePermissionRepository)
                    .existsByRoleIdAndPermissionId(
                            masterRole.getId(),
                            permission.getId()
                    );

            verify(rolePermissionRepository, never())
                    .insert(any());
        }

        @Test
        @DisplayName("Should propagate exception when role-permission insertion fails")
        void shouldPropagateExceptionWhenInsertionFails() {

            when(properties.permissions())
                    .thenReturn(List.of(permissionConfig));

            when(roleRepository.findByName("MASTER"))
                    .thenReturn(Optional.of(masterRole));

            when(permissionRepository.findByName("Create User"))
                    .thenReturn(Optional.of(permission));

            when(rolePermissionRepository
                    .existsByRoleIdAndPermissionId(
                            masterRole.getId(),
                            permission.getId()
                    ))
                    .thenReturn(false);

            doThrow(
                    new RuntimeException(
                            "Database insertion failure"
                    )
            ).when(rolePermissionRepository)
                    .insert(any(RolePermissionEntity.class));

            assertThatThrownBy(
                    () -> useCase.execute()
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(
                            "Database insertion failure"
                    );

            verify(rolePermissionRepository)
                    .insert(any(RolePermissionEntity.class));
        }
    }
}
