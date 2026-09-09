package org.janus.modules.authentication.application.mapper;

import org.janus.modules.authentication.application.dto.session.request.CreateSessionDTO;
import org.janus.modules.authentication.application.dto.session.request.UpdateSessionDTO;
import org.janus.modules.authentication.application.dto.session.response.SessionDTO;
import org.janus.modules.authentication.domain.entity.SessionEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
        componentModel = "jakarta",
        config = org.janus.configs.mapperStruct.CentralMapperConfig.class,
        builder = @org.mapstruct.Builder(disableBuilder = true)
)
public interface SessionMapper {

    SessionDTO toDTO(SessionEntity entity);

    List<SessionDTO> toDTO(List<SessionEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", constant = "0L")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    SessionEntity toEntity(CreateSessionDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromDto(UpdateSessionDTO dto, @MappingTarget SessionEntity entity);
}