package org.janus.configs.mapperStruct.extensions;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@ApplicationScoped
public class DateTimeMapper {

    public OffsetDateTime fromEpochMilli(Long epochMilli) {
        return epochMilli == null ? null : Instant.ofEpochMilli(epochMilli).atOffset(ZoneOffset.UTC);
    }

    public Long toEpochMilli(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant().toEpochMilli();
    }

    public String toString(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.toString();
    }
}