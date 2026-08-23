package org.janus.modules.authorization.application.dto.role.filter;

import lombok.Getter;
import lombok.Setter;
import org.janus.shared.domain.base.filter.FilterBaseDTO;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RoleFilterDTO extends FilterBaseDTO {
    private String name;
    private String description;
    private String slug;

    private Boolean isSystem;
    private Boolean isActive;

    private List<RoleOrder> orders = List.of(RoleOrder.CREATED_AT);
}
