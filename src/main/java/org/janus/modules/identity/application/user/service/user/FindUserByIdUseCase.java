package org.janus.modules.identity.application.user.service.user;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.ports.out.UserRepository;
import org.janus.modules.identity.ports.in.user.IFindUserByIdUseCase;
import org.janus.shared.domain.result.Result;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class FindUserByIdUseCase implements IFindUserByIdUseCase {

    private final UserRepository repository;

    @Override
    public Result<UserEntity> execute(UUID id) {
        Optional<UserEntity> user = this.repository.findById(id);

        return user.map(Result::success).orElseGet(() -> Result.notFound("User not found"));
    }

}
