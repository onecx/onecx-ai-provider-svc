package org.tkit.onecx.ai.provider.rs.internal.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.tkit.onecx.ai.provider.domain.criteria.RuntimeConfigSearchCriteria;
import org.tkit.onecx.ai.provider.domain.models.RuntimeConfig;
import org.tkit.quarkus.jpa.daos.PageResult;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.model.CreateRuntimeConfigRequestDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.RuntimeConfigDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.RuntimeConfigPageResultDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.RuntimeConfigSearchCriteriaDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.UpdateRuntimeConfigRequestDTO;

@Mapper(uses = { OffsetDateTimeMapper.class, ProviderMapper.class })
public interface RuntimeConfigMapper {

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "modificationUser", ignore = true)
    @Mapping(target = "modificationDate", ignore = true)
    @Mapping(target = "modificationCount", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creationUser", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    RuntimeConfig create(CreateRuntimeConfigRequestDTO dto);

    RuntimeConfigDTO map(RuntimeConfig runtimeConfig);

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    RuntimeConfig map(RuntimeConfigDTO dto);

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "modificationUser", ignore = true)
    @Mapping(target = "modificationDate", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creationUser", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    void update(@MappingTarget RuntimeConfig runtimeConfig, UpdateRuntimeConfigRequestDTO dto);

    RuntimeConfigSearchCriteria mapCriteria(RuntimeConfigSearchCriteriaDTO dto);

    @Mapping(target = "removeStreamItem", ignore = true)
    RuntimeConfigPageResultDTO mapPageResult(PageResult<RuntimeConfig> result);
}
