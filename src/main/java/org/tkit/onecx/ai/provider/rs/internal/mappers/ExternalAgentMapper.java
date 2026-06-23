package org.tkit.onecx.ai.provider.rs.internal.mappers;

import java.util.Set;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.tkit.onecx.ai.provider.domain.criteria.ExternalAgentSearchCriteria;
import org.tkit.onecx.ai.provider.domain.models.AgentGroup;
import org.tkit.onecx.ai.provider.domain.models.ExternalAgent;
import org.tkit.quarkus.jpa.daos.PageResult;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.model.CreateExternalAgentRequestDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ExternalAgentDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ExternalAgentPageResultDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ExternalAgentSearchCriteriaDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.UpdateExternalAgentRequestDTO;

@Mapper(uses = { OffsetDateTimeMapper.class, AgentGroupMapper.class })
public interface ExternalAgentMapper {
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "modificationUser", ignore = true)
    @Mapping(target = "modificationDate", ignore = true)
    @Mapping(target = "modificationCount", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creationUser", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    @Mapping(target = "groups", ignore = true)
    ExternalAgent create(CreateExternalAgentRequestDTO dto);

    @Mapping(target = "removeGroupsItem", ignore = true)
    ExternalAgentDTO map(ExternalAgent item);

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "modificationUser", ignore = true)
    @Mapping(target = "modificationDate", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creationUser", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    @Mapping(target = "groups", source = "groups")
    void update(@MappingTarget ExternalAgent externalAgent, UpdateExternalAgentRequestDTO dto,
            Set<AgentGroup> groups);

    ExternalAgentSearchCriteria mapCriteria(ExternalAgentSearchCriteriaDTO dto);

    @Mapping(target = "removeStreamItem", ignore = true)
    ExternalAgentPageResultDTO mapPageResult(PageResult<ExternalAgent> result);
}
