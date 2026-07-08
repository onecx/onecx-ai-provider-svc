package org.tkit.onecx.ai.provider.rs.internal.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.tkit.onecx.ai.provider.domain.criteria.ModelSearchCriteria;
import org.tkit.onecx.ai.provider.domain.models.Model;
import org.tkit.quarkus.jpa.daos.PageResult;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.model.CreateModelRequestDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ModelDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ModelPageResultDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ModelSearchCriteriaDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.UpdateModelRequestDTO;

@Mapper(uses = { OffsetDateTimeMapper.class, ProviderMapper.class })
public interface ModelMapper {

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "modificationUser", ignore = true)
    @Mapping(target = "modificationDate", ignore = true)
    @Mapping(target = "modificationCount", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creationUser", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    Model create(CreateModelRequestDTO dto);

    ModelDTO map(Model model);

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    Model map(ModelDTO dto);

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "modificationUser", ignore = true)
    @Mapping(target = "modificationDate", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creationUser", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    void update(@MappingTarget Model model, UpdateModelRequestDTO dto);

    ModelSearchCriteria mapCriteria(ModelSearchCriteriaDTO dto);

    @Mapping(target = "removeStreamItem", ignore = true)
    ModelPageResultDTO mapPageResult(PageResult<Model> result);
}
