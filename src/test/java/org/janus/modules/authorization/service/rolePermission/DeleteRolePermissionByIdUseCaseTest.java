package org.janus.modules.authorization.service.rolePermission;

import org.janus.modules.authorization.application.service.rolePermission.DeleteRolePermissionByIdUseCase;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.modules.authorization.infrastructure.out.RolePermissionRepository;
import org.janus.shared.domain.result.Result;
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
public class DeleteRolePermissionByIdUseCaseTest {

    @Mock
    private RolePermissionRepository repository;

    @InjectMocks
    private DeleteRolePermissionByIdUseCase useCase;

    @Nested
    class Execute {

        @Test
        void shouldReturnBadRequestWhenIdIsNull() {
            Result<RolePermissionEntity> result = useCase.execute(null);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage().isPresent()).isTrue();
            assertThat(result.getMessage()).contains("Role Permission Id should be defined");
            verifyNoInteractions(repository);
        }

        @Test
        void shouldReturnNotFoundWhenNoRecordDeleted() {
            UUID id = UUID.randomUUID();
            when(repository.deleteById(id)).thenReturn(0);

            Result<RolePermissionEntity> result = useCase.execute(id);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage().isPresent()).isTrue();
            assertThat(result.getMessage()).contains("Role Permission not found");
            verify(repository, times(1)).deleteById(id);
        }

        @Test
        void shouldReturnFailureWhenMoreThanOneRecordDeleted() {
            UUID id = UUID.randomUUID();
            when(repository.deleteById(id)).thenReturn(2);

            Result<RolePermissionEntity> result = useCase.execute(id);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage().isPresent()).isTrue();
            assertThat(result.getMessage()).contains("More than one role permission was deleted for ID: " + id);
            verify(repository, times(1)).deleteById(id);
        }

        @Test
        void shouldReturnSuccessWhenOneRecordDeleted() {
            UUID id = UUID.randomUUID();
            when(repository.deleteById(id)).thenReturn(1);

            Result<RolePermissionEntity> result = useCase.execute(id);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            verify(repository, times(1)).deleteById(id);
        }
    }
}