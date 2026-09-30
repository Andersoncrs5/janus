package org.janus.modules.authorization.domain.entity;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.janus.shared.domain.base.model.BaseEntity;

import java.util.List;
import java.util.Objects;

@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class RoleEntity extends BaseEntity {

    public static final String TABLE_NAME = "roles";

    private String name;
    private String description;
    private String slug;

    @Builder.Default
    private Boolean isActive = true;

    private Boolean isSystem;

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public boolean nameEquals(List<String> list) {
        if (this.name == null || list == null || list.isEmpty()) {
            return false;
        }

        return list.stream()
                .filter(Objects::nonNull)
                .anyMatch(item -> item.equalsIgnoreCase(this.name));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public Boolean getSystem() {
        return isSystem;
    }

    public void setSystem(Boolean system) {
        isSystem = system;
    }
}