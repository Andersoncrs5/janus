package org.janus.modules.authentication.application.service.session;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authentication.domain.entity.SessionEntity;
import org.janus.modules.authentication.port.in.session.IFindSessionByIdUseCase;
import org.janus.modules.authentication.port.out.SessionRepository;
import org.janus.shared.domain.result.Result;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class FindSessionByIdUseCase implements IFindSessionByIdUseCase {

    private final SessionRepository repository;

    @Override
    public Result<SessionEntity> execute(UUID id) {
        SessionEntity role = repository.findById(id).orElse(null);

        if (role == null) {
            return Result.notFound("Session not found");
        }

        return Result.success(role);
    }
}
