package org.janus.modules.bootstrap.service.userRole;

import org.janus.modules.authorization.application.dto.userRole.request.CreateUserRoleDTO;
import org.janus.modules.authorization.application.service.role.FindRoleByNameUseCase;
import org.janus.modules.authorization.application.service.userRole.CreateUserRoleUseCase;
import org.janus.modules.authorization.application.service.userRole.ExistsByUserIdAndRoleIdUseCase;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.domain.entity.UserRoleEntity;
import org.janus.modules.authorization.gateway.role.RoleOutboundGateway;
import org.janus.modules.authorization.gateway.userRole.UserRoleOutBoundGateway;
import org.janus.modules.bootstrap.gateway.BootstrapInBoundGateway;
import org.janus.modules.identity.application.user.service.user.FindUserByEmailUseCase;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.gateway.user.UserOutboundGateway;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.properties.BootstrapProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkRoleMasterToUserMasterUseCaseTest {

    @Mock
    private BootstrapInBoundGateway gateway;

    @Mock
    private BootstrapProperties properties;

    @Mock
    private BootstrapProperties.MasterConfig masterConfig;

    @Mock
    private UserOutboundGateway userOutboundGateway;

    @Mock
    private RoleOutboundGateway roleOutboundGateway;

    @Mock
    private UserRoleOutBoundGateway userRoleOutboundGateway;

    @Mock
    private FindUserByEmailUseCase findUserByEmailUseCase;

    @Mock
    private FindRoleByNameUseCase findRoleByNameUseCase;

    @Mock
    private ExistsByUserIdAndRoleIdUseCase existsByUserIdAndRoleIdUseCase;

    @Mock
    private CreateUserRoleUseCase createUserRoleUseCase;

    @InjectMocks
    private LinkRoleMasterToUserMasterUseCase useCase;

    private final String masterEmail = "admin@gmail.com";
    private UUID userId;
    private UUID roleId;
    private UserEntity userEntity;
    private RoleEntity roleEntity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        roleId = UUID.randomUUID();

        userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setEmail(masterEmail);

        roleEntity = new RoleEntity();
        roleEntity.setId(roleId);
        roleEntity.setName("MASTER");

        lenient().when(properties.master()).thenReturn(masterConfig);
        lenient().when(masterConfig.email()).thenReturn(masterEmail);

        lenient().when(gateway.userOutboundGateway()).thenReturn(userOutboundGateway);
        lenient().when(gateway.roleOutboundGateway()).thenReturn(roleOutboundGateway);
        lenient().when(gateway.userRoleOutBoundGateway()).thenReturn(userRoleOutboundGateway);

        lenient().when(userOutboundGateway.findUserByEmailUseCase()).thenReturn(findUserByEmailUseCase);
        lenient().when(roleOutboundGateway.findRoleByNameUseCase()).thenReturn(findRoleByNameUseCase);
        lenient().when(userRoleOutboundGateway.existsByUserIdAndRoleIdUseCase()).thenReturn(existsByUserIdAndRoleIdUseCase);
        lenient().when(userRoleOutboundGateway.createUserRoleUseCase()).thenReturn(createUserRoleUseCase);
    }

    @Test
    @DisplayName("Should link MASTER role to master user successfully when not linked yet")
    void shouldLinkRoleMasterToUserMasterSuccessfully() {
        when(findUserByEmailUseCase.execute(masterEmail)).thenReturn(Result.success(userEntity));
        when(findRoleByNameUseCase.execute("MASTER")).thenReturn(Result.success(roleEntity));
        when(existsByUserIdAndRoleIdUseCase.execute(userId, roleId)).thenReturn(Result.success(false));

        UserRoleEntity userRoleEntity = new UserRoleEntity();
        userRoleEntity.setUserId(userId);
        userRoleEntity.setRoleId(roleId);

        when(createUserRoleUseCase.execute(any(CreateUserRoleDTO.class))).thenReturn(Result.success(userRoleEntity));

        useCase.execute();

        ArgumentCaptor<CreateUserRoleDTO> captor = ArgumentCaptor.forClass(CreateUserRoleDTO.class);
        verify(createUserRoleUseCase, times(1)).execute(captor.capture());

        CreateUserRoleDTO dto = captor.getValue();
        assertEquals(userId, dto.getUserId());
        assertEquals(roleId, dto.getRoleId());
    }

    @Test
    @DisplayName("Should skip linking when MASTER role is already assigned")
    void shouldSkipLinkingWhenRoleIsAlreadyAssigned() {
        when(findUserByEmailUseCase.execute(masterEmail)).thenReturn(Result.success(userEntity));
        when(findRoleByNameUseCase.execute("MASTER")).thenReturn(Result.success(roleEntity));
        when(existsByUserIdAndRoleIdUseCase.execute(userId, roleId)).thenReturn(Result.success(true));

        useCase.execute();

        verify(createUserRoleUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("Should abort execution when user search fails")
    void shouldAbortWhenUserSearchFails() {
        when(findUserByEmailUseCase.execute(masterEmail)).thenReturn(Result.notFound("User not found"));

        useCase.execute();

        verify(findRoleByNameUseCase, never()).execute(any());
        verify(createUserRoleUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("Should abort execution when MASTER role search fails")
    void shouldAbortWhenRoleSearchFails() {
        when(findUserByEmailUseCase.execute(masterEmail)).thenReturn(Result.success(userEntity));
        when(findRoleByNameUseCase.execute("MASTER")).thenReturn(Result.notFound("Role not found"));

        useCase.execute();

        verify(existsByUserIdAndRoleIdUseCase, never()).execute(any(), any());
        verify(createUserRoleUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("Should abort execution when user-role existence check fails")
    void shouldAbortWhenExistenceCheckFails() {
        when(findUserByEmailUseCase.execute(masterEmail)).thenReturn(Result.success(userEntity));
        when(findRoleByNameUseCase.execute("MASTER")).thenReturn(Result.success(roleEntity));
        when(existsByUserIdAndRoleIdUseCase.execute(userId, roleId)).thenReturn(Result.failure("Database error",500));

        useCase.execute();

        verify(createUserRoleUseCase, never()).execute(any());
    }
}