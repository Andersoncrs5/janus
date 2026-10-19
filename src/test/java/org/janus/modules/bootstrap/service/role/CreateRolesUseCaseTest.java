package org.janus.modules.bootstrap.service.role;

import org.janus.modules.authorization.application.dto.role.request.CreateRoleDTO;
import org.janus.modules.authorization.application.service.role.CreateRoleUseCase;
import org.janus.modules.authorization.application.service.role.ExistsRoleByNameUseCase;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.bootstrap.gateway.BootstrapInBoundGateway;
import org.janus.shared.domain.exception.InternalServerErrorException;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

        lenient()
                .when(adminRoleConfig.name())
                .thenReturn("Administrator");

        lenient()
                .when(adminRoleConfig.description())
                .thenReturn("System Administrator Role");

        lenient()
                .when(adminRoleConfig.slug())
                .thenReturn("admin");

        userRoleConfig = mock(BootstrapProperties.RoleConfig.class);

        lenient()
                .when(userRoleConfig.name())
                .thenReturn("Regular User");

        lenient()
                .when(userRoleConfig.description())
                .thenReturn("Standard User Role");

        lenient()
                .when(userRoleConfig.slug())
                .thenReturn("user");

        lenient()
                .when(gateway.existsRoleByNameUseCase())
                .thenReturn(existsRoleByNameUseCase);

        lenient()
                .when(gateway.createRoleUseCase())
                .thenReturn(createRoleUseCase);
    }

    @Test
    @DisplayName("Should throw exception when roles list is null")
    void shouldThrowExceptionWhenRolesListIsNull() {

        when(properties.roles()).thenReturn(null);

        assertThatThrownBy(() -> useCase.execute())
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessage("No initial roles defined for bootstrap.");

        verifyNoInteractions(gateway);
    }

    @Test
    @DisplayName("Should throw exception when roles list is empty")
    void shouldThrowExceptionWhenRolesListIsEmpty() {

        when(properties.roles())
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> useCase.execute())
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessage("No initial roles defined for bootstrap.");

        verifyNoInteractions(gateway);
    }

    @Test
    @DisplayName("Should create all roles successfully when none exist")
    void shouldCreateRolesSuccessfully() {

        when(properties.roles())
                .thenReturn(List.of(
                        adminRoleConfig,
                        userRoleConfig
                ));

        when(existsRoleByNameUseCase.execute("Administrator"))
                .thenReturn(Result.success(false));

        when(existsRoleByNameUseCase.execute("Regular User"))
                .thenReturn(Result.success(false));

        RoleEntity adminEntity = new RoleEntity();
        adminEntity.setId(UUID.randomUUID());

        RoleEntity userEntity = new RoleEntity();
        userEntity.setId(UUID.randomUUID());

        when(createRoleUseCase.execute(any(CreateRoleDTO.class)))
                .thenReturn(Result.success(adminEntity))
                .thenReturn(Result.success(userEntity));

        useCase.execute();

        verify(gateway, times(2))
                .existsRoleByNameUseCase();

        verify(gateway, times(2))
                .createRoleUseCase();

        verify(existsRoleByNameUseCase)
                .execute("Administrator");

        verify(existsRoleByNameUseCase)
                .execute("Regular User");

        ArgumentCaptor<CreateRoleDTO> captor =
                ArgumentCaptor.forClass(CreateRoleDTO.class);

        verify(createRoleUseCase, times(2))
                .execute(captor.capture());

        List<CreateRoleDTO> capturedDtos =
                captor.getAllValues();

        CreateRoleDTO adminDto = capturedDtos.get(0);

        assertEquals(
                "Administrator",
                adminDto.name()
        );

        assertEquals(
                "System Administrator Role",
                adminDto.description()
        );

        assertEquals(
                "admin",
                adminDto.slug()
        );

        CreateRoleDTO userDto = capturedDtos.get(1);

        assertEquals(
                "Regular User",
                userDto.name()
        );

        assertEquals(
                "Standard User Role",
                userDto.description()
        );

        assertEquals(
                "user",
                userDto.slug()
        );
    }

    @Test
    @DisplayName("Should skip role creation when role already exists")
    void shouldSkipRoleCreationWhenRoleExists() {

        when(properties.roles())
                .thenReturn(List.of(adminRoleConfig));

        when(existsRoleByNameUseCase.execute("Administrator"))
                .thenReturn(Result.success(true));

        useCase.execute();

        verify(existsRoleByNameUseCase)
                .execute("Administrator");

        verify(createRoleUseCase, never())
                .execute(any(CreateRoleDTO.class));
    }

    @Test
    @DisplayName("Should skip existing roles and create missing roles")
    void shouldSkipExistingRolesAndCreateMissingRoles() {

        when(properties.roles())
                .thenReturn(List.of(
                        adminRoleConfig,
                        userRoleConfig
                ));

        when(existsRoleByNameUseCase.execute("Administrator"))
                .thenReturn(Result.success(true));

        when(existsRoleByNameUseCase.execute("Regular User"))
                .thenReturn(Result.success(false));

        RoleEntity userEntity = new RoleEntity();
        userEntity.setId(UUID.randomUUID());

        when(createRoleUseCase.execute(any(CreateRoleDTO.class)))
                .thenReturn(Result.success(userEntity));

        useCase.execute();

        verify(existsRoleByNameUseCase)
                .execute("Administrator");

        verify(existsRoleByNameUseCase)
                .execute("Regular User");

        ArgumentCaptor<CreateRoleDTO> captor =
                ArgumentCaptor.forClass(CreateRoleDTO.class);

        verify(createRoleUseCase)
                .execute(captor.capture());

        CreateRoleDTO dto = captor.getValue();

        assertEquals(
                "Regular User",
                dto.name()
        );

        assertEquals(
                "Standard User Role",
                dto.description()
        );

        assertEquals(
                "user",
                dto.slug()
        );
    }

    @Test
    @DisplayName("Should throw exception when role existence check fails")
    void shouldThrowExceptionWhenExistenceCheckFails() {

        when(properties.roles())
                .thenReturn(List.of(
                        adminRoleConfig,
                        userRoleConfig
                ));

        when(existsRoleByNameUseCase.execute("Administrator"))
                .thenReturn(
                        Result.failure(
                                "Database error",
                                500
                        )
                );

        assertThatThrownBy(() -> useCase.execute())
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessage(
                        "Failed to check existence for role 'Administrator': Database error"
                );

        verify(existsRoleByNameUseCase)
                .execute("Administrator");

        verify(existsRoleByNameUseCase, never())
                .execute("Regular User");

        verify(createRoleUseCase, never())
                .execute(any(CreateRoleDTO.class));
    }

    @Test
    @DisplayName("Should throw exception when role creation fails")
    void shouldThrowExceptionWhenRoleCreationFails() {

        when(properties.roles())
                .thenReturn(List.of(
                        adminRoleConfig,
                        userRoleConfig
                ));

        when(existsRoleByNameUseCase.execute("Administrator"))
                .thenReturn(Result.success(false));

        when(createRoleUseCase.execute(any(CreateRoleDTO.class)))
                .thenReturn(
                        Result.failure(
                                "Constraint error",
                                500
                        )
                );

        assertThatThrownBy(() -> useCase.execute())
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessage(
                        "Failed to create role 'Administrator': Constraint error"
                );

        verify(existsRoleByNameUseCase)
                .execute("Administrator");

        verify(createRoleUseCase)
                .execute(any(CreateRoleDTO.class));

        verify(existsRoleByNameUseCase, never())
                .execute("Regular User");

        verify(createRoleUseCase, times(1))
                .execute(any(CreateRoleDTO.class));
    }

    @Test
    @DisplayName("Should propagate unexpected exception from existence check")
    void shouldPropagateUnexpectedExceptionFromExistenceCheck() {

        when(properties.roles())
                .thenReturn(List.of(adminRoleConfig));

        RuntimeException exception =
                new RuntimeException("Unexpected database failure");

        when(existsRoleByNameUseCase.execute("Administrator"))
                .thenThrow(exception);

        assertThatThrownBy(() -> useCase.execute())
                .isSameAs(exception);

        verify(existsRoleByNameUseCase)
                .execute("Administrator");

        verify(createRoleUseCase, never())
                .execute(any(CreateRoleDTO.class));
    }

    @Test
    @DisplayName("Should propagate unexpected exception from role creation")
    void shouldPropagateUnexpectedExceptionFromRoleCreation() {

        when(properties.roles())
                .thenReturn(List.of(adminRoleConfig));

        when(existsRoleByNameUseCase.execute("Administrator"))
                .thenReturn(Result.success(false));

        RuntimeException exception =
                new RuntimeException("Unexpected creation failure");

        when(createRoleUseCase.execute(any(CreateRoleDTO.class)))
                .thenThrow(exception);

        assertThatThrownBy(() -> useCase.execute())
                .isSameAs(exception);

        verify(existsRoleByNameUseCase)
                .execute("Administrator");

        verify(createRoleUseCase)
                .execute(any(CreateRoleDTO.class));
    }

    @Test
    @DisplayName("Should process roles in configuration order")
    void shouldProcessRolesInConfigurationOrder() {

        when(properties.roles())
                .thenReturn(List.of(
                        adminRoleConfig,
                        userRoleConfig
                ));

        when(existsRoleByNameUseCase.execute(anyString()))
                .thenReturn(Result.success(false));

        RoleEntity adminEntity = new RoleEntity();
        adminEntity.setId(UUID.randomUUID());

        RoleEntity userEntity = new RoleEntity();
        userEntity.setId(UUID.randomUUID());

        when(createRoleUseCase.execute(any(CreateRoleDTO.class)))
                .thenReturn(Result.success(adminEntity))
                .thenReturn(Result.success(userEntity));

        useCase.execute();

        ArgumentCaptor<CreateRoleDTO> captor =
                ArgumentCaptor.forClass(CreateRoleDTO.class);

        verify(createRoleUseCase, times(2))
                .execute(captor.capture());

        List<CreateRoleDTO> dtos =
                captor.getAllValues();

        assertEquals(
                "Administrator",
                dtos.get(0).name()
        );

        assertEquals(
                "Regular User",
                dtos.get(1).name()
        );
    }

    @Test
    @DisplayName("Should not create any role when all roles already exist")
    void shouldNotCreateAnyRoleWhenAllRolesAlreadyExist() {

        when(properties.roles())
                .thenReturn(List.of(
                        adminRoleConfig,
                        userRoleConfig
                ));

        when(existsRoleByNameUseCase.execute("Administrator"))
                .thenReturn(Result.success(true));

        when(existsRoleByNameUseCase.execute("Regular User"))
                .thenReturn(Result.success(true));

        useCase.execute();

        verify(existsRoleByNameUseCase)
                .execute("Administrator");

        verify(existsRoleByNameUseCase)
                .execute("Regular User");

        verify(createRoleUseCase, never())
                .execute(any(CreateRoleDTO.class));
    }
}