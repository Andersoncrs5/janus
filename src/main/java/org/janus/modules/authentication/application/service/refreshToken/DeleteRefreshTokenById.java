package org.janus.modules.authentication.application.service.refreshToken;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.janus.modules.authentication.port.in.refreshToken.IDeleteRefreshTokenById;
import org.janus.modules.authentication.port.out.RefreshTokenRepository;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.UUID;

@ApplicationScoped
public class DeleteRefreshTokenById implements IDeleteRefreshTokenById {

    private final RefreshTokenRepository repository;

    @Inject
    public DeleteRefreshTokenById(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    @ResultTransaction
    public Result<Void> execute(UUID id) {
        if (id == null) {
            return Result.badRequest("Refresh token ID cannot be null");
        }

        try {
            int deleted = repository.deleteById(id);
            if (deleted == 0)
                return Result.notFound("Refresh token not found with id: '" + id + "'");

            if (deleted == 2)
                return Result.badRequest("More one Refresh token deleted");

            return Result.noContent();
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }
}