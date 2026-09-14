package org.janus.shared.infrastructure.properties;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.security.jwt")
public interface JwtProperties {

    String group();

    Exp exp();

    interface Exp {
        long token();

        long refresh();

        long session();
    }
}