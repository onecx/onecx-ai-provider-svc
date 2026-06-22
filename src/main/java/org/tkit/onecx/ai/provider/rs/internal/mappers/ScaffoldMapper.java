package org.tkit.onecx.ai.provider.rs.internal.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.tkit.onecx.ai.provider.domain.criteria.ScaffoldSearchCriteria;
import org.tkit.onecx.ai.provider.domain.models.Scaffold;
import org.tkit.quarkus.jpa.daos.PageResult;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.model.CreateScaffoldRequestDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ScaffoldDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ScaffoldPageResultDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ScaffoldSearchCriteriaDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.UpdateScaffoldRequestDTO;

@Mapper(uses = { OffsetDateTimeMapper.class })
public interface ScaffoldMapper {

    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "modificationUser", ignore = true)
    @Mapping(target = "modificationDate", ignore = true)
    @Mapping(target = "modificationCount", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creationUser", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    @Mapping(target = "skills", ignore = true)
    Scaffold create(CreateScaffoldRequestDTO dto);

    @Mapping(target = "removeSkillsItem", ignore = true)
    ScaffoldDTO map(Scaffold scaffold);

    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    @Mapping(target = "skills", ignore = true)
    Scaffold map(ScaffoldDTO dto);

    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "modificationUser", ignore = true)
    @Mapping(target = "modificationDate", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creationUser", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    @Mapping(target = "skills", ignore = true)
    void update(@MappingTarget Scaffold scaffold, UpdateScaffoldRequestDTO dto);

    ScaffoldSearchCriteria mapCriteria(ScaffoldSearchCriteriaDTO dto);

    @Mapping(target = "removeStreamItem", ignore = true)
    ScaffoldPageResultDTO mapPageResult(PageResult<Scaffold> result);
}
