package org.janus.modules.authorization.service.role;

import org.janus.modules.authorization.application.service.role.ExistsRoleByNameUseCase;
import org.janus.modules.authorization.infrastructure.out.RoleRepository;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExistsRoleByNameUseCaseTest {

    @Mock
    private RoleRepository repository;

    @InjectMocks
    private ExistsRoleByNameUseCase useCase;

    @Test
    @DisplayName("Deve retornar Result.success com valor true quando a role existir")
    void shouldReturnTrueWhenRoleExists() {
        String roleName = "ROLE_ADMIN";
        when(repository.existsByName(roleName)).thenReturn(true);

        Result<Boolean> result = useCase.execute(roleName);

        assertNotNull(result);
        assertEquals(true, result.getData());
        verify(repository, times(1)).existsByName(roleName);
    }

    @Test
    @DisplayName("Deve retornar Result.success com valor false quando a role não existir")
    void shouldReturnFalseWhenRoleDoesNotExist() {
        String roleName = "ROLE_USER";
        when(repository.existsByName(roleName)).thenReturn(false);

        Result<Boolean> result = useCase.execute(roleName);

        assertNotNull(result);
        assertEquals(false, result.getData());
        verify(repository, times(1)).existsByName(roleName);
    }
}