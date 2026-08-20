package org.janus.shared.infrastructure.properties;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "janus.security.argon2")
public interface Argon2Properties {

    @WithName("iterations")
    @WithDefault("2")
    int iterations();

    @WithName("memory-kb")
    @WithDefault("65536")
    int memoryKb();

    @WithName("parallelism")
    @WithDefault("1")
    int parallelism();
}