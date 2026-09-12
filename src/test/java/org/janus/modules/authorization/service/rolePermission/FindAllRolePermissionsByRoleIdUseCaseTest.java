package org.janus.modules.authorization.service.rolePermission;

import org.janus.modules.authorization.application.service.rolePermission.FindAllRolePermissionsByRoleIdUseCase;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FindAllRolePermissionsByRoleIdUseCaseTest {

    @Mock
    private RolePermissionRepository repository;

    @InjectMocks
    private FindAllRolePermissionsByRoleIdUseCase useCase;

    private RolePermissionEntity rolePermissionEntity;
    private UUID roleId;

    @BeforeEach
    void setUp() {
        roleId = UUID.randomUUID();

        rolePermissionEntity = new RolePermissionEntity();
        rolePermissionEntity.setId(UUID.randomUUID());
        rolePermissionEntity.setRoleId(roleId);
    }

    @Nested
    @DisplayName("Validation Scenarios")
    class ValidationScenarios {

        @Test
        @DisplayName("Should return 400 bad request when role ID is null")
        void shouldReturnBadRequestWhenRoleIdIsNull() {
            Result<List<RolePermissionEntity>> result = useCase.execute(null);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getMessage()).contains("Role ID should be defined");

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should successfully find and return role permissions by role ID")
        void shouldFindAllByRoleIdSuccessfully() {
            List<RolePermissionEntity> expectedList = List.of(rolePermissionEntity);
            when(repository.findAllByRoleId(roleId)).thenReturn(expectedList);

            Result<List<RolePermissionEntity>> result = useCase.execute(roleId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(200);
            assertThat(result.getValue()).isEqualTo(expectedList);

            verify(repository, times(1)).findAllByRoleId(roleId);
        }
    }

    @Nested
    @DisplayName("Unexpected Error Scenarios")
    class UnexpectedErrorScenarios {

        @Test
        @DisplayName("Should throw InternalServerErrorException when repository throws exception")
        void shouldThrowInternalServerErrorExceptionOnFailure() {
            when(repository.findAllByRoleId(roleId))
                    .thenThrow(new RuntimeException("Database error"));

            InternalServerErrorException exception = assertThrows(
                    InternalServerErrorException.class,
                    () -> useCase.execute(roleId)
            );

            assertThat(exception.getMessage()).isEqualTo("Database error");
            verify(repository, times(1)).findAllByRoleId(roleId);
        }
    }
}