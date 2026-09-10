package org.janus.shared.domain.base.filter;

import jakarta.ws.rs.QueryParam;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FilterBaseDTO {
    @QueryParam("id")
    private UUID id;

    @QueryParam("versionMin")
    private Long versionMin;

    @QueryParam("versionMax")
    private Long versionMax;

    @QueryParam("createdAtMin")
    private OffsetDateTime createdAtMin;

    @QueryParam("createdAtMax")
    private OffsetDateTime createdAtMax;

    @QueryParam("updatedAtMin")
    private OffsetDateTime updatedAtMin;

    @QueryParam("updatedAtMax")
    private OffsetDateTime updatedAtMax;

    @QueryParam("deletedAtMin")
    private OffsetDateTime deletedAtMin;

    @QueryParam("deletedAtMax")
    private OffsetDateTime deletedAtMax;

    @QueryParam("seeDeleted")
    private Boolean seeDeleted;

    @QueryParam("page")
    @Builder.Default
    private int page = 0;

    @QueryParam("size")
    @Builder.Default
    private int size = 20;

    public int getOffset() {
        return page * size;
    }

}