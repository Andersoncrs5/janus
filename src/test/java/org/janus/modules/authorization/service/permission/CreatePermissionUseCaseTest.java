package org.janus.modules.authorization.service.permission;

import org.janus.modules.authorization.application.dto.permission.request.CreatePermissionDTO;
import org.janus.modules.authorization.application.dto.rolePermission.request.CreateRolePermissionDTO;
import org.janus.modules.authorization.application.mapper.PermissionMapper;
import org.janus.modules.authorization.application.service.permission.CreatePermissionUseCase;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.modules.authorization.infrastructure.in.role.IFindRoleByNameUseCase;
import org.janus.modules.authorization.infrastructure.in.rolePermission.ICreateRolePermissionUseCase;
import org.janus.modules.authorization.infrastructure.out.PermissionRepository;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.ports.in.user.IFindUserByEmailUseCase;
import org.janus.shared.domain.enums.permission.PermissionModule;
import org.janus.shared.domain.enums.permission.PermissionResource;
import org.janus.shared.domain.enums.permission.PermissionRiskLevel;
import org.janus.shared.domain.enums.rolePermission.PermissionEffectEnum;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreatePermissionUseCase Tests")
class CreatePermissionUseCaseTest {

    @Mock
    private PermissionRepository repository;

    @Mock
    private PermissionMapper mapper;

    @Mock
    private BootstrapProperties properties;

    @Mock
    private IFindRoleByNameUseCase findRoleByName;

    @Mock
    private IFindUserByEmailUseCase findUserByEmail;

    @Mock
    private ICreateRolePermissionUseCase createRolePermission;

    @Mock
    private BootstrapProperties.MasterConfig masterConfig;

    @InjectMocks
    private CreatePermissionUseCase useCase;

    private CreatePermissionDTO createPermissionDTO;
    private PermissionEntity mappedPermission;
    private PermissionEntity savedPermission;

    private UUID createdBy;
    private UUID permissionId;
    private UUID masterUserId;
    private UUID masterRoleId;

    @BeforeEach
    void setUp() {
        createdBy = UUID.randomUUID();
        permissionId = UUID.randomUUID();
        masterUserId = UUID.randomUUID();
        masterRoleId = UUID.randomUUID();

        createPermissionDTO = new CreatePermissionDTO(
                "Read Users",
                "users:read",
                "Description test",
                PermissionModule.IDENTITY,
                PermissionResource.USER,
                "read",
                PermissionRiskLevel.LOW,
                true,
                false,
                "{\"category\":\"test\"}"
        );

        mappedPermission = PermissionEntity.builder()
                .name(createPermissionDTO.name())
                .slug(createPermissionDTO.slug())
                .description(createPermissionDTO.description())
                .build();

        savedPermission = PermissionEntity.builder()
                .id(permissionId)
                .name(createPermissionDTO.name())
                .slug(createPermissionDTO.slug())
                .description(createPermissionDTO.description())
                .createdBy(createdBy)
                .build();
    }

    private void mockMasterDependencies() {
        when(properties.master())
                .thenReturn(masterConfig);

        when(masterConfig.email())
                .thenReturn("master@janus.com");

        UserEntity masterUser = UserEntity.builder()
                .id(masterUserId)
                .build();

        RoleEntity masterRole = RoleEntity.builder()
                .id(masterRoleId)
                .name("MASTER")
                .build();

        when(findUserByEmail.execute("master@janus.com"))
                .thenReturn(Result.success(masterUser));

        when(findRoleByName.execute("MASTER"))
                .thenReturn(Result.success(masterRole));

        RolePermissionEntity rolePermission =
                RolePermissionEntity.builder()
                        .id(UUID.randomUUID())
                        .roleId(masterRoleId)
                        .permissionId(permissionId)
                        .effect(PermissionEffectEnum.ALLOW)
                        .build();

        when(createRolePermission.execute(
                any(CreateRolePermissionDTO.class),
                eq(masterUserId)
        )).thenReturn(Result.created(rolePermission));
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should create permission and link it to MASTER successfully")
        void shouldCreatePermissionAndLinkToMasterSuccessfully() {
            // Arrange
            mockMasterDependencies();

            when(mapper.toEntity(createPermissionDTO))
                    .thenReturn(mappedPermission);

            when(repository.insert(mappedPermission))
                    .thenReturn(savedPermission);

            // Act
            Result<PermissionEntity> result =
                    useCase.execute(createPermissionDTO, createdBy);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isEqualTo(201);
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData().getId())
                    .isEqualTo(permissionId);
            assertThat(result.getData().getCreatedBy())
                    .isEqualTo(createdBy);

            verify(mapper)
                    .toEntity(createPermissionDTO);

            verify(repository)
                    .insert(mappedPermission);

            verify(findUserByEmail)
                    .execute("master@janus.com");

            verify(findRoleByName)
                    .execute("MASTER");

            verify(createRolePermission)
                    .execute(
                            any(CreateRolePermissionDTO.class),
                            eq(masterUserId)
                    );
        }

