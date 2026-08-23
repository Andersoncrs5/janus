package org.janus.configs.mapperStruct;

import org.janus.configs.mapperStruct.extensions.*;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.MapperConfig;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@MapperConfig(
        componentModel = "jakarta",
        uses = {
                DateTimeMapper.class,
                BooleanByteMapper.class
        },
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        imports = {
                UUID.class,
                Objects.class,
                OffsetDateTime.class,
                StringUtils.class,
                JsonMapperUtils.class,
                EnumMapperUtils.class,
                DateTimeMapper.class,
                BooleanByteMapper.class
        }
)
public interface CentralMapperConfig {
}