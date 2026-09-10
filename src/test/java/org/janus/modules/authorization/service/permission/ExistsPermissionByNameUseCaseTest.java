package org.janus.modules.authorization.service.permission;

import org.janus.modules.authorization.application.service.permission.ExistsPermissionByNameUseCase;
import org.janus.modules.authorization.infrastructure.out.PermissionRepository;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExistsPermissionByNameUseCaseTest {

    @Mock
    private PermissionRepository repository;

    @InjectMocks
    private ExistsPermissionByNameUseCase useCase;

    @Test
    @DisplayName("Should return bad request when permission name is null")
    void shouldReturnBadRequestWhenNameIsNull() {
        Result<Boolean> result = useCase.execute(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getMessage().isPresent()).isTrue();
        assertThat(result.getMessage().get()).isEqualTo("Permission name should be defined");
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("Should return bad request when permission name is blank")
    void shouldReturnBadRequestWhenNameIsBlank(String name) {
        Result<Boolean> result = useCase.execute(name);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getMessage().isPresent()).isTrue();
        assertThat(result.getMessage().get()).isEqualTo("Permission name should be defined");
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should return success with true when permission name exists")
    void shouldReturnTrueWhenPermissionNameExists() {
        String name = "READ_USER";
        when(repository.existsByName(name)).thenReturn(true);

        Result<Boolean> result = useCase.execute(name);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isTrue();
        verify(repository, times(1)).existsByName(name);
    }

    @Test
    @DisplayName("Should return success with false when permission name does not exist")
    void shouldReturnFalseWhenPermissionNameDoesNotExist() {
        String name = "NON_EXISTENT_PERMISSION";
        when(repository.existsByName(name)).thenReturn(false);

        Result<Boolean> result = useCase.execute(name);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isFalse();
        verify(repository, times(1)).existsByName(name);
    }
}