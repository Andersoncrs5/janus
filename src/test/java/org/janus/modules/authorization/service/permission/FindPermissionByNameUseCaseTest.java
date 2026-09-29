package org.janus.modules.authorization.service.permission;

import org.janus.modules.authorization.application.service.permission.FindPermissionByNameUseCase;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.modules.authorization.infrastructure.out.PermissionRepository;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindPermissionByNameUseCaseTest {

    @Mock
    private PermissionRepository repository;

    @InjectMocks
    private FindPermissionByNameUseCase useCase;

    @Test
    @DisplayName("Should return a successful result with the permission when found")
    void shouldReturnSuccessWhenPermissionIsFound() {
        // Arrange
        String permissionName = "Criar Usuário";
        PermissionEntity mockEntity = new PermissionEntity();
        mockEntity.setId(UUID.randomUUID());
        mockEntity.setName(permissionName);

        when(repository.findByName(permissionName)).thenReturn(Optional.of(mockEntity));

        // Act
        Result<PermissionEntity> result = useCase.execute(permissionName);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getName()).isEqualTo(permissionName);

        verify(repository, times(1)).findByName(permissionName);
    }

    @Test
    @DisplayName("Should return a failure result when permission is not found")
    void shouldReturnFailureWhenPermissionIsNotFound() {
        // Arrange
        String permissionName = "Criar Usuário";

        when(repository.findByName(permissionName)).thenReturn(Optional.empty());

        // Act
        Result<PermissionEntity> result = useCase.execute(permissionName);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getFirstError()).contains("not found");

        verify(repository, times(1)).findByName(permissionName);
    }

    @Test
    @DisplayName("Should return a failure result when name is null")
    void shouldReturnFailureWhenNameIsNull() {
        // Act
        Result<PermissionEntity> result = useCase.execute(null);

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getFirstError()).contains("cannot be null or empty");

        verify(repository, never()).findByName(anyString());
    }

    @Test
    @DisplayName("Should return a failure result when name is empty")
    void shouldReturnFailureWhenNameIsEmpty() {
        // Act
        Result<PermissionEntity> result = useCase.execute("   ");

        // Assert
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getFirstError()).contains("cannot be null or empty");

        verify(repository, never()).findByName(anyString());
    }
}
