package org.janus.modules.authorization.application.mapper;

import org.janus.modules.authorization.application.dto.userRole.request.CreateUserRoleDTO;
import org.janus.modules.authorization.application.dto.userRole.response.UserRoleDTO;
import org.janus.modules.authorization.domain.entity.UserRoleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "jakarta",
        config = org.janus.configs.mapperStruct.CentralMapperConfig.class
)
public interface UserRoleMapper {


    UserRoleDTO toDTO(UserRoleEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", constant = "0L")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    UserRoleEntity toEntity(CreateUserRoleDTO dto);


}
