package org.janus.shared.infrastructure.security;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.janus.shared.domain.security.PasswordEncoderPort;
import org.janus.shared.infrastructure.properties.Argon2Properties;

@ApplicationScoped
public class Argon2PasswordEncoderAdapter implements PasswordEncoderPort {

    private final Argon2 argon2;
    private final Argon2Properties argon2Properties;

    @Inject
    public Argon2PasswordEncoderAdapter(Argon2Properties argon2Properties) {
        this.argon2Properties = argon2Properties;
        this.argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
    }

    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Raw password cannot be null");
        }

        char[] passwordChars = rawPassword.toString().toCharArray();
        try {
            return argon2.hash(
                    argon2Properties.iterations(),
                    argon2Properties.memoryKb(),
                    argon2Properties.parallelism(),
                    passwordChars
            );
        } finally {
            argon2.wipeArray(passwordChars);
        }
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }

        char[] passwordChars = rawPassword.toString().toCharArray();
        try {
            return argon2.verify(encodedPassword, passwordChars);
        } finally {
            argon2.wipeArray(passwordChars);
        }
    }
}