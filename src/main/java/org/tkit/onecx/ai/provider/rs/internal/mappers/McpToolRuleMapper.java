package org.tkit.onecx.ai.provider.rs.internal.mappers;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.tkit.onecx.ai.provider.domain.models.McpToolRule;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.model.CreateMcpToolRuleRequestDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.McpToolRuleDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.UpdateMcpToolRuleRequestDTO;

@Mapper(uses = { OffsetDateTimeMapper.class })
public interface McpToolRuleMapper {

    @Mapping(target = "id", source = "id")
    McpToolRuleDTO map(McpToolRule rule);

    List<McpToolRuleDTO> map(List<McpToolRule> rules);

    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "modificationUser", ignore = true)
    @Mapping(target = "modificationDate", ignore = true)
    @Mapping(target = "modificationCount", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creationUser", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "tool", ignore = true)
    @Mapping(target = "globalTool", ignore = true)
    McpToolRule create(CreateMcpToolRuleRequestDTO dto);

    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "modificationUser", ignore = true)
    @Mapping(target = "modificationDate", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creationUser", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "tool", ignore = true)
    @Mapping(target = "globalTool", ignore = true)
    @Mapping(target = "toolName", ignore = true)
    @Mapping(target = "toolDescription", ignore = true)
    @Mapping(target = "autoDangerLevel", ignore = true)
    void update(@MappingTarget McpToolRule rule, UpdateMcpToolRuleRequestDTO dto);
}
