package org.janus.modules.authorization.application.mapper;

import org.janus.modules.authorization.application.dto.rolePermission.request.CreateRolePermissionDTO;
import org.janus.modules.authorization.application.dto.rolePermission.request.UpdateRolePermissionDTO;
import org.janus.modules.authorization.application.dto.rolePermission.response.RolePermissionDTO;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "jakarta",
        config = org.janus.configs.mapperStruct.CentralMapperConfig.class
)
public interface RolePermissionMapper {

    RolePermissionDTO toDTO(RolePermissionEntity entity);

    List<RolePermissionDTO> toDTO(List<RolePermissionEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", constant = "0L")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    RolePermissionEntity toEntity(CreateRolePermissionDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromDto(UpdateRolePermissionDTO dto, @MappingTarget RolePermissionEntity entity);
}