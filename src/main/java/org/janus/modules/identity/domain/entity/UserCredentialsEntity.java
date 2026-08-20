package org.janus.modules.identity.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.janus.shared.domain.base.model.BaseEntity;
import org.janus.shared.domain.enums.PasswordAlgorithmEnum;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class UserCredentialsEntity extends BaseEntity {

    public static final String TABLE_NAME = "public.user_credentials";

    private UUID userId;
    private String passwordHash;

    @Builder.Default
    private String algorithm = PasswordAlgorithmEnum.ARGON2ID.getValue();

    @Builder.Default
    private Long version = 0L;

    public PasswordAlgorithmEnum getAlgorithmEnum() {
        return PasswordAlgorithmEnum.fromValue(this.algorithm);
    }

    public void setAlgorithmEnum(PasswordAlgorithmEnum algorithm) {
        this.algorithm = algorithm != null ? algorithm.getValue() : PasswordAlgorithmEnum.ARGON2ID.getValue();
    }

    public void incrementVersion() {
        this.version = (this.version == null ? 0L : this.version) + 1L;
    }

    public void markAsDeleted() {
        setDeletedAt(OffsetDateTime.now());
    }
}