package org.janus.shared.infrastructure.properties;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import org.janus.shared.domain.enums.permission.PermissionModule;
import org.janus.shared.domain.enums.permission.PermissionResource;
import org.janus.shared.domain.enums.permission.PermissionRiskLevel;

import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "janus.bootstrap")
public interface BootstrapProperties {

    MasterConfig master();

    List<RoleConfig> roles();

    List<PermissionConfig> permissions();

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

    interface PermissionConfig {
        String name();

        String slug();

        String description();

        PermissionModule module();

        PermissionResource resource();

        String action();

        @WithDefault("LOW")
        @WithName("risk-level")
        PermissionRiskLevel riskLevel();

        @WithDefault("true")
        @WithName("is-active")
        boolean isActive();

        @WithDefault("true")
        @WithName("is-system")
        boolean isSystem();

        Optional<String> metadata();
    }
}