package org.janus.shared.domain.base.service;

import org.janus.shared.domain.database.DatabaseConstraintHandler;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class BaseUseCase<T> {

    protected Map<String, Supplier<Result<T>>> getConstraintHandlers() {
        return Collections.emptyMap();
    }

    protected Result<T> executeSafely(Supplier<Result<T>> action) {
        try {
            return action.get();
        } catch (DataIntegrityViolationException e) {
            return handleConstraintException(e);
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }

    protected Optional<Result<T>> validate(boolean condition, Supplier<Result<T>> failureResult) {
        if (!condition) {
            return Optional.of(failureResult.get());
        }
        return Optional.empty();
    }

    protected Result<T> resultFromOptional(Optional<T> optional, String notFoundMessage) {
        return optional.map(Result::success)
                .orElseGet(() -> Result.notFound(notFoundMessage));
    }

    protected Result<T> handleConstraintException(DataIntegrityViolationException e) {
        String message = e.getMessage();

        if (message != null) {
            String lowerMessage = message.toLowerCase();
            for (Map.Entry<String, Supplier<Result<T>>> entry : getConstraintHandlers().entrySet()) {
                if (lowerMessage.contains(entry.getKey())) {
                    return entry.getValue().get();
                }
            }
        }

        return DatabaseConstraintHandler.handle(e);
    }
}