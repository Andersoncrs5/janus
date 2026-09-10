package org.janus.modules.authorization.service.permission;

import org.janus.modules.authorization.application.service.permission.FindPermissionByIdUseCase;
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
class FindPermissionByIdUseCaseTest {

    @Mock
    private PermissionRepository repository;

    @InjectMocks
    private FindPermissionByIdUseCase useCase;

    @Test
    @DisplayName("Should return bad request when permission id is null")
    void shouldReturnBadRequestWhenIdIsNull() {
        Result<PermissionEntity> result = useCase.execute(null);

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

        Result<PermissionEntity> result = useCase.execute(id);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getMessage().isPresent()).isTrue();
        assertThat(result.getMessage().get()).isEqualTo("Permission not found");
        verify(repository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Should return success with permission when ID exists")
    void shouldReturnSuccessWhenPermissionExists() {
        UUID id = UUID.randomUUID();
        PermissionEntity permission = PermissionEntity.builder()
                .id(id)
                .name("READ_USER")
                .slug("user:read")
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(permission));

        Result<PermissionEntity> result = useCase.execute(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getId()).isEqualTo(id);
        assertThat(result.getData().getName()).isEqualTo("READ_USER");
        verify(repository, times(1)).findById(id);
    }
}