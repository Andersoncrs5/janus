package org.janus.modules.bootstrap.service.userRole;

import org.assertj.core.api.Assertions;
import org.janus.modules.authorization.application.dto.userRole.request.CreateUserRoleDTO;
import org.janus.modules.authorization.application.service.role.FindRoleByNameUseCase;
import org.janus.modules.authorization.application.service.userRole.CreateUserRoleUseCase;
import org.janus.modules.authorization.application.service.userRole.ExistsByUserIdAndRoleIdUseCase;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.domain.entity.UserRoleEntity;
import org.janus.modules.authorization.gateway.role.RoleOutboundGateway;
import org.janus.modules.authorization.gateway.userRole.UserRoleOutBoundGateway;
import org.janus.modules.bootstrap.gateway.BootstrapInBoundGateway;
import org.janus.modules.identity.application.user.service.FindUserByEmailUseCase;
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


}