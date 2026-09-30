package org.janus.shared.infrastructure.security.jwt.producer;

import io.quarkus.security.UnauthorizedException;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.janus.shared.infrastructure.security.jwt.annotations.UserId;

import java.util.UUID;

@RequestScoped
public class UserContextProducer {

    @Inject
    JsonWebToken jwt;

    @Produces
    @Dependent
    @UserId
    public UUID produceUserId() {
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new UnauthorizedException("Subject claim (sub) missing in JWT");
        }

        try {
            return UUID.fromString(sub);
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid UUID format in JWT subject");
        }
    }
}