package org.janus.shared.domain.base.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseEntity {

    protected UUID id;
    protected Long version;
    protected OffsetDateTime createdAt;
    protected OffsetDateTime updatedAt;
    @Nullable
    protected OffsetDateTime deletedAt;

    public void markAsDeleted() {
        setDeletedAt(OffsetDateTime.now());
    }

    public void incrementVersion() {
        this.version = (this.version == null ? 0L : this.version) + 1L;
    }

}