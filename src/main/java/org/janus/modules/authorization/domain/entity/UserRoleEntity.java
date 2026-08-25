package org.janus.modules.authorization.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.janus.shared.domain.base.model.BaseEntity;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class UserRoleEntity extends BaseEntity {
    public static final String TABLE_NAME = "user_roles";

    private UUID userId;

    private UUID roleId;

    @Nullable private OffsetDateTime expiresAt;

    @Nullable private UUID assignedById;
}