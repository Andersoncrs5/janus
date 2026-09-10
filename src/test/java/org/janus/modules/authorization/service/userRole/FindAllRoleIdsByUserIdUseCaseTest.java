package org.janus.modules.authorization.service.userRole;

import org.janus.modules.authorization.application.service.userRole.FindAllRoleIdsByUserIdUseCase;
import org.janus.modules.authorization.infrastructure.out.UserRoleRepository;
import org.janus.shared.domain.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindAllRoleIdsByUserIdUseCaseTest {

    @Mock
    private UserRoleRepository repository;

    @InjectMocks
    private FindAllRoleIdsByUserIdUseCase useCase;

    private UUID userId;
    private List<UUID> roleIds;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        roleIds = List.of(UUID.randomUUID(), UUID.randomUUID());
    }

    @Test
    @DisplayName("Should return list of role IDs successfully when user has roles")
    void shouldReturnRoleIdsSuccessfullyWhenUserHasRoles() {
        when(repository.findAllRoleIdsByUserId(userId)).thenReturn(roleIds);

        Result<List<UUID>> result = useCase.execute(userId);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        assertEquals(roleIds, result.getData());

        verify(repository, times(1)).findAllRoleIdsByUserId(userId);
    }

    @Test
    @DisplayName("Should return empty list when user has no roles")
    void shouldReturnEmptyListWhenUserHasNoRoles() {
        when(repository.findAllRoleIdsByUserId(userId)).thenReturn(List.of());

        Result<List<UUID>> result = useCase.execute(userId);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty());

        verify(repository, times(1)).findAllRoleIdsByUserId(userId);
    }
}