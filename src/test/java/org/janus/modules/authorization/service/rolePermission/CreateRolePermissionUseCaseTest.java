package org.janus.modules.authorization.service.rolePermission;

import org.janus.modules.authorization.application.dto.rolePermission.request.CreateRolePermissionDTO;
import org.janus.modules.authorization.application.mapper.RolePermissionMapper;
import org.janus.modules.authorization.application.service.rolePermission.CreateRolePermissionUseCase;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.modules.authorization.infrastructure.out.RolePermissionRepository;
import org.janus.shared.domain.enums.rolePermission.PermissionEffectEnum;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateRolePermissionUseCaseTest {

    @Mock
    private RolePermissionRepository repository;

    @Mock
    private RolePermissionMapper mapper;

    @InjectMocks
    private CreateRolePermissionUseCase useCase;

    private CreateRolePermissionDTO createRolePermissionDTO;
    private RolePermissionEntity rolePermissionEntity;
    private UUID assignedBy;
    private UUID roleId;
    private UUID permissionId;

    @BeforeEach
    void setUp() {
        roleId = UUID.randomUUID();
        permissionId = UUID.randomUUID();
        assignedBy = UUID.randomUUID();

        createRolePermissionDTO = new CreateRolePermissionDTO(
                roleId,
                permissionId,
                PermissionEffectEnum.ALLOW,
                null,
                null
        );

        rolePermissionEntity = new RolePermissionEntity();
        rolePermissionEntity.setRoleId(roleId);
        rolePermissionEntity.setPermissionId(permissionId);
    }

    @Nested
    @DisplayName("Validation Scenarios")
    class ValidationScenarios {

        @Test
        @DisplayName("Should return 400 bad request when DTO is null")
        void shouldReturnBadRequestWhenDtoIsNull() {
            Result<RolePermissionEntity> result = useCase.execute(null, assignedBy);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getMessage()).contains("CreateRolePermissionDTO must not be null");

            verifyNoInteractions(mapper, repository);
        }
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should successfully assign permission to role")
        void shouldCreateRolePermissionSuccessfully() {
            when(mapper.toEntity(createRolePermissionDTO)).thenReturn(rolePermissionEntity);
            when(repository.insert(rolePermissionEntity)).thenReturn(rolePermissionEntity);

            Result<RolePermissionEntity> result = useCase.execute(createRolePermissionDTO, assignedBy);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue()).isEqualTo(rolePermissionEntity);
            assertThat(rolePermissionEntity.getAssignedBy()).isEqualTo(assignedBy);

            verify(mapper, times(1)).toEntity(createRolePermissionDTO);
            verify(repository, times(1)).insert(rolePermissionEntity);
        }
    }

    @Nested
    @DisplayName("Data Integrity Constraint Scenarios")
    class DataIntegrityScenarios {

        @Test
        @DisplayName("Should return 409 conflict when uk_role_permissions_active constraint fails")
        void shouldReturn409WhenPermissionAlreadyAssigned() {
            String errorMessage = "Data integrity violation: uk_role_permissions_active constraint failed";
            when(mapper.toEntity(createRolePermissionDTO)).thenReturn(rolePermissionEntity);
            when(repository.insert(rolePermissionEntity))
                    .thenThrow(new DataIntegrityViolationException(errorMessage));

            Result<RolePermissionEntity> result = useCase.execute(createRolePermissionDTO, assignedBy);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(409);
            assertThat(result.getMessage().isPresent()).isTrue();
            assertThat(result.getMessage().get()).contains("Permission is already assigned to this role");
        }

        @Test
        @DisplayName("Should return 404 not found when fk_role_permissions_role constraint fails")
        void shouldReturn404WhenRoleNotFound() {
            String errorMessage = "Data integrity violation: fk_role_permissions_role constraint failed";
            when(mapper.toEntity(createRolePermissionDTO)).thenReturn(rolePermissionEntity);
            when(repository.insert(rolePermissionEntity))
                    .thenThrow(new DataIntegrityViolationException(errorMessage));

            Result<RolePermissionEntity> result = useCase.execute(createRolePermissionDTO, assignedBy);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(404);
            assertThat(result.getMessage().isPresent()).isTrue();
            assertThat(result.getMessage().get()).contains("Role not found with ID: " + roleId);
        }

        @Test
        @DisplayName("Should return 404 not found when fk_role_permissions_permission constraint fails")
        void shouldReturn404WhenPermissionNotFound() {
            String errorMessage = "Data integrity violation: fk_role_permissions_permission constraint failed";
            when(mapper.toEntity(createRolePermissionDTO)).thenReturn(rolePermissionEntity);
            when(repository.insert(rolePermissionEntity))
                    .thenThrow(new DataIntegrityViolationException(errorMessage));

            Result<RolePermissionEntity> result = useCase.execute(createRolePermissionDTO, assignedBy);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(404);
            assertThat(result.getMessage().isPresent()).isTrue();
            assertThat(result.getMessage().get()).contains("Permission not found with ID: " + permissionId);
        }

        @Test
        @DisplayName("Should return 400 bad request when fk_role_permissions_assigned_by constraint fails")
        void shouldReturn400WhenAssignedByUserNotFound() {
            String errorMessage = "Data integrity violation: fk_role_permissions_assigned_by constraint failed";
            when(mapper.toEntity(createRolePermissionDTO)).thenReturn(rolePermissionEntity);
            when(repository.insert(rolePermissionEntity))
                    .thenThrow(new DataIntegrityViolationException(errorMessage));

            Result<RolePermissionEntity> result = useCase.execute(createRolePermissionDTO, assignedBy);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getMessage().isPresent()).isTrue();
            assertThat(result.getMessage().get()).contains("User assigned by not found with ID: " + assignedBy);
        }

        @Test
        @DisplayName("Should return 400 bad request when ck_role_permissions_version constraint fails")
        void shouldReturn400WhenInvalidVersion() {
            String errorMessage = "Data integrity violation: ck_role_permissions_version constraint failed";
            when(mapper.toEntity(createRolePermissionDTO)).thenReturn(rolePermissionEntity);
            when(repository.insert(rolePermissionEntity))
                    .thenThrow(new DataIntegrityViolationException(errorMessage));

            Result<RolePermissionEntity> result = useCase.execute(createRolePermissionDTO, assignedBy);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getMessage().isPresent()).isTrue();
            assertThat(result.getMessage().get()).contains("Invalid record version");
        }

        @Test
        @DisplayName("Should return 400 bad request when ck_role_permissions_expires_at_future constraint fails")
        void shouldReturn400WhenExpirationDateNotInFuture() {
            String errorMessage = "Data integrity violation: ck_role_permissions_expires_at_future constraint failed";
            when(mapper.toEntity(createRolePermissionDTO)).thenReturn(rolePermissionEntity);
            when(repository.insert(rolePermissionEntity))
                    .thenThrow(new DataIntegrityViolationException(errorMessage));

            Result<RolePermissionEntity> result = useCase.execute(createRolePermissionDTO, assignedBy);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getMessage().isPresent()).isTrue();
            assertThat(result.getMessage().isPresent()).isTrue();
            assertThat(result.getMessage().get()).contains("Expiration date must be in the future");
        }

        @Test
        @DisplayName("Should handle constraint exception when exception message is null")
        void shouldHandleExceptionWhenMessageIsNull() {
            when(mapper.toEntity(createRolePermissionDTO)).thenReturn(rolePermissionEntity);
            when(repository.insert(rolePermissionEntity))
                    .thenThrow(new DataIntegrityViolationException(null));

            Result<RolePermissionEntity> result = useCase.execute(createRolePermissionDTO, assignedBy);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("Should handle constraint exception on unknown constraint name")
        void shouldHandleExceptionOnUnknownConstraint() {
            when(mapper.toEntity(createRolePermissionDTO)).thenReturn(rolePermissionEntity);
            when(repository.insert(rolePermissionEntity))
                    .thenThrow(new DataIntegrityViolationException("unknown_constraint_name"));

            Result<RolePermissionEntity> result = useCase.execute(createRolePermissionDTO, assignedBy);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
        }
    }

    @Nested
    @DisplayName("Unexpected Error Scenarios")
    class UnexpectedErrorScenarios {

        @Test
        @DisplayName("Should throw InternalServerErrorException when generic exception occurs")
        void shouldThrowInternalServerErrorExceptionOnGenericFailure() {
            when(mapper.toEntity(createRolePermissionDTO)).thenReturn(rolePermissionEntity);
            when(repository.insert(rolePermissionEntity))
                    .thenThrow(new RuntimeException("Database connection timeout"));

            InternalServerErrorException exception = assertThrows(
                    InternalServerErrorException.class,
                    () -> useCase.execute(createRolePermissionDTO, assignedBy)
            );

            assertThat(exception.getMessage()).isEqualTo("Database connection timeout");
        }
    }
}