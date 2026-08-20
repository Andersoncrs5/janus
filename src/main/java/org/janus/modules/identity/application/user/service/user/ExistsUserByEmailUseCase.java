package org.janus.modules.identity.application.user.service.user;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.identity.ports.out.UserRepository;
import org.janus.modules.identity.ports.in.user.IExistsUserByEmailUseCase;
import org.janus.shared.domain.result.Result;

@ApplicationScoped
@RequiredArgsConstructor
public class ExistsUserByEmailUseCase implements IExistsUserByEmailUseCase {

    private final UserRepository repository;

    @Override
    public Result<Boolean> execute(String email) {
        return Result.success(
            repository.existsByEmail(email)
        );
    }
}
