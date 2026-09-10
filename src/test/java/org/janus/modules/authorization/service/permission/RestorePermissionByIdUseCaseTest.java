package org.janus.modules.authorization.service.permission;

import org.janus.modules.authorization.application.service.permission.RestorePermissionByIdUseCase;
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
class RestorePermissionByIdUseCaseTest {

    @Mock
    private PermissionRepository repository;

    @InjectMocks
    private RestorePermissionByIdUseCase useCase;

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
            Result<Void> result = useCase.execute(null);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("Permission id should be defined");
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should return Not Found result when repository updates 0 rows")
        void execute_ShouldReturnNotFound_WhenNoRowsUpdated() {
            when(repository.restoreById(permissionId)).thenReturn(0);

            Result<Void> result = useCase.execute(permissionId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("Permission not found or not deleted with ID: " + permissionId);
            verify(repository, times(1)).restoreById(permissionId);
        }

        @Test
        @DisplayName("Should return OK result when permission is successfully restored")
        void execute_ShouldReturnOk_WhenPermissionRestored() {
            when(repository.restoreById(permissionId)).thenReturn(1);

            Result<Void> result = useCase.execute(permissionId);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            verify(repository, times(1)).restoreById(permissionId);
        }
    }
}