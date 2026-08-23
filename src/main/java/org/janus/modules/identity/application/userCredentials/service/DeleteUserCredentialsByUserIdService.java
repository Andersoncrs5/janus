package org.janus.modules.identity.application.userCredentials.service;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.identity.domain.entity.UserCredentialsEntity;
import org.janus.modules.identity.ports.in.userCredentials.IDeleteUserCredentialsByUserIdService;
import org.janus.modules.identity.ports.out.UserCredentialRepository;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class DeleteUserCredentialsByUserIdService implements IDeleteUserCredentialsByUserIdService {

    private final UserCredentialRepository repository;

    @Override
    @ResultTransaction
    public Result<UserCredentialsEntity> execute(UUID userId) {
        boolean deleted = repository.deleteByUserId(userId);

        if (!deleted) return Result.notFound("User not found");

        return Result.success();
    }

}
