package org.janus.modules.identity.application.user.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.janus.modules.identity.ports.in.user.IDeleteUserByIdUseCase;
import org.janus.modules.identity.ports.out.UserRepository;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.UUID;

@ApplicationScoped
public class DeleteUserByIdUseCase implements IDeleteUserByIdUseCase {

    @Inject
    private UserRepository repository;

    @Override
    @ResultTransaction
    public Result<Void> execute(UUID id) {
        int deleted = this.repository.deleteById(id);

        if (deleted <= 0) return Result.notFound("User not found");

        return Result.success();
    }

}
