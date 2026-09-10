package org.janus.modules.authorization.service.permission;

import org.janus.modules.authorization.application.service.permission.ExistsPermissionBySlugUseCase;
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
class ExistsPermissionBySlugUseCaseTest {

    @Mock
    private PermissionRepository repository;

    @InjectMocks
    private ExistsPermissionBySlugUseCase useCase;

    @Test
    @DisplayName("Should return bad request when permission slug is null")
    void shouldReturnBadRequestWhenSlugIsNull() {
        Result<Boolean> result = useCase.execute(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getMessage().isPresent()).isTrue();
        assertThat(result.getMessage().get()).isEqualTo("Permission slug should be defined");
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("Should return bad request when permission slug is blank")
    void shouldReturnBadRequestWhenSlugIsBlank(String slug) {
        Result<Boolean> result = useCase.execute(slug);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getMessage().isPresent()).isTrue();
        assertThat(result.getMessage().get()).isEqualTo("Permission slug should be defined");
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Should return success with true when permission slug exists")
    void shouldReturnTrueWhenPermissionSlugExists() {
        String slug = "user:read";
        when(repository.existsBySlug(slug)).thenReturn(true);

        Result<Boolean> result = useCase.execute(slug);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isTrue();
        verify(repository, times(1)).existsBySlug(slug);
    }

    @Test
    @DisplayName("Should return success with false when permission slug does not exist")
    void shouldReturnFalseWhenPermissionSlugDoesNotExist() {
        String slug = "non:existent:slug";
        when(repository.existsBySlug(slug)).thenReturn(false);

        Result<Boolean> result = useCase.execute(slug);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isFalse();
        verify(repository, times(1)).existsBySlug(slug);
    }
}