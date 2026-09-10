package org.janus.modules.authorization.application.mapper;

import org.janus.modules.authorization.application.dto.permission.request.CreatePermissionDTO;
import org.janus.modules.authorization.application.dto.permission.request.UpdatePermissionDTO;
import org.janus.modules.authorization.application.dto.permission.response.PermissionDTO;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "jakarta",
        config = org.janus.configs.mapperStruct.CentralMapperConfig.class
)
public interface PermissionMapper {

    PermissionDTO toDTO(PermissionEntity entity);

    List<PermissionDTO> toDTO(List<PermissionEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", constant = "0L")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    PermissionEntity toEntity(CreatePermissionDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromDto(UpdatePermissionDTO dto, @MappingTarget PermissionEntity entity);
}