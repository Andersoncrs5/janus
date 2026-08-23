package org.janus.modules.authorization.domain.entity;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.janus.shared.domain.base.model.BaseEntity;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class RoleEntity extends BaseEntity {

    public static final String TABLE_NAME = "public.roles";

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
}