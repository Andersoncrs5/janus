package org.janus.modules.authentication.application.service.session;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authentication.port.in.session.IDeleteSessionByIdUseCase;
import org.janus.modules.authentication.port.out.SessionRepository;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class DeleteSessionByIdUseCase implements IDeleteSessionByIdUseCase {

    private final SessionRepository repository;

    @Override
    @ResultTransaction
    public Result<Void> execute(UUID id) {
        int deleted = repository.deleteById(id);

        if (deleted <= 0)
            return Result.notFound("Session not found");

        if (deleted > 1)
            return Result.failure("More than one session was deleted for ID: " + id, 500);

        return Result.success();
    }
}