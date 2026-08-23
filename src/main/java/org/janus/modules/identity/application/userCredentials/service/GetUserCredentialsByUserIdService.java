package org.janus.modules.identity.application.userCredentials.service;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.identity.domain.entity.UserCredentialsEntity;
import org.janus.modules.identity.ports.in.userCredentials.IGetUserCredentialsByUserIdService;
import org.janus.modules.identity.ports.out.UserCredentialRepository;
import org.janus.shared.domain.result.Result;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class GetUserCredentialsByUserIdService implements IGetUserCredentialsByUserIdService {

    private final UserCredentialRepository repository;

    @Override
    public Result<UserCredentialsEntity> execute(UUID userId) {
        Optional<UserCredentialsEntity> credentials = repository.findByUserId(userId);

        return credentials.map(Result::success)
                .orElseGet(() -> Result.notFound("User credentials not found"));

    }
}