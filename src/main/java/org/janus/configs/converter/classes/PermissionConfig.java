package org.janus.configs.converter.classes;

import io.smallrye.config.WithConverter;
import org.janus.configs.converter.PermissionResourceConverter;
import org.janus.shared.domain.enums.permission.PermissionResource;

public interface PermissionConfig {

    @WithConverter(PermissionResourceConverter.class)
    PermissionResource resource();

}
