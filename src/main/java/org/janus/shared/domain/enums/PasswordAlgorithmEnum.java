package org.janus.shared.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PasswordAlgorithmEnum {

    ARGON2ID("argon2id"),
    BCRYPT("bcrypt"),
    PBKDF2("pbkdf2");

    private final String value;

    public static PasswordAlgorithmEnum fromValue(String value) {
        if (value == null || value.isBlank()) {
            return ARGON2ID;
        }

        for (PasswordAlgorithmEnum algo : values()) {
            if (algo.getValue().equalsIgnoreCase(value)) {
                return algo;
            }
        }
        throw new IllegalArgumentException("Algoritmo de senha não suportado: " + value);
    }
}