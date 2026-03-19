package org.tkit.onecx.ai.provider.rs.external.v1.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tkit.onecx.ai.provider.common.models.ChatRequestModel;
import org.tkit.onecx.ai.provider.common.models.ChatResponseModel;
import org.tkit.onecx.ai.provider.common.models.RequestContext;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatMessageDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.RequestContextDTOV1;

@Mapper(uses = { OffsetDateTimeMapper.class })
public interface DispatchMapper {

    ChatRequestModel map(ChatRequestDTOV1 dto);

    @Mapping(target = "filterKey", source = "filter.key")
    @Mapping(target = "filterValue", source = "filter.value")
    RequestContext map(RequestContextDTOV1 dto);

    @Mapping(target = "creationDate", expression = "java(java.time.Instant.now().toEpochMilli())")
    ChatMessageDTOV1 create(ChatResponseModel response);
}
