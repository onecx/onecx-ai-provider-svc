package org.tkit.onecx.ai.provider.rs.internal.mappers;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.tkit.onecx.ai.provider.domain.criteria.AgentGroupSearchCriteria;
import org.tkit.onecx.ai.provider.domain.models.AgentGroup;
import org.tkit.onecx.ai.provider.domain.models.enums.AgentGroupOrchestrationMode;
import org.tkit.onecx.ai.provider.domain.models.enums.AgentGroupResponseStrategy;
import org.tkit.quarkus.jpa.daos.PageResult;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.model.AgentGroupDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.AgentGroupPageResultDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.AgentGroupSearchCriteriaDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.CreateAgentGroupRequestDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.UpdateAgentGroupRequestDTO;

@Mapper(uses = { OffsetDateTimeMapper.class })
public interface AgentGroupMapper {

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "modificationUser", ignore = true)
    @Mapping(target = "modificationDate", ignore = true)
    @Mapping(target = "modificationCount", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creationUser", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    AgentGroup create(CreateAgentGroupRequestDTO dto);

    AgentGroupDTO map(AgentGroup agentGroup);

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    AgentGroup map(AgentGroupDTO dto);

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "modificationUser", ignore = true)
    @Mapping(target = "modificationDate", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creationUser", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    void update(@MappingTarget AgentGroup agentGroup, UpdateAgentGroupRequestDTO dto);

    AgentGroupSearchCriteria mapCriteria(AgentGroupSearchCriteriaDTO dto);

    @Mapping(target = "removeStreamItem", ignore = true)
    AgentGroupPageResultDTO mapPageResult(PageResult<AgentGroup> result);

    @AfterMapping
    default void applyDefaults(@MappingTarget AgentGroup agentGroup) {
        if (agentGroup.getOrchestrationMode() == null) {
            agentGroup.setOrchestrationMode(AgentGroupOrchestrationMode.SUPERVISOR_ROUTED);
        }
        if (agentGroup.getResponseStrategy() == null) {
            agentGroup.setResponseStrategy(AgentGroupResponseStrategy.LAST);
        }
    }
}
