package org.janus.modules.bootstrap.service.permission;

import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.modules.authorization.infrastructure.out.PermissionRepository;
import org.janus.shared.domain.enums.permission.PermissionModule;
import org.janus.shared.domain.enums.permission.PermissionResource;
import org.janus.shared.domain.enums.permission.PermissionRiskLevel;
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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatePermissionUseCaseTest {

    @Mock
    private BootstrapProperties properties;

    @Mock
    private PermissionRepository repository;

    @InjectMocks
    private CreatePermissionUseCaseBootstrap useCase;

    private BootstrapProperties.PermissionConfig permissionConfig;

    @BeforeEach
    void setUp() {
        permissionConfig = mock(BootstrapProperties.PermissionConfig.class);

        lenient().when(permissionConfig.name())
                .thenReturn("Criar Usuário");

        lenient().when(permissionConfig.slug())
                .thenReturn("users:create");

        lenient().when(permissionConfig.description())
                .thenReturn("Criar novos usuários no sistema");

        lenient().when(permissionConfig.module())
                .thenReturn(PermissionModule.AUTHORIZATION);

        lenient().when(permissionConfig.resource())
                .thenReturn(PermissionResource.USER);

        lenient().when(permissionConfig.action())
                .thenReturn("create");

        lenient().when(permissionConfig.riskLevel())
                .thenReturn(PermissionRiskLevel.HIGH);

        lenient().when(permissionConfig.isActive())
                .thenReturn(true);

        lenient().when(permissionConfig.isSystem())
                .thenReturn(true);

        lenient().when(permissionConfig.metadata())
                .thenReturn(Optional.empty());
    }

    @Nested
    @DisplayName("Validation & Empty List Scenarios")
    class EarlyReturnScenarios {

        @Test
        @DisplayName("Should return early when permissions list is null")
        void shouldReturnEarlyWhenPermissionsListIsNull() {

            when(properties.permissions()).thenReturn(null);

            assertDoesNotThrow(() -> useCase.execute());

            verify(properties).permissions();
            verifyNoInteractions(repository);
        }

    }

    @Nested
    @DisplayName("Execution Scenarios")
    class ExecutionScenarios {

        @Test
        @DisplayName("Should create permission successfully when permission does not exist")
        void shouldCreatePermissionSuccessfully() {

            when(properties.permissions())
                    .thenReturn(List.of(permissionConfig));

            when(repository.existsByName("Criar Usuário"))
                    .thenReturn(false);

            UUID permissionId = UUID.randomUUID();

            PermissionEntity inserted = PermissionEntity.builder()
                    .id(permissionId)
                    .name("Criar Usuário")
                    .slug("users:create")
                    .description("Criar novos usuários no sistema")
                    .module(PermissionModule.AUTHORIZATION)
                    .resource(PermissionResource.USER)
                    .action("create")
                    .riskLevel(PermissionRiskLevel.HIGH)
                    .isActive(true)
                    .isSystem(true)
                    .metadata(null)
                    .createdBy(null)
                    .build();

            when(repository.insert(any(PermissionEntity.class)))
                    .thenReturn(inserted);

            useCase.execute();

            ArgumentCaptor<PermissionEntity> captor =
                    ArgumentCaptor.forClass(PermissionEntity.class);

            verify(repository).existsByName("Criar Usuário");
            verify(repository).insert(captor.capture());

            PermissionEntity permission = captor.getValue();

            assertThat(permission.getName())
                    .isEqualTo("Criar Usuário");

            assertThat(permission.getSlug())
                    .isEqualTo("users:create");

            assertThat(permission.getDescription())
                    .isEqualTo("Criar novos usuários no sistema");

            assertThat(permission.getModule())
                    .isEqualTo(PermissionModule.AUTHORIZATION);

            assertThat(permission.getResource())
                    .isEqualTo(PermissionResource.USER);

            assertThat(permission.getAction())
                    .isEqualTo("create");

            assertThat(permission.getRiskLevel())
                    .isEqualTo(PermissionRiskLevel.HIGH);

            assertThat(permission.getIsActive())
                    .isTrue();

            assertThat(permission.getIsSystem())
                    .isTrue();

            assertThat(permission.getMetadata())
                    .isNull();

            assertThat(permission.getCreatedBy())
                    .isNull();
        }

        @Test
        @DisplayName("Should skip permission creation when permission already exists")
        void shouldSkipPermissionCreationWhenPermissionExists() {

            when(properties.permissions())
                    .thenReturn(List.of(permissionConfig));

            when(repository.existsByName("Criar Usuário"))
                    .thenReturn(true);

            useCase.execute();

            verify(repository)
                    .existsByName("Criar Usuário");

            verify(repository, never())
                    .insert(any(PermissionEntity.class));
        }

        @Test
        @DisplayName("Should process multiple permissions")
        void shouldProcessMultiplePermissions() {

            BootstrapProperties.PermissionConfig secondPermission =
                    mock(BootstrapProperties.PermissionConfig.class);

            when(secondPermission.name())
                    .thenReturn("Visualizar Usuários");

            when(secondPermission.slug())
                    .thenReturn("users:read");

            when(secondPermission.description())
                    .thenReturn("Visualizar informações e listas de usuários");

            when(secondPermission.module())
                    .thenReturn(PermissionModule.AUTHORIZATION);

            when(secondPermission.resource())
                    .thenReturn(PermissionResource.USER);

            when(secondPermission.action())
                    .thenReturn("read");

            when(secondPermission.riskLevel())
                    .thenReturn(PermissionRiskLevel.LOW);

            when(secondPermission.isActive())
                    .thenReturn(true);

            when(secondPermission.isSystem())
                    .thenReturn(true);

            when(secondPermission.metadata())
                    .thenReturn(Optional.empty());

            when(properties.permissions())
                    .thenReturn(List.of(
                            permissionConfig,
                            secondPermission
                    ));

            when(repository.existsByName(anyString()))
                    .thenReturn(false);

            when(repository.insert(any(PermissionEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            useCase.execute();

            verify(repository)
                    .existsByName("Criar Usuário");

            verify(repository)
                    .existsByName("Visualizar Usuários");

            verify(repository, times(2))
                    .insert(any(PermissionEntity.class));
        }
    }

    @Nested
    @DisplayName("Failure Scenarios")
    class FailureScenarios {

        @Test
        @DisplayName("Should propagate exception when existence check fails")
        void shouldPropagateExceptionWhenExistenceCheckFails() {

            when(properties.permissions())
                    .thenReturn(List.of(permissionConfig));

            when(repository.existsByName("Criar Usuário"))
                    .thenThrow(new RuntimeException("Database connection failure"));

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> useCase.execute()
            );

            assertThat(exception)
                    .hasMessage("Database connection failure");

            verify(repository)
                    .existsByName("Criar Usuário");

            verify(repository, never())
                    .insert(any(PermissionEntity.class));
        }

        @Test
        @DisplayName("Should propagate exception when permission creation fails")
        void shouldPropagateExceptionWhenPermissionCreationFails() {

            when(properties.permissions())
                    .thenReturn(List.of(permissionConfig));

            when(repository.existsByName("Criar Usuário"))
                    .thenReturn(false);

            when(repository.insert(any(PermissionEntity.class)))
                    .thenThrow(new RuntimeException("Database insertion failure"));

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> useCase.execute()
            );

            assertThat(exception)
                    .hasMessage("Database insertion failure");

            verify(repository)
                    .existsByName("Criar Usuário");

            verify(repository)
                    .insert(any(PermissionEntity.class));
        }
    }

    @Nested
    @DisplayName("Dependency Interaction Scenarios")
    class DependencyInteractionScenarios {

        @Test
        @DisplayName("Should check existence before inserting permission")
        void shouldCheckExistenceBeforeInsert() {

            when(properties.permissions())
                    .thenReturn(List.of(permissionConfig));

            when(repository.existsByName("Criar Usuário"))
                    .thenReturn(false);

            when(repository.insert(any(PermissionEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            useCase.execute();

            var inOrder = inOrder(repository);

            inOrder.verify(repository)
                    .existsByName("Criar Usuário");

            inOrder.verify(repository)
                    .insert(any(PermissionEntity.class));
        }

        @Test
        @DisplayName("Should not insert when permission already exists")
        void shouldNotInsertWhenPermissionAlreadyExists() {

            when(properties.permissions())
                    .thenReturn(List.of(permissionConfig));

            when(repository.existsByName("Criar Usuário"))
                    .thenReturn(true);

            useCase.execute();

            verify(repository)
                    .existsByName("Criar Usuário");

            verify(repository, never())
                    .insert(any(PermissionEntity.class));

            verifyNoMoreInteractions(repository);
        }
    }
}