package org.janus.modules.identity.application.user.service.user;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.ports.in.user.IFindUserByEmailUseCase;
import org.janus.modules.identity.ports.out.UserRepository;
import org.janus.shared.domain.result.Result;

import java.util.Optional;

@ApplicationScoped
@RequiredArgsConstructor
public class FindUserByEmailUseCase implements IFindUserByEmailUseCase {

    private final UserRepository repository;

    @Override
    public Result<UserEntity> execute(String email) {
        Optional<UserEntity> user = repository.findByEmail(email);

        return user.map(Result::success).orElseGet(() -> Result.notFound("User not found"));
    }
}
