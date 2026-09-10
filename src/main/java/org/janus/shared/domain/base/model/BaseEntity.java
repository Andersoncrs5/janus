package org.janus.shared.domain.base.model;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEntity {

    protected UUID id;

    @Setter(AccessLevel.PROTECTED)
    protected Long version;

    @Setter(AccessLevel.PROTECTED)
    protected OffsetDateTime createdAt;

    @Setter(AccessLevel.PROTECTED)
    protected OffsetDateTime updatedAt;

    @Nullable
    @Setter(AccessLevel.PROTECTED)
    protected OffsetDateTime deletedAt;

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public void markAsDeleted() {
        this.deletedAt = OffsetDateTime.now();
    }

    public void restore() {
        this.deletedAt = null;
        this.updatedAt = OffsetDateTime.now();
    }

    public void touch() {
        this.updatedAt = OffsetDateTime.now();
    }

    public void incrementVersion() {
        this.version = (this.version == null) ? 1L : this.version + 1L;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}