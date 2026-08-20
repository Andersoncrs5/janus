package org.janus.modules.identity.application.user.service.user;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.identity.ports.out.UserRepository;
import org.janus.modules.identity.ports.in.user.IDeleteUserByIdUseCase;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class DeleteUserByIdUseCase implements IDeleteUserByIdUseCase {

    private final UserRepository repository;

    @Override
    @ResultTransaction
    public Result<Void> execute(UUID id) {
        int deleted = this.repository.deleteById(id);

        if (deleted <= 0) return Result.notFound("User not found");

        return Result.success();
    }

}
