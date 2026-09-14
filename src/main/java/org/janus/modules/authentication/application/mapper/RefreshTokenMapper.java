package org.janus.modules.authentication.application.mapper;

import org.janus.modules.authentication.application.dto.refreshToken.request.CreateRefreshTokenDTO;
import org.janus.modules.authentication.domain.entity.RefreshTokenEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RefreshTokenMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tokenHash", ignore = true)
    @Mapping(target = "isUsed", expression = "java(false)")
    @Mapping(target = "replacedByTokenId", ignore = true)
    @Mapping(target = "isRevoked", expression = "java(false)")
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    RefreshTokenEntity toEntity(CreateRefreshTokenDTO dto);
}