package org.janus.shared.domain.security;

public interface PasswordEncoderPort {

    String encode(CharSequence rawPassword);
    boolean matches(CharSequence rawPassword, String encodedPassword);
}