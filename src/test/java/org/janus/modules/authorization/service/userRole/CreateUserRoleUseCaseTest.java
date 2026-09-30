package org.janus.modules.authorization.service.userRole;

import org.janus.modules.authorization.application.dto.userRole.request.CreateUserRoleDTO;
import org.janus.modules.authorization.application.mapper.UserRoleMapper;
import org.janus.modules.authorization.application.service.userRole.CreateUserRoleUseCase;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.domain.entity.UserRoleEntity;
import org.janus.modules.authorization.infrastructure.in.role.IFindRoleByIdUseCase;
import org.janus.modules.authorization.infrastructure.in.userRole.IFindRolesOnlyNameByUserIdUseCase;
import org.janus.modules.authorization.infrastructure.out.UserRoleRepository;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateUserRoleUseCaseTest {

    @Mock
    private UserRoleRepository repository;

    @Mock
    private UserRoleMapper mapper;

    @Mock
    private IFindRoleByIdUseCase findRoleById;

    @Mock
    private IFindRolesOnlyNameByUserIdUseCase findRolesOnlyNameByUserId;

    @InjectMocks
    private CreateUserRoleUseCase useCase;

    private CreateUserRoleDTO dto;
    private UserRoleEntity entity;

    private RoleEntity userRoleEntity;
    private RoleEntity adminRoleEntity;
    private RoleEntity masterRoleEntity;

    private UUID assignId;
    private UUID userId;
    private UUID roleId;

    @BeforeEach
    void setUp() {
        assignId = UUID.randomUUID();
        userId = UUID.randomUUID();
        roleId = UUID.randomUUID();

        dto = new CreateUserRoleDTO();
        dto.setUserId(userId);
        dto.setRoleId(roleId);
        dto.setAssignedById(assignId);

        entity = new UserRoleEntity();
        entity.setUserId(userId);
        entity.setRoleId(roleId);
        entity.setAssignedById(assignId);

        userRoleEntity = RoleEntity.builder()
                .id(roleId)
                .name("USER")
                .isActive(true)
                .build();

        adminRoleEntity = RoleEntity.builder()
                .id(roleId)
                .name("ADMINISTRADOR")
                .isActive(true)
                .build();

        masterRoleEntity = RoleEntity.builder()
                .id(roleId)
                .name("MASTER")
                .isActive(true)
                .build();
    }

    // ============================================================
    // SUCCESS
    // ============================================================

    @Test
    @DisplayName("Should create user role successfully when valid data is provided")
    void shouldCreateUserRoleSuccessfully() {

        when(findRoleById.execute(roleId))
                .thenReturn(Result.success(userRoleEntity));

        when(findRolesOnlyNameByUserId.execute(assignId))
                .thenReturn(Result.success(List.of("USER")));

        when(mapper.toEntity(dto))
                .thenReturn(entity);

        when(repository.insert(any(UserRoleEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Result<UserRoleEntity> result =
                useCase.execute(dto, assignId);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());

        assertEquals(userId, result.getData().getUserId());
        assertEquals(roleId, result.getData().getRoleId());
        assertEquals(assignId, result.getData().getAssignedById());

        verify(findRoleById).execute(roleId);
        verify(findRolesOnlyNameByUserId).execute(assignId);
        verify(mapper).toEntity(dto);
        verify(repository).insert(entity);
    }

    @Test
    @DisplayName("Should succeed when MASTER operator assigns ADMINISTRADOR role")
    void shouldSucceedWhenMasterAssignsAdminRole() {

        when(findRoleById.execute(roleId))
                .thenReturn(Result.success(adminRoleEntity));

        when(findRolesOnlyNameByUserId.execute(assignId))
                .thenReturn(Result.success(List.of("MASTER")));

        when(mapper.toEntity(dto))
                .thenReturn(entity);

        when(repository.insert(any(UserRoleEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Result<UserRoleEntity> result =
                useCase.execute(dto, assignId);

        assertTrue(result.isSuccess());

        verify(repository).insert(entity);
    }

    @Test
    @DisplayName("Should allow ADMINISTRADOR operator to assign normal role")
    void shouldAllowAdminToAssignNormalRole() {

        when(findRoleById.execute(roleId))
                .thenReturn(Result.success(userRoleEntity));

        when(findRolesOnlyNameByUserId.execute(assignId))
                .thenReturn(Result.success(List.of("ADMINISTRADOR")));

        when(mapper.toEntity(dto))
                .thenReturn(entity);

        when(repository.insert(any(UserRoleEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Result<UserRoleEntity> result =
                useCase.execute(dto, assignId);

        assertTrue(result.isSuccess());

        verify(repository).insert(entity);
    }

    // ============================================================
    // VALIDATION
    // ============================================================

    @Test
    @DisplayName("Should return 400 when DTO is null")
    void shouldReturn400WhenDtoIsNull() {

        Result<UserRoleEntity> result =
                useCase.execute(null, assignId);

        assertTrue(result.isFailure());
        assertEquals(400, result.getStatus());
        assertEquals(
                "CreateUserRoleDTO cannot be null",
                result.getFirstError()
        );

        verifyNoInteractions(
                repository,
                mapper,
                findRoleById,
                findRolesOnlyNameByUserId
        );
    }

    @Test
    @DisplayName("Should return 400 when user ID is null")
    void shouldReturn400WhenUserIdIsNull() {

        dto.setUserId(null);

        Result<UserRoleEntity> result =
                useCase.execute(dto, assignId);

        assertTrue(result.isFailure());
        assertEquals(400, result.getStatus());
        assertEquals(
                "User, role, and operator IDs are required",
                result.getFirstError()
        );

        verifyNoInteractions(
                repository,
                mapper,
                findRoleById,
                findRolesOnlyNameByUserId
        );
    }

    @Test
    @DisplayName("Should return 400 when role ID is null")
    void shouldReturn400WhenRoleIdIsNull() {

        dto.setRoleId(null);

        Result<UserRoleEntity> result =
                useCase.execute(dto, assignId);

        assertTrue(result.isFailure());
        assertEquals(400, result.getStatus());
        assertEquals(
                "User, role, and operator IDs are required",
                result.getFirstError()
        );

        verifyNoInteractions(
                repository,
                mapper,
                findRoleById,
                findRolesOnlyNameByUserId
        );
    }

    @Test
    @DisplayName("Should return 400 when assign ID is null")
    void shouldReturn400WhenAssignIdIsNull() {

        Result<UserRoleEntity> result =
                useCase.execute(dto, null);

        assertTrue(result.isFailure());
        assertEquals(400, result.getStatus());
        assertEquals(
                "User, role, and operator IDs are required",
                result.getFirstError()
        );

        verifyNoInteractions(
                repository,
                mapper,
                findRoleById,
                findRolesOnlyNameByUserId
        );
    }

    // ============================================================
    // BUSINESS RULES
    // ============================================================

    @Test
    @DisplayName("Should return 403 when user attempts to assign role to self")
    void shouldReturn403WhenAssigningToSelf() {

        dto.setUserId(assignId);

        Result<UserRoleEntity> result =
                useCase.execute(dto, assignId);

        assertTrue(result.isFailure());
        assertEquals(403, result.getStatus());
        assertEquals(
                "Cannot assign roles to yourself",
                result.getFirstError()
        );

        verifyNoInteractions(
                repository,
                mapper,
                findRoleById,
                findRolesOnlyNameByUserId
        );
    }

    @Test
    @DisplayName("Should return role lookup failure when role cannot be found")
    void shouldReturnRoleLookupFailure() {

        when(findRoleById.execute(roleId))
                .thenReturn(Result.notFound("Role not found"));

        Result<UserRoleEntity> result =
                useCase.execute(dto, assignId);

        assertTrue(result.isFailure());
        assertEquals(404, result.getStatus());
        assertEquals(
                "Role not found",
                result.getFirstError()
        );

        verify(findRoleById).execute(roleId);

        verifyNoInteractions(
                repository,
                mapper,
                findRolesOnlyNameByUserId
        );
    }

    @Test
    @DisplayName("Should return 400 when requested role is inactive")
    void shouldReturn400WhenRoleIsInactive() {

        userRoleEntity.deactivate();

        when(findRoleById.execute(roleId))
                .thenReturn(Result.success(userRoleEntity));

        Result<UserRoleEntity> result =
                useCase.execute(dto, assignId);

        assertTrue(result.isFailure());
        assertEquals(400, result.getStatus());
        assertEquals(
                "The requested role is inactive",
                result.getFirstError()
        );

        verify(findRoleById).execute(roleId);

        verifyNoInteractions(
                repository,
                mapper,
                findRolesOnlyNameByUserId
        );
    }

    @Test
    @DisplayName("Should return 403 when attempting to assign MASTER role")
    void shouldReturn403WhenAssigningMasterRole() {

        when(findRoleById.execute(roleId))
                .thenReturn(Result.success(masterRoleEntity));

        Result<UserRoleEntity> result =
                useCase.execute(dto, assignId);

        assertTrue(result.isFailure());
        assertEquals(403, result.getStatus());
        assertEquals(
                "Assigning the MASTER role is not allowed",
                result.getFirstError()
        );

        verify(findRoleById).execute(roleId);

        verifyNoInteractions(
                repository,
                mapper,
                findRolesOnlyNameByUserId
        );
    }

    @Test
    @DisplayName("Should return 403 when non-MASTER operator attempts to assign ADMINISTRADOR role")
    void shouldReturn403WhenNonMasterAssignsAdminRole() {

        when(findRoleById.execute(roleId))
                .thenReturn(Result.success(adminRoleEntity));

        when(findRolesOnlyNameByUserId.execute(assignId))
                .thenReturn(Result.success(List.of("ADMINISTRADOR")));

        Result<UserRoleEntity> result =
                useCase.execute(dto, assignId);

        assertTrue(result.isFailure());
        assertEquals(403, result.getStatus());
        assertEquals(
                "Only MASTER users can assign the ADMINISTRADOR role",
                result.getFirstError()
        );

        verify(findRoleById).execute(roleId);
        verify(findRolesOnlyNameByUserId).execute(assignId);

        verifyNoInteractions(
                repository,
                mapper
        );
    }

    @Test
    @DisplayName("Should return operator roles lookup failure")
    void shouldReturnOperatorRolesLookupFailure() {

        when(findRoleById.execute(roleId))
                .thenReturn(Result.success(userRoleEntity));

        when(findRolesOnlyNameByUserId.execute(assignId))
                .thenReturn(Result.failure(
                        "Unable to load operator roles",
                        500
                ));

        Result<UserRoleEntity> result =
                useCase.execute(dto, assignId);

        assertTrue(result.isFailure());
        assertEquals(500, result.getStatus());
        assertEquals(
                "Unable to load operator roles",
                result.getFirstError()
        );

        verify(findRoleById).execute(roleId);
        verify(findRolesOnlyNameByUserId).execute(assignId);

        verifyNoInteractions(
                repository,
                mapper
        );
    }

    // ============================================================
    // DATABASE CONSTRAINTS
    // ============================================================

    @Test
    @DisplayName("Should return 409 when role is already assigned")
    void shouldReturnConflictWhenRoleIsAlreadyAssigned() {

        when(findRoleById.execute(roleId))
                .thenReturn(Result.success(userRoleEntity));

        when(findRolesOnlyNameByUserId.execute(assignId))
                .thenReturn(Result.success(List.of("USER")));

        when(mapper.toEntity(dto))
                .thenReturn(entity);

        DataIntegrityViolationException exception =
                new DataIntegrityViolationException(
                        "duplicate key value violates unique constraint " +
                                "\"uk_user_role_ids_user_roles\""
                );

        when(repository.insert(any(UserRoleEntity.class)))
                .thenThrow(exception);

        Result<UserRoleEntity> result =
                useCase.execute(dto, assignId);

        assertTrue(result.isFailure());
        assertEquals(409, result.getStatus());
        assertEquals(
                "The user already has this role assigned",
                result.getFirstError()
        );

        verify(repository).insert(entity);
    }

    @Test
    @DisplayName("Should return 404 when user does not exist")
    void shouldReturnNotFoundWhenUserDoesNotExist() {

        when(findRoleById.execute(roleId))
                .thenReturn(Result.success(userRoleEntity));

        when(findRolesOnlyNameByUserId.execute(assignId))
                .thenReturn(Result.success(List.of("USER")));

        when(mapper.toEntity(dto))
                .thenReturn(entity);

        DataIntegrityViolationException exception =
                new DataIntegrityViolationException(
                        "Key (user_id)=(" + userId + ") " +
                                "is not present in table users. " +
                                "Constraint: fk_user_roles_user"
                );

        when(repository.insert(any(UserRoleEntity.class)))
                .thenThrow(exception);

        Result<UserRoleEntity> result =
                useCase.execute(dto, assignId);

        assertTrue(result.isFailure());
        assertEquals(404, result.getStatus());

    }

    @Test
    @DisplayName("Should return 404 when role does not exist")
    void shouldReturnNotFoundWhenRoleDoesNotExist() {

        when(findRoleById.execute(roleId))
                .thenReturn(Result.success(userRoleEntity));

        when(findRolesOnlyNameByUserId.execute(assignId))
                .thenReturn(Result.success(List.of("USER")));

        when(mapper.toEntity(dto))
                .thenReturn(entity);

        DataIntegrityViolationException exception =
                new DataIntegrityViolationException(
                        "Key (role_id)=(" + roleId + ") " +
                                "is not present in table roles. " +
                                "Constraint: fk_user_roles_role"
                );

        when(repository.insert(any(UserRoleEntity.class)))
                .thenThrow(exception);

        Result<UserRoleEntity> result =
                useCase.execute(dto, assignId);

        assertTrue(result.isFailure());
        assertEquals(404, result.getStatus());

    }

    @Test
    @DisplayName("Should return 404 when assigned-by user does not exist")
    void shouldReturnNotFoundWhenAssignedByUserDoesNotExist() {

        when(findRoleById.execute(roleId))
                .thenReturn(Result.success(userRoleEntity));

        when(findRolesOnlyNameByUserId.execute(assignId))
                .thenReturn(Result.success(List.of("USER")));

        when(mapper.toEntity(dto))
                .thenReturn(entity);

        DataIntegrityViolationException exception =
                new DataIntegrityViolationException(
                        "Key (assigned_by)=(" + assignId + ") " +
                                "is not present in table users. " +
                                "Constraint: fk_user_roles_assigned_by"
                );

        when(repository.insert(any(UserRoleEntity.class)))
                .thenThrow(exception);

        Result<UserRoleEntity> result =
                useCase.execute(dto, assignId);

        assertTrue(result.isFailure());
        assertEquals(404, result.getStatus());

    }

    // ============================================================
    // UNEXPECTED ERROR
    // ============================================================

    @Test
    @DisplayName("Should throw InternalServerErrorException on unexpected error")
    void shouldThrowInternalServerErrorExceptionOnGenericError() {

        when(findRoleById.execute(roleId))
                .thenReturn(Result.success(userRoleEntity));

        when(findRolesOnlyNameByUserId.execute(assignId))
                .thenReturn(Result.success(List.of("USER")));

        when(mapper.toEntity(dto))
                .thenReturn(entity);

        when(repository.insert(any(UserRoleEntity.class)))
                .thenThrow(
                        new RuntimeException(
                                "Database connection timeout"
                        )
                );

        assertThrows(
                InternalServerErrorException.class,
                () -> useCase.execute(dto, assignId)
        );

        verify(repository).insert(entity);
    }
}