package org.janus.modules.authorization.service.permission;

import org.janus.modules.authorization.application.service.permission.ExistsPermissionByIdUseCase;
import org.janus.modules.authorization.infrastructure.out.PermissionRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExistsPermissionByIdUseCaseTest {

    @Mock
    private PermissionRepository repository;

    @InjectMocks
    private ExistsPermissionByIdUseCase useCase;

    private UUID permissionId;

    @BeforeEach
    void setUp() {
        permissionId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Execute Tests")
    class ExecuteTests {

        @Test
        @DisplayName("Should return Bad Request result when permission ID is null")
        void execute_ShouldReturnBadRequest_WhenIdIsNull() {
            Result<Boolean> result = useCase.execute(null);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("Permission id should be defined");
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should return true when permission exists")
        void execute_ShouldReturnTrue_WhenPermissionExists() {
            when(repository.existsById(permissionId)).thenReturn(true);

            Result<Boolean> result = useCase.execute(permissionId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isTrue();
            verify(repository, times(1)).existsById(permissionId);
        }

        @Test
        @DisplayName("Should return false when permission does not exist")
        void execute_ShouldReturnFalse_WhenPermissionDoesNotExist() {
            when(repository.existsById(permissionId)).thenReturn(false);

            Result<Boolean> result = useCase.execute(permissionId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isFalse();
            verify(repository, times(1)).existsById(permissionId);
        }
    }
}