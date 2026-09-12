package org.janus.modules.authorization.service.rolePermission;

import org.janus.modules.authorization.application.dto.rolePermission.filter.RolePermissionFilterDTO;
import org.janus.modules.authorization.application.service.rolePermission.FindAllRolePermissionsUseCase;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.modules.authorization.infrastructure.out.RolePermissionRepository;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.page.Page;
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
public class FindAllRolePermissionsUseCaseTest {

    @Mock
    private RolePermissionRepository repository;

    @InjectMocks
    private FindAllRolePermissionsUseCase useCase;

    private RolePermissionFilterDTO filterDTO;
    private RolePermissionEntity rolePermissionEntity;
    private Page<RolePermissionEntity> pageResult;

    @BeforeEach
    void setUp() {
        filterDTO = new RolePermissionFilterDTO();

        rolePermissionEntity = new RolePermissionEntity();
        rolePermissionEntity.setId(UUID.randomUUID());

        pageResult = new Page<>(List.of(rolePermissionEntity), 1L, 1, 10);
    }

    @Nested
    @DisplayName("Validation Scenarios")
    class ValidationScenarios {

        @Test
        @DisplayName("Should return 400 bad request when filter DTO is null")
        void shouldReturnBadRequestWhenFilterIsNull() {
            Result<Page<RolePermissionEntity>> result = useCase.execute(null);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(400);
            assertThat(result.getMessage()).contains("RolePermissionFilterDTO must not be null");

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("Success Scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("Should successfully find and return paginated role permissions")
        void shouldFindAllRolePermissionsSuccessfully() {
            when(repository.findAll(filterDTO)).thenReturn(pageResult);

            Result<Page<RolePermissionEntity>> result = useCase.execute(filterDTO);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(200);
            assertThat(result.getValue()).isEqualTo(pageResult);

            verify(repository, times(1)).findAll(filterDTO);
        }
    }

    @Nested
    @DisplayName("Unexpected Error Scenarios")
    class UnexpectedErrorScenarios {

        @Test
        @DisplayName("Should throw InternalServerErrorException when repository throws exception")
        void shouldThrowInternalServerErrorExceptionOnFailure() {
            when(repository.findAll(filterDTO))
                    .thenThrow(new RuntimeException("Database error"));

            InternalServerErrorException exception = assertThrows(
                    InternalServerErrorException.class,
                    () -> useCase.execute(filterDTO)
            );

            assertThat(exception.getMessage()).isEqualTo("Database error");
            verify(repository, times(1)).findAll(filterDTO);
        }
    }
}