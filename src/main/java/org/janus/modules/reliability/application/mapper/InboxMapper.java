package org.janus.modules.reliability.application.mapper;

import org.janus.modules.reliability.application.dto.inbox.request.CreateInboxDTO;
import org.janus.modules.reliability.application.dto.inbox.response.InboxDTO;
import org.janus.modules.reliability.application.dto.inbox.request.UpdateInboxDTO;
import org.janus.modules.reliability.domain.entity.InboxEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
        componentModel = "jakarta",
        config = org.janus.configs.mapperStruct.CentralMapperConfig.class
)
public interface InboxMapper {

    InboxDTO toDTO(InboxEntity entity);

    List<InboxDTO> toDTO(List<InboxEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", constant = "0L")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    InboxEntity toEntity(CreateInboxDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "messageKey", ignore = true)
    @Mapping(target = "consumerGroup", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromDto(UpdateInboxDTO dto, @MappingTarget InboxEntity entity);
}