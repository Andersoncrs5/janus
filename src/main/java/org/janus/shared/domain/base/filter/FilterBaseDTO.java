package org.janus.shared.domain.base.filter;

import jakarta.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
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

    @QueryParam("deletedAtMax")
    private OffsetDateTime deletedAtMin;

    @QueryParam("deletedAtMax")
    private OffsetDateTime deletedAtMax;

    @QueryParam("seeDeleted")
    private Boolean seeDeleted;

    @QueryParam("page")
    private int page = 0;

    @QueryParam("size")
    private int size = 20;

    public int getOffset() {
        return page * size;
    }

}

