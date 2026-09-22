package org.janus.modules.bootstrap.service.permission;

import org.janus.modules.authorization.application.dto.permission.request.CreatePermissionDTO;
import org.janus.modules.authorization.application.service.permission.CreatePermissionUseCase;
import org.janus.modules.authorization.application.service.permission.ExistsPermissionByNameUseCase;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.modules.authorization.gateway.permission.PermissionOutboundGateway;
import org.janus.modules.bootstrap.gateway.BootstrapInBoundGateway;
import org.janus.shared.domain.enums.permission.PermissionModule;
import org.janus.shared.domain.enums.permission.PermissionResource;
import org.janus.shared.domain.enums.permission.PermissionRiskLevel;
import org.janus.shared.domain.exception.InternalServerErrorException;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatePermissionUseCaseTest {

    @Mock
    private BootstrapInBoundGateway gateway;

    @Mock
    private PermissionOutboundGateway permissionOutboundGateway;

    @Mock
    private ExistsPermissionByNameUseCase existsPermissionByNameUseCase;

    @Mock
    private CreatePermissionUseCase createPermissionUseCase;

    @Mock
    private BootstrapProperties properties;

    @InjectMocks
    private CreatePermissionUseCaseBootstrap useCase;

    private BootstrapProperties.PermissionConfig permissionConfig;

    @BeforeEach
    void setUp() {
        permissionConfig = mock(BootstrapProperties.PermissionConfig.class);
        lenient().when(permissionConfig.name()).thenReturn("Criar Usuário");
        lenient().when(permissionConfig.slug()).thenReturn("users:create");
        lenient().when(permissionConfig.description()).thenReturn("Criar novos usuários no sistema");
        lenient().when(permissionConfig.module()).thenReturn(PermissionModule.AUTHORIZATION);
        lenient().when(permissionConfig.resource()).thenReturn(PermissionResource.USER);
        lenient().when(permissionConfig.action()).thenReturn("create");
        lenient().when(permissionConfig.riskLevel()).thenReturn(PermissionRiskLevel.HIGH);
        lenient().when(permissionConfig.isActive()).thenReturn(true);
        lenient().when(permissionConfig.isSystem()).thenReturn(true);
        lenient().when(permissionConfig.metadata()).thenReturn(Optional.empty());

        lenient().when(gateway.permissionOutboundGateway()).thenReturn(permissionOutboundGateway);
        lenient().when(permissionOutboundGateway.existsPermissionByNameUseCase()).thenReturn(existsPermissionByNameUseCase);
        lenient().when(permissionOutboundGateway.createPermissionUseCase()).thenReturn(createPermissionUseCase);
    }

    @Nested
    @DisplayName("Validation & Empty List Scenarios")
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
    }

    @Nested
    @DisplayName("Execution Scenarios")
    class ExecutionScenarios {

        @Test
        @DisplayName("Should create permissions successfully when permission does not exist")
        void shouldCreatePermissionSuccessfully() {
            when(properties.permissions()).thenReturn(List.of(permissionConfig));
            when(existsPermissionByNameUseCase.execute("Criar Usuário")).thenReturn(Result.ok(false));

            PermissionEntity entity = new PermissionEntity();
            entity.setId(UUID.randomUUID());

            when(createPermissionUseCase.execute(any(CreatePermissionDTO.class), isNull()))
                    .thenReturn(Result.ok(entity));

            useCase.execute();

            ArgumentCaptor<CreatePermissionDTO> captor = ArgumentCaptor.forClass(CreatePermissionDTO.class);
            verify(createPermissionUseCase, times(1)).execute(captor.capture(), isNull());

            CreatePermissionDTO dto = captor.getValue();
            assertThat(dto.name()).isEqualTo("Criar Usuário");
            assertThat(dto.slug()).isEqualTo("users:create");
            assertThat(dto.description()).isEqualTo("Criar novos usuários no sistema");
            assertThat(dto.module()).isEqualTo(PermissionModule.AUTHORIZATION);
            assertThat(dto.resource()).isEqualTo(PermissionResource.USER);
            assertThat(dto.action()).isEqualTo("create");
            assertThat(dto.riskLevel()).isEqualTo(PermissionRiskLevel.HIGH);
            assertThat(dto.isActive()).isTrue();
            assertThat(dto.isSystem()).isTrue();
            assertThat(dto.metadata()).isNull();
        }

        @Test
        @DisplayName("Should skip permission creation when permission already exists")
        void shouldSkipPermissionCreationWhenPermissionExists() {
            when(properties.permissions()).thenReturn(List.of(permissionConfig));
            when(existsPermissionByNameUseCase.execute("Criar Usuário")).thenReturn(Result.ok(true));

            useCase.execute();

            verify(existsPermissionByNameUseCase, times(1)).execute("Criar Usuário");
            verify(createPermissionUseCase, never()).execute(any(), any());
        }
    }

    @Nested
    @DisplayName("Failure Scenarios")
    class FailureScenarios {

        @Test
        @DisplayName("Should throw InternalServerErrorException when existence check fails")
        void shouldThrowInternalServerErrorExceptionWhenExistenceCheckFails() {
            when(properties.permissions()).thenReturn(List.of(permissionConfig));
            when(existsPermissionByNameUseCase.execute("Criar Usuário"))
                    .thenReturn(Result.badRequest("Database connection failure"));

            InternalServerErrorException exception = assertThrows(
                    InternalServerErrorException.class,
                    () -> useCase.execute()
            );

            assertThat(exception.getMessage()).contains("Failed to check existence for role Criar Usuário");
            verify(createPermissionUseCase, never()).execute(any(), any());
        }

        @Test
        @DisplayName("Should throw InternalServerErrorException when permission creation fails")
        void shouldThrowInternalServerErrorExceptionWhenPermissionCreationFails() {
            when(properties.permissions()).thenReturn(List.of(permissionConfig));
            when(existsPermissionByNameUseCase.execute("Criar Usuário")).thenReturn(Result.ok(false));
            when(createPermissionUseCase.execute(any(CreatePermissionDTO.class), isNull()))
                    .thenReturn(Result.badRequest("Constraint violation error"));

            InternalServerErrorException exception = assertThrows(
                    InternalServerErrorException.class,
                    () -> useCase.execute()
            );

            assertThat(exception.getMessage()).contains("Failed to create role 'Criar Usuário'");
            verify(createPermissionUseCase, times(1)).execute(any(CreatePermissionDTO.class), isNull());
        }
    }
}