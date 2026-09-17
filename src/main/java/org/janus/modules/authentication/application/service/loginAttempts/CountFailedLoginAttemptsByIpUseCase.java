package org.janus.modules.authentication.application.service.loginAttempts;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authentication.port.in.loginAttempts.ICountFailedLoginAttemptsByIpUseCase;
import org.janus.modules.authentication.port.out.LoginAttemptRepository;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.time.OffsetDateTime;

@ApplicationScoped
@RequiredArgsConstructor
public class CountFailedLoginAttemptsByIpUseCase implements ICountFailedLoginAttemptsByIpUseCase {

    private final LoginAttemptRepository repository;

    @Override
    @ResultTransaction
    public Result<Long> execute(String ipAddress, OffsetDateTime since) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return Result.badRequest("IP address cannot be null or blank");
        }

        if (since == null) {
            return Result.badRequest("Since timestamp cannot be null");
        }

        long count = repository.countFailedAttemptsByIpAddress(ipAddress.trim(), since);

        return Result.success(count);
    }
}