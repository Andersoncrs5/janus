package org.janus.modules.authorization.service.rolePermission;

import org.janus.modules.authorization.application.service.rolePermission.FindRolePermissionByIdUseCase;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.modules.authorization.infrastructure.out.RolePermissionRepository;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.Nested;
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
public class FindRolePermissionByIdUseCaseTest {

    @Mock
    private RolePermissionRepository repository;

    @InjectMocks
    private FindRolePermissionByIdUseCase useCase;

    @Nested
    class Execute {

        @Test
        void shouldReturnBadRequestWhenIdIsNull() {
            Result<RolePermissionEntity> result = useCase.execute(null);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage().isPresent()).isTrue();
            assertThat(result.getMessage().get()).contains("Role Permission Id should be defined");
            verifyNoInteractions(repository);
        }

        @Test
        void shouldReturnNotFoundWhenEntityDoesNotExist() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            Result<RolePermissionEntity> result = useCase.execute(id);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage().isPresent()).isTrue();
            assertThat(result.getMessage().get()).contains("Role Permission not found");
            verify(repository, times(1)).findById(id);
        }

        @Test
        void shouldReturnSuccessWhenEntityExists() {
            UUID id = UUID.randomUUID();
            RolePermissionEntity entity = new RolePermissionEntity();
            entity.setId(id);

            when(repository.findById(id)).thenReturn(Optional.of(entity));

            Result<RolePermissionEntity> result = useCase.execute(id);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo(entity);
            verify(repository, times(1)).findById(id);
        }
    }
}