package org.janus.modules.authorization.application.dto.userRole.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.janus.shared.domain.base.dto.BaseDTO;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserRoleDTO extends BaseDTO {

    private UUID userId;

    private UUID roleId;

    @Nullable
    private OffsetDateTime expiresAt;

    @Nullable private UUID assignedById;
}
