package org.tkit.onecx.ai.provider.domain.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tkit.onecx.ai.provider.domain.models.GlobalSkill;
import org.tkit.onecx.ai.provider.domain.models.Skill;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

@Mapper(uses = OffsetDateTimeMapper.class)
public interface SkillMapper {

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "source", constant = "GLOBAL")
    Skill fromGlobal(GlobalSkill globalSkill);
}
