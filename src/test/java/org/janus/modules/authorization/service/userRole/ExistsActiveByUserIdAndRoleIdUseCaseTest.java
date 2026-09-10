package org.janus.modules.authorization.service.userRole;

import org.janus.modules.authorization.application.service.userRole.ExistsActiveByUserIdAndRoleIdUseCase;
import org.janus.modules.authorization.infrastructure.out.UserRoleRepository;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExistsActiveByUserIdAndRoleIdUseCaseTest {

    @Mock
    private UserRoleRepository repository;

    @InjectMocks
    private ExistsActiveByUserIdAndRoleIdUseCase useCase;

    private UUID userId;
    private UUID roleId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        roleId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should return true when active user role association exists")
    void shouldReturnTrueWhenActiveUserRoleExists() {
        when(repository.existsActiveByUserIdAndRoleId(userId, roleId)).thenReturn(true);

        Result<Boolean> result = useCase.execute(userId, roleId);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData());

        verify(repository, times(1)).existsActiveByUserIdAndRoleId(userId, roleId);
    }

    @Test
    @DisplayName("Should return false when active user role association does not exist")
    void shouldReturnFalseWhenActiveUserRoleDoesNotExist() {
        when(repository.existsActiveByUserIdAndRoleId(userId, roleId)).thenReturn(false);

        Result<Boolean> result = useCase.execute(userId, roleId);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertFalse(result.getData());

        verify(repository, times(1)).existsActiveByUserIdAndRoleId(userId, roleId);
    }
}