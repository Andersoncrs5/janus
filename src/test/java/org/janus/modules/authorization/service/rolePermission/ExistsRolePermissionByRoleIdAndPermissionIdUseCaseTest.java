package org.janus.modules.authorization.service.rolePermission;

import org.janus.modules.authorization.application.service.rolePermission.ExistsRolePermissionByRoleIdAndPermissionIdUseCase;
import org.janus.modules.authorization.infrastructure.out.RolePermissionRepository;
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
public class ExistsRolePermissionByRoleIdAndPermissionIdUseCaseTest {

    @Mock
    private RolePermissionRepository repository;

    @InjectMocks
    private ExistsRolePermissionByRoleIdAndPermissionIdUseCase useCase;

    private UUID roleId;
    private UUID permissionId;

    @BeforeEach
    void setUp() {
        roleId = UUID.randomUUID();
        permissionId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Validation Scenarios")
    class ValidationScenarios {

        @Test
        @DisplayName("Should return 400 bad request when role ID is null")
        void shouldReturnBadRequestWhenRoleIdIsNull() {
            Result<Boolean> result = useCase.execute(null, permissionId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getMessage()).contains("Role ID should be defined");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should return 400 bad request when permission ID is null")
        void shouldReturnBadRequestWhenPermissionIdIsNull() {
            Result<Boolean> result = useCase.execute(roleId, null);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getMessage()).contains("Permission ID should be defined");

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should return true when role permission exists")
        void shouldReturnTrueWhenRolePermissionExists() {
            when(repository.existsByRoleIdAndPermissionId(roleId, permissionId)).thenReturn(true);

            Result<Boolean> result = useCase.execute(roleId, permissionId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(200);
            assertThat(result.getValue()).isTrue();

            verify(repository, times(1)).existsByRoleIdAndPermissionId(roleId, permissionId);
        }

        @Test
        @DisplayName("Should return false when role permission does not exist")
        void shouldReturnFalseWhenRolePermissionDoesNotExist() {
            when(repository.existsByRoleIdAndPermissionId(roleId, permissionId)).thenReturn(false);

            Result<Boolean> result = useCase.execute(roleId, permissionId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(200);
            assertThat(result.getValue()).isFalse();

            verify(repository, times(1)).existsByRoleIdAndPermissionId(roleId, permissionId);
        }
    }

    @Nested
    @DisplayName("Unexpected Error Scenarios")
    class UnexpectedErrorScenarios {

        @Test
        @DisplayName("Should throw InternalServerErrorException when repository throws exception")
        void shouldThrowInternalServerErrorExceptionOnFailure() {
            when(repository.existsByRoleIdAndPermissionId(roleId, permissionId))
                    .thenThrow(new RuntimeException("Database error"));

            InternalServerErrorException exception = assertThrows(
                    InternalServerErrorException.class,
                    () -> useCase.execute(roleId, permissionId)
            );

            assertThat(exception.getMessage()).isEqualTo("Database error");
            verify(repository, times(1)).existsByRoleIdAndPermissionId(roleId, permissionId);
        }
    }
}