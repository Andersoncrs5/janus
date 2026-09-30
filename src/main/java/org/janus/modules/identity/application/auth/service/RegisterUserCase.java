package org.janus.modules.identity.application.auth.service;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authentication.application.dto.refreshToken.request.CreateRefreshTokenDTO;
import org.janus.modules.authentication.application.dto.session.request.CreateSessionDTO;
import org.janus.modules.authentication.domain.entity.RefreshTokenEntity;
import org.janus.modules.authentication.domain.entity.SessionEntity;
import org.janus.modules.authorization.gateway.userRole.UserRoleOutBoundGateway;
import org.janus.modules.authorization.infrastructure.in.permission.IFindPermissionSlugsByRoleNameUseCase;
import org.janus.modules.identity.application.user.dto.request.CreateUserDTO;
import org.janus.modules.identity.application.user.mapper.UserMapper;
import org.janus.modules.identity.application.userCredentials.dto.CreateUserCredentialsDTO;
import org.janus.modules.identity.domain.entity.UserCredentialsEntity;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.gateway.auth.AuthGatewayInBound;
import org.janus.modules.identity.ports.in.auth.IRegisterUserCase;
import org.janus.modules.identity.ports.in.jwt.ITokenService;
import org.janus.modules.identity.ports.in.user.ICreateUserUseCase;
import org.janus.modules.identity.ports.in.userCredentials.ICreateUserCredentialUseCase;
import org.janus.shared.domain.api.TokenResponse;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;
import org.janus.shared.infrastructure.properties.JwtProperties;

import java.time.OffsetDateTime;
import java.util.List;

@ApplicationScoped
@RequiredArgsConstructor
public class RegisterUserCase implements IRegisterUserCase {

    private final AuthGatewayInBound authGatewayInBound;
    private final UserRoleOutBoundGateway userRoleOutBoundGateway;
    private final ICreateUserUseCase createUser;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;
    private final ITokenService tokenService;
    private final ICreateUserCredentialUseCase createUserCredential;
    private final IFindPermissionSlugsByRoleNameUseCase findPermissionSlugsByRoleName;

    @Override
    @ResultTransaction
    public Result<TokenResponse> execute(CreateUserDTO dto, String ipAddress, String userAgent) {
        try {
            Result<UserEntity> userResult = createUser.execute(dto);
            if (userResult.isFailure()) return Result.failure(userResult.getFirstError(), userResult.getStatus());

            UserEntity user = userResult.getData();

            Result<UserCredentialsEntity> credentialsEntityResult = createUserCredential.execute(
                    CreateUserCredentialsDTO.init(
                            dto.getPassword(),
                            null
                    ),
                    user.getId()
            );
            if (credentialsEntityResult.isFailure())
                return Result.failure(credentialsEntityResult.getFirstError(), credentialsEntityResult.getStatus());

            Result<List<String>> findRoleNameResult = userRoleOutBoundGateway.findRolesOnlyNameByUserId().execute(user.getId());
            if (findRoleNameResult.isFailure())
                return Result.failure(findRoleNameResult.getFirstError(), findRoleNameResult.getStatus());
            var rolesName = findRoleNameResult.getData();

            Result<List<String>> listResult = findPermissionSlugsByRoleName.execute(rolesName);
            if (listResult.isFailure())
                return Result.failure(listResult.getFirstError(), listResult.getStatus());

            Result<String> tokenResult = tokenService.generateToken(user, rolesName, listResult.getData());
            if (tokenResult.isFailure()) return Result.failure(tokenResult.getFirstError(), tokenResult.getStatus());

            Result<SessionEntity> sessionResult = authGatewayInBound.sessionGateway().createSessionUseCase().execute(new CreateSessionDTO(
                    user.getId(),
                    ipAddress,
                    userAgent,
                    jwtProperties.exp().session()
            ));
            if (sessionResult.isFailure())
                return Result.failure(sessionResult.getFirstError(), sessionResult.getStatus());
            SessionEntity session = sessionResult.getData();

            Result<RefreshTokenEntity> refreshTokenResult = authGatewayInBound.refreshTokenGatewayOutBound().CreateRefreshToken().execute(new CreateRefreshTokenDTO(
                    session.getId(),
                    user.getId()
            ));
            if (refreshTokenResult.isFailure())
                return Result.failure(refreshTokenResult.getFirstError(), refreshTokenResult.getStatus());
            RefreshTokenEntity refreshToken = refreshTokenResult.getData();

            TokenResponse tokens = new TokenResponse(
                    tokenResult.getData(),
                    OffsetDateTime.now().plusDays(jwtProperties.exp().token() * 60),
                    refreshToken.getTokenHash(),
                    refreshToken.getExpiresAt(),
                    userMapper.toDTO(user),
                    rolesName
            );

            return Result.created(tokens);
        } catch (RuntimeException e) {
            throw new InternalServerErrorException("Error executing INSERT for table: users", e);
        }
    }

}