        @Test
        @DisplayName("Should set createdBy before inserting permission")
        void shouldSetCreatedByBeforeInsert() {
            // Arrange
            mockMasterDependencies();

            when(mapper.toEntity(createPermissionDTO))
                    .thenReturn(mappedPermission);

            when(repository.insert(mappedPermission))
                    .thenReturn(savedPermission);

            // Act
            useCase.execute(createPermissionDTO, createdBy);

            // Assert
            assertThat(mappedPermission.getCreatedBy())
                    .isEqualTo(createdBy);

            verify(repository)
                    .insert(mappedPermission);
        }

        @Test
        @DisplayName("Should link the saved permission ID to MASTER role")
        void shouldLinkSavedPermissionToMasterRole() {
            // Arrange
            mockMasterDependencies();

            when(mapper.toEntity(createPermissionDTO))
                    .thenReturn(mappedPermission);

            when(repository.insert(mappedPermission))
                    .thenReturn(savedPermission);

            ArgumentCaptor<CreateRolePermissionDTO> captor =
                    ArgumentCaptor.forClass(
                            CreateRolePermissionDTO.class
                    );

            // Act
            useCase.execute(createPermissionDTO, createdBy);

            // Assert
            verify(createRolePermission)
                    .execute(
                            captor.capture(),
                            eq(masterUserId)
                    );

            CreateRolePermissionDTO dto =
                    captor.getValue();

            assertThat(dto.roleId())
                    .isEqualTo(masterRoleId);

            assertThat(dto.permissionId())
                    .isEqualTo(permissionId);

            assertThat(dto.effect())
                    .isEqualTo(PermissionEffectEnum.ALLOW);

            assertThat(dto.conditions())
                    .isNull();

            assertThat(dto.expiresAt())
                    .isNull();
        }
    }

    @Nested
    @DisplayName("Data Integrity Exception Scenarios")
    class DataIntegrityScenarios {

        @Test
        @DisplayName("Should return 409 when slug constraint is violated")
        void shouldReturnConflictWhenSlugAlreadyExists() {
            // Arrange
            when(mapper.toEntity(createPermissionDTO))
                    .thenReturn(mappedPermission);

            when(repository.insert(mappedPermission))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "duplicate key value violates "
                                            + "unique constraint "
                                            + "\"uk_permissions_slug\""
                            )
                    );

            // Act
            Result<PermissionEntity> result =
                    useCase.execute(createPermissionDTO, createdBy);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(409);
            assertThat(result.getMessage())
                    .contains(
                            "Permission already exists with this slug"
                    );

            verify(repository)
                    .insert(mappedPermission);

            verifyNoInteractions(
                    properties,
                    findUserByEmail,
                    findRoleByName,
                    createRolePermission
            );
        }

        @Test
        @DisplayName("Should return 409 when active slug constraint is violated")
        void shouldReturnConflictWhenActiveSlugAlreadyExists() {
            // Arrange
            when(mapper.toEntity(createPermissionDTO))
                    .thenReturn(mappedPermission);

            when(repository.insert(mappedPermission))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "duplicate key value violates "
                                            + "unique constraint "
                                            + "\"uk_permissions_slug_active\""
                            )
                    );

            // Act
            Result<PermissionEntity> result =
                    useCase.execute(createPermissionDTO, createdBy);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(409);
            assertThat(result.getMessage())
                    .contains(
                            "Permission already exists with this slug"
                    );
        }

        @Test
        @DisplayName("Should return 409 when name constraint is violated")
        void shouldReturnConflictWhenNameAlreadyExists() {
            // Arrange
            when(mapper.toEntity(createPermissionDTO))
                    .thenReturn(mappedPermission);

            when(repository.insert(mappedPermission))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "duplicate key value violates "
                                            + "unique constraint "
                                            + "\"uk_permissions_name\""
                            )
                    );

            // Act
            Result<PermissionEntity> result =
                    useCase.execute(createPermissionDTO, createdBy);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(409);
            assertThat(result.getMessage())
                    .contains(
                            "Permission already exists with this name"
                    );
        }

        @Test
        @DisplayName("Should return 409 when active name constraint is violated")
        void shouldReturnConflictWhenActiveNameAlreadyExists() {
            // Arrange
            when(mapper.toEntity(createPermissionDTO))
                    .thenReturn(mappedPermission);

            when(repository.insert(mappedPermission))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "duplicate key value violates "
                                            + "unique constraint "
                                            + "\"uk_permissions_name_active\""
                            )
                    );

            // Act
            Result<PermissionEntity> result =
                    useCase.execute(createPermissionDTO, createdBy);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(409);
            assertThat(result.getMessage())
                    .contains(
                            "Permission already exists with this name"
                    );
        }

        @Test
        @DisplayName("Should return 409 when module resource and action combination already exists")
        void shouldReturnConflictWhenModuleResourceActionAlreadyExists() {
            // Arrange
            when(mapper.toEntity(createPermissionDTO))
                    .thenReturn(mappedPermission);

            when(repository.insert(mappedPermission))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "duplicate key value violates "
                                            + "unique constraint "
                                            + "\"uk_permissions_module_resource_action_active\""
                            )
                    );

            // Act
            Result<PermissionEntity> result =
                    useCase.execute(createPermissionDTO, createdBy);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(409);
            assertThat(result.getMessage())
                    .contains(
                            "Permission already exists for this "
                                    + "module, resource, and action combination"
                    );
        }

        @Test
        @DisplayName("Should return 400 when slug is empty")
        void shouldReturnBadRequestWhenSlugIsEmpty() {
            // Arrange
            when(mapper.toEntity(createPermissionDTO))
                    .thenReturn(mappedPermission);

            when(repository.insert(mappedPermission))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "violates check constraint "
                                            + "\"ck_permissions_slug_not_empty\""
                            )
                    );

            // Act
            Result<PermissionEntity> result =
                    useCase.execute(createPermissionDTO, createdBy);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(400);
            assertThat(result.getMessage())
                    .contains(
                            "Permission slug cannot be empty or blank"
                    );
        }

        @Test
        @DisplayName("Should return 400 when name is empty")
        void shouldReturnBadRequestWhenNameIsEmpty() {
            // Arrange
            when(mapper.toEntity(createPermissionDTO))
                    .thenReturn(mappedPermission);

            when(repository.insert(mappedPermission))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "violates check constraint "
                                            + "\"ck_permissions_name_not_empty\""
                            )
                    );

            // Act
            Result<PermissionEntity> result =
                    useCase.execute(createPermissionDTO, createdBy);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(400);
            assertThat(result.getMessage())
                    .contains(
                            "Permission name cannot be empty or blank"
                    );
        }

        @Test
        @DisplayName("Should return 400 when action is empty")
        void shouldReturnBadRequestWhenActionIsEmpty() {
            // Arrange
            when(mapper.toEntity(createPermissionDTO))
                    .thenReturn(mappedPermission);

            when(repository.insert(mappedPermission))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "violates check constraint "
                                            + "\"ck_permissions_action_not_empty\""
                            )
                    );

            // Act
            Result<PermissionEntity> result =
                    useCase.execute(createPermissionDTO, createdBy);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(400);
            assertThat(result.getMessage())
                    .contains(
                            "Permission action cannot be empty or blank"
                    );
        }

        @Test
        @DisplayName("Should return 400 when slug format is invalid")
        void shouldReturnBadRequestWhenSlugFormatIsInvalid() {
            // Arrange
            when(mapper.toEntity(createPermissionDTO))
                    .thenReturn(mappedPermission);

            when(repository.insert(mappedPermission))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "violates check constraint "
                                            + "\"ck_permissions_slug_format\""
                            )
                    );

            // Act
            Result<PermissionEntity> result =
                    useCase.execute(createPermissionDTO, createdBy);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(400);
            assertThat(result.getMessage())
                    .contains(
                            "Permission slug contains invalid format"
                    );
        }

        @Test
        @DisplayName("Should return 400 when action format is invalid")
        void shouldReturnBadRequestWhenActionFormatIsInvalid() {
            // Arrange
            when(mapper.toEntity(createPermissionDTO))
                    .thenReturn(mappedPermission);

            when(repository.insert(mappedPermission))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "violates check constraint "
                                            + "\"ck_permissions_action_format\""
                            )
                    );

            // Act
            Result<PermissionEntity> result =
                    useCase.execute(createPermissionDTO, createdBy);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(400);
            assertThat(result.getMessage())
                    .contains(
                            "Permission action contains invalid format"
                    );
        }

        @Test
        @DisplayName("Should return 400 when version is invalid")
        void shouldReturnBadRequestWhenVersionIsInvalid() {
            // Arrange
            when(mapper.toEntity(createPermissionDTO))
                    .thenReturn(mappedPermission);

            when(repository.insert(mappedPermission))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "violates check constraint "
                                            + "\"ck_permissions_version\""
                            )
                    );

            // Act
            Result<PermissionEntity> result =
                    useCase.execute(createPermissionDTO, createdBy);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(400);
            assertThat(result.getMessage())
                    .contains("Invalid record version");
        }

        @Test
        @DisplayName("Should return 400 when metadata is not a JSON object")
        void shouldReturnBadRequestWhenMetadataIsNotObject() {
            // Arrange
            when(mapper.toEntity(createPermissionDTO))
                    .thenReturn(mappedPermission);

            when(repository.insert(mappedPermission))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "violates check constraint "
                                            + "\"ck_permissions_metadata_is_object\""
                            )
                    );

            // Act
            Result<PermissionEntity> result =
                    useCase.execute(createPermissionDTO, createdBy);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(400);
            assertThat(result.getMessage())
                    .contains(
                            "Metadata must be a valid JSON object"
                    );
        }

        @Test
        @DisplayName("Should return 400 when createdBy foreign key is invalid")
        void shouldReturnBadRequestWhenCreatedByForeignKeyFails() {
            // Arrange
            when(mapper.toEntity(createPermissionDTO))
                    .thenReturn(mappedPermission);

            when(repository.insert(mappedPermission))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "violates foreign key constraint "
                                            + "\"fk_permissions_created_by\""
                            )
                    );

            // Act
            Result<PermissionEntity> result =
                    useCase.execute(createPermissionDTO, createdBy);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getStatus()).isEqualTo(400);
            assertThat(result.getMessage())
                    .contains(
                            "User specified in 'createdBy' does not exist"
                    );
        }

        @Test
        @DisplayName("Should delegate unknown constraint to DatabaseConstraintHandler")
        void shouldDelegateUnknownConstraintToHandler() {
            // Arrange
            when(mapper.toEntity(createPermissionDTO))
                    .thenReturn(mappedPermission);

            when(repository.insert(mappedPermission))
                    .thenThrow(
                            new DataIntegrityViolationException(
                                    "some_unknown_constraint"
                            )
                    );

            // Act
            Result<PermissionEntity> result =
                    useCase.execute(createPermissionDTO, createdBy);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isFailure()).isTrue();

            verifyNoInteractions(
                    properties,
                    findUserByEmail,
                    findRoleByName,
                    createRolePermission
            );
        }

        @Test
        @DisplayName("Should delegate to DatabaseConstraintHandler when exception message is null")
        void shouldDelegateWhenExceptionMessageIsNull() {
            // Arrange
            when(mapper.toEntity(createPermissionDTO))
                    .thenReturn(mappedPermission);

            when(repository.insert(mappedPermission))
                    .thenThrow(
                            new DataIntegrityViolationException(null)
                    );

            // Act
            Result<PermissionEntity> result =
                    useCase.execute(createPermissionDTO, createdBy);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isFailure()).isTrue();
        }
    }

    @Nested
    @DisplayName("MASTER Linking Scenarios")
    class MasterLinkingScenarios {

        @Test
        @DisplayName("Should throw InternalServerErrorException when MASTER user cannot be found")
        void shouldThrowWhenMasterUserCannotBeFound() {
            // Arrange
            when(mapper.toEntity(createPermissionDTO))
                    .thenReturn(mappedPermission);

            when(repository.insert(mappedPermission))
                    .thenReturn(savedPermission);

            when(properties.master())
                    .thenReturn(masterConfig);

            when(masterConfig.email())
                    .thenReturn("master@janus.com");

            when(findUserByEmail.execute("master@janus.com"))
                    .thenReturn(
                            Result.notFound(
                                    "Master user not found"
                            )
                    );

            // Act & Assert
            assertThatThrownBy(
                    () -> useCase.execute(
                            createPermissionDTO,
                            createdBy
                    )
            )
                    .isInstanceOf(
                            InternalServerErrorException.class
                    )
                    .hasMessage(
                            "Failed to find MASTER user"
                    );

            verify(repository)
                    .insert(mappedPermission);

            verify(findUserByEmail)
                    .execute("master@janus.com");

            verifyNoInteractions(
                    findRoleByName,
                    createRolePermission
            );
        }

        @Test
        @DisplayName("Should throw InternalServerErrorException when MASTER role cannot be found")
        void shouldThrowWhenMasterRoleCannotBeFound() {
            // Arrange
            when(mapper.toEntity(createPermissionDTO))
                    .thenReturn(mappedPermission);

            when(repository.insert(mappedPermission))
                    .thenReturn(savedPermission);

            when(properties.master())
                    .thenReturn(masterConfig);

            when(masterConfig.email())
                    .thenReturn("master@janus.com");

            UserEntity masterUser = UserEntity.builder()
                    .id(masterUserId)
                    .build();

            when(findUserByEmail.execute("master@janus.com"))
                    .thenReturn(
                            Result.success(masterUser)
                    );

            when(findRoleByName.execute("MASTER"))
                    .thenReturn(
                            Result.notFound(
                                    "MASTER role not found"
                            )
                    );

            // Act & Assert
            assertThatThrownBy(
                    () -> useCase.execute(
                            createPermissionDTO,
                            createdBy
                    )
            )
                    .isInstanceOf(
                            InternalServerErrorException.class
                    )
                    .hasMessage(
                            "Failed to find MASTER role"
                    );

            verify(findUserByEmail)
                    .execute("master@janus.com");

            verify(findRoleByName)
                    .execute("MASTER");

            verifyNoInteractions(createRolePermission);
        }

        @Test
        @DisplayName("Should throw InternalServerErrorException when permission cannot be linked to MASTER")
        void shouldThrowWhenPermissionCannotBeLinkedToMaster() {
            // Arrange
            when(mapper.toEntity(createPermissionDTO))
                    .thenReturn(mappedPermission);

            when(repository.insert(mappedPermission))
                    .thenReturn(savedPermission);

            when(properties.master())
                    .thenReturn(masterConfig);

            when(masterConfig.email())
                    .thenReturn("master@janus.com");

            UserEntity masterUser = UserEntity.builder()
                    .id(masterUserId)
                    .build();

            RoleEntity masterRole = RoleEntity.builder()
                    .id(masterRoleId)
                    .name("MASTER")
                    .build();

            when(findUserByEmail.execute("master@janus.com"))
                    .thenReturn(
                            Result.success(masterUser)
                    );

            when(findRoleByName.execute("MASTER"))
                    .thenReturn(
                            Result.success(masterRole)
                    );

            when(createRolePermission.execute(
                    any(CreateRolePermissionDTO.class),
                    eq(masterUserId)
            )).thenReturn(
                    Result.failure(
                            "Failed to create role permission",
                            500
                    )
            );

            // Act & Assert
            assertThatThrownBy(
                    () -> useCase.execute(
                            createPermissionDTO,
                            createdBy
                    )
            )
                    .isInstanceOf(
                            InternalServerErrorException.class
                    )
                    .hasMessage(
                            "Failed to link permission to MASTER role: "
                                    + "Failed to create role permission"
                    );

            verify(createRolePermission)
                    .execute(
                            any(CreateRolePermissionDTO.class),
                            eq(masterUserId)
                    );
        }
    }

    @Nested
    @DisplayName("Unexpected Error Scenarios")
    class UnexpectedErrorScenarios {

        @Test
        @DisplayName("Should throw InternalServerErrorException when mapper fails")
        void shouldThrowWhenMapperFails() {
            // Arrange
            when(mapper.toEntity(createPermissionDTO))
                    .thenThrow(
                            new RuntimeException(
                                    "Unexpected mapper failure"
                            )
                    );

            // Act & Assert
            assertThatThrownBy(
                    () -> useCase.execute(
                            createPermissionDTO,
                            createdBy
                    )
            )
                    .isInstanceOf(
                            InternalServerErrorException.class
                    )
                    .hasMessage(
                            "Unexpected mapper failure"
                    );

            verify(mapper)
                    .toEntity(createPermissionDTO);

            verifyNoInteractions(
                    repository,
                    properties,
                    findUserByEmail,
                    findRoleByName,
                    createRolePermission
            );
        }

        @Test
        @DisplayName("Should throw InternalServerErrorException when repository fails")
        void shouldThrowWhenRepositoryFails() {
            // Arrange
            when(mapper.toEntity(createPermissionDTO))
                    .thenReturn(mappedPermission);

            when(repository.insert(mappedPermission))
                    .thenThrow(
                            new RuntimeException(
                                    "Unexpected DB failure"
                            )
                    );

            // Act & Assert
            assertThatThrownBy(
                    () -> useCase.execute(
                            createPermissionDTO,
                            createdBy
                    )
            )
                    .isInstanceOf(
                            InternalServerErrorException.class
                    )
                    .hasMessage(
                            "Unexpected DB failure"
                    );

            verify(repository)
                    .insert(mappedPermission);

            verifyNoInteractions(
                    properties,
                    findUserByEmail,
                    findRoleByName,
                    createRolePermission
            );
        }

        @Test
        @DisplayName("Should throw InternalServerErrorException when finding MASTER user fails unexpectedly")
        void shouldThrowWhenFindingMasterUserFailsUnexpectedly() {
            // Arrange
            when(mapper.toEntity(createPermissionDTO))
                    .thenReturn(mappedPermission);

            when(repository.insert(mappedPermission))
                    .thenReturn(savedPermission);

            when(properties.master())
                    .thenReturn(masterConfig);

            when(masterConfig.email())
                    .thenReturn("master@janus.com");

            when(findUserByEmail.execute("master@janus.com"))
                    .thenThrow(
                            new RuntimeException(
                                    "User lookup failed"
                            )
                    );

            // Act & Assert
            assertThatThrownBy(
                    () -> useCase.execute(
                            createPermissionDTO,
                            createdBy
                    )
            )
                    .isInstanceOf(
                            InternalServerErrorException.class
                    )
                    .hasMessage(
                            "User lookup failed"
                    );
        }
    }
}