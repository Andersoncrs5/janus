package org.janus.modules.authentication.application.service.loginAttempts;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authentication.port.in.loginAttempts.IDeleteLoginAttemptsByIdUseCase;
import org.janus.modules.authentication.port.out.LoginAttemptRepository;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class DeleteLoginAttemptsByIdUseCase implements IDeleteLoginAttemptsByIdUseCase {

    private final LoginAttemptRepository repository;

    @Override
    @ResultTransaction
    public Result<Void> execute(UUID id) {
        int deleted = repository.deleteById(id);

        if (deleted <= 0) {
            return Result.notFound("Login attempt not found");
        }

        if (deleted > 1) {
            return Result.failure("More than one login attempt was deleted for ID: " + id, 500);
        }

        return Result.success();
    }
}