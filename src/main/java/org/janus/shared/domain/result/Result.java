package org.janus.shared.domain.result;

import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
public class Result<T> {

    private final T value;
    private final List<String> errors;
    private final int statusCode;

    private Result(T value, List<String> errors, int statusCode) {
        this.value = value;
        this.errors = errors == null ? Collections.emptyList() : List.copyOf(errors);
        this.statusCode = statusCode;
    }

    public T getData() {
        return value;
    }

    public boolean isSuccess() {
        return errors.isEmpty();
    }

    public boolean isFailure() {
        return !isSuccess();
    }

    public String getFirstError() {
        Optional<String> s = errors.stream().findFirst();
        return s.orElse("");
    }

    public Optional<String> getMessage() {
        return errors.stream().findFirst();
    }

    public static <T> Result<T> success() {
        return new Result<>(null, List.of(), 200);
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(value, List.of(), 200);
    }

    public static <T> Result<T> success(T value, int status) {
        return new Result<>(value, List.of(), status);
    }

    public static <T> Result<T> success(int status) {
        return new Result<>(null, List.of(), status);
    }

    public static <T> Result<T> failure(Result<T> result) {
        return new Result<>(result.value, result.errors, result.statusCode);
    }

    public static <T> Result<T> failure(String error, int status) {
        return new Result<>(null, List.of(error), status);
    }

    public static <T> Result<T> failure(List<String> errors, int status) {
        return new Result<>(null, errors, status);
    }
}