package org.tkit.onecx.ai.provider.rs.internal.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.tkit.onecx.ai.provider.domain.criteria.SkillSearchCriteria;
import org.tkit.onecx.ai.provider.domain.models.Skill;
import org.tkit.quarkus.jpa.daos.PageResult;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.model.CreateSkillRequestDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.SkillDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.SkillPageResultDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.SkillSearchCriteriaDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.UpdateSkillRequestDTO;

@Mapper(uses = { OffsetDateTimeMapper.class })
public interface SkillMapper {

    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "modificationUser", ignore = true)
    @Mapping(target = "modificationDate", ignore = true)
    @Mapping(target = "modificationCount", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creationUser", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    Skill create(CreateSkillRequestDTO dto);

    SkillDTO map(Skill skill);

    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    Skill map(SkillDTO dto);

    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "modificationUser", ignore = true)
    @Mapping(target = "modificationDate", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creationUser", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    void update(@MappingTarget Skill skill, UpdateSkillRequestDTO dto);

    SkillSearchCriteria mapCriteria(SkillSearchCriteriaDTO dto);

    @Mapping(target = "removeStreamItem", ignore = true)
    SkillPageResultDTO mapPageResult(PageResult<Skill> result);
}
