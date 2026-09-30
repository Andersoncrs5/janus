package org.janus.modules.authentication.application.mapper;

import org.janus.modules.authentication.application.dto.loginAttempts.request.CreateLoginAttemptDTO;
import org.janus.modules.authentication.application.dto.loginAttempts.response.LoginAttemptDTO;
import org.janus.modules.authentication.domain.entity.LoginAttemptEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
        componentModel = "jakarta",
        config = org.janus.configs.mapperStruct.CentralMapperConfig.class,
        builder = @org.mapstruct.Builder(disableBuilder = true)
)
public interface LoginAttemptsMapper {

    LoginAttemptDTO toDTO(LoginAttemptEntity entity);

    List<LoginAttemptDTO> toDTO(List<LoginAttemptEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    LoginAttemptEntity toEntity(CreateLoginAttemptDTO dto);
}
