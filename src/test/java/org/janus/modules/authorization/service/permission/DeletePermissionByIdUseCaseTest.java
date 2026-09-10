package org.janus.modules.authorization.service.permission;

import org.janus.modules.authorization.application.service.permission.DeletePermissionByIdUseCase;
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
class DeletePermissionByIdUseCaseTest {

    @Mock
    private PermissionRepository repository;

    @InjectMocks
    private DeletePermissionByIdUseCase useCase;

    @Test
    @DisplayName("Should return bad request when permission id is null")
    void shouldReturnBadRequestWhenIdIsNull() {
        Result<Void> result = useCase.execute(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getMessage().isPresent()).isTrue();
        assertThat(result.getMessage().get()).isEqualTo("Permission id should be defined");
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should return not found when permission does not exist")
    void shouldReturnNotFoundWhenPermissionDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        Result<Void> result = useCase.execute(id);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getMessage().isPresent()).isTrue();
        assertThat(result.getMessage().get()).isEqualTo("Permission not found");
        verify(repository, times(1)).findById(id);
        verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should return bad request when permission is system permission")
    void shouldReturnBadRequestWhenPermissionIsSystem() {
        UUID id = UUID.randomUUID();
        PermissionEntity permission = PermissionEntity.builder()
                .id(id)
                .isSystem(true)
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(permission));

        Result<Void> result = useCase.execute(id);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getMessage().isPresent()).isTrue();
        assertThat(result.getMessage().get()).isEqualTo("System permissions cannot be deleted");
        verify(repository, times(1)).findById(id);
        verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should return not found when delete operation returns zero rows affected")
    void shouldReturnNotFoundWhenDeleteCountIsZero() {
        UUID id = UUID.randomUUID();
        PermissionEntity permission = PermissionEntity.builder()
                .id(id)
                .isSystem(false)
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(permission));
        when(repository.deleteById(id)).thenReturn(0);

        Result<Void> result = useCase.execute(id);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getMessage().isPresent()).isTrue();
        assertThat(result.getMessage().get()).isEqualTo("Permission not found during deletion");
        verify(repository, times(1)).findById(id);
        verify(repository, times(1)).deleteById(id);
    }

    @Test
    @DisplayName("Should delete permission successfully when ID exists and is not system permission")
    void shouldDeletePermissionSuccessfully() {
        UUID id = UUID.randomUUID();
        PermissionEntity permission = PermissionEntity.builder()
                .id(id)
                .isSystem(false)
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(permission));
        when(repository.deleteById(id)).thenReturn(1);

        Result<Void> result = useCase.execute(id);

        assertThat(result.isSuccess()).isTrue();
        verify(repository, times(1)).findById(id);
        verify(repository, times(1)).deleteById(id);
    }
}