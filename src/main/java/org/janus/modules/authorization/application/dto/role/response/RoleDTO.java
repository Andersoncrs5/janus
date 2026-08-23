package org.janus.modules.authorization.application.dto.role.response;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.janus.shared.domain.base.dto.BaseDTO;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RoleDTO extends BaseDTO {
    private String name;
    private String description;
    private String slug;

    private Boolean isSystem;
    private Boolean isActive;
}
