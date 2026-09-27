package org.janus.modules.authentication.application.service.refreshToken;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.janus.modules.authentication.domain.entity.RefreshTokenEntity;
import org.janus.modules.authentication.port.in.refreshToken.IFindRefreshTokenByHashUseCase;
import org.janus.modules.authentication.port.out.RefreshTokenRepository;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;

import java.util.Optional;

@ApplicationScoped
public class FindRefreshTokenByHashUseCase implements IFindRefreshTokenByHashUseCase {

    private final RefreshTokenRepository repository;

    @Inject
    public FindRefreshTokenByHashUseCase(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    public Result<RefreshTokenEntity> execute(String tokenHash) {
        if (tokenHash == null || tokenHash.isBlank()) {
            return Result.badRequest("Token hash cannot be null or empty");
        }

        try {
            Optional<RefreshTokenEntity> tokenOptional = repository.findByTokenHash(tokenHash);
            return tokenOptional.map(Result::ok).orElseGet(() -> Result.notFound("Refresh token not found for hash: '" + tokenHash + "'"));

        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }
}