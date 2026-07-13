package org.tkit.onecx.ai.provider.domain.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tkit.onecx.ai.provider.domain.models.GlobalTool;
import org.tkit.onecx.ai.provider.domain.models.Tool;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

@Mapper(uses = OffsetDateTimeMapper.class)
public interface ToolMapper {

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "source", constant = "GLOBAL")
    Tool fromGlobal(GlobalTool globalTool);

}
