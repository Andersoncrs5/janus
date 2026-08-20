package org.janus.modules.identity.ports.in.user;

import jakarta.validation.constraints.Email;
import org.janus.shared.domain.result.Result;

public interface IExistsUserByEmailUseCase {
    Result<Boolean> execute(@Email String email);
}
