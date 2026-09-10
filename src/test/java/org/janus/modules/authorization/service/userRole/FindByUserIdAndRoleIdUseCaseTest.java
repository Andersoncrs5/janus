package org.janus.modules.authorization.service.userRole;

import org.janus.modules.authorization.application.service.userRole.FindByUserIdAndRoleIdUseCase;
import org.janus.modules.authorization.domain.entity.UserRoleEntity;
import org.janus.modules.authorization.infrastructure.out.UserRoleRepository;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindByUserIdAndRoleIdUseCaseTest {

    @Mock
    private UserRoleRepository repository;

    @InjectMocks
    private FindByUserIdAndRoleIdUseCase useCase;

    private UUID userId;
    private UUID roleId;
    private UserRoleEntity entity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        roleId = UUID.randomUUID();
        entity = new UserRoleEntity();
        entity.setUserId(userId);
        entity.setRoleId(roleId);
    }

    @Test
    @DisplayName("Should return user role successfully when association exists")
    void shouldReturnUserRoleWhenExists() {
        when(repository.findByUserIdAndRoleId(userId, roleId)).thenReturn(Optional.of(entity));

        Result<UserRoleEntity> result = useCase.execute(userId, roleId);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(userId, result.getData().getUserId());
        assertEquals(roleId, result.getData().getRoleId());

        verify(repository, times(1)).findByUserIdAndRoleId(userId, roleId);
    }

    @Test
    @DisplayName("Should return not found when user role association does not exist")
    void shouldReturnNotFoundWhenDoesNotExist() {
        when(repository.findByUserIdAndRoleId(userId, roleId)).thenReturn(Optional.empty());

        Result<UserRoleEntity> result = useCase.execute(userId, roleId);

        assertFalse(result.isSuccess());
        assertEquals(404, result.getStatusCode());
        assertEquals("User Role not found", result.getMessage().get());

        verify(repository, times(1)).findByUserIdAndRoleId(userId, roleId);
    }
}