package org.janus.shared.infrastructure.properties;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.util.List;

@ConfigMapping(prefix = "janus.bootstrap")
public interface BootstrapProperties {

    MasterConfig master();

    List<RoleConfig> roles();

    interface MasterConfig {
        String username();

        String email();

        String fullName();

        String password();

        List<String> roles();
    }

    interface RoleConfig {
        String name();

        String slug();

        String description();

        @WithDefault("true")
        boolean isSystem();
    }
}