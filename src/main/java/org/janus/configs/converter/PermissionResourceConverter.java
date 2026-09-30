package org.janus.configs.converter;

import org.eclipse.microprofile.config.spi.Converter;
import org.janus.shared.domain.enums.permission.PermissionResource;

public class PermissionResourceConverter implements Converter<PermissionResource> {

    @Override
    public PermissionResource convert(String value) throws IllegalArgumentException {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toUpperCase();

        if (normalized.endsWith("S") && !normalized.endsWith("STATUS")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        normalized = normalized.replace("-", "_");

        for (PermissionResource resource : PermissionResource.values()) {
            if (resource.name().equals(normalized) || resource.getValue().equalsIgnoreCase(value)) {
                return resource;
            }
        }

        throw new IllegalArgumentException("Cannot convert '" + value + "' to PermissionResource");
    }
}