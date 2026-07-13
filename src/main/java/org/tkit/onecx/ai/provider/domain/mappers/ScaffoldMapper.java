package org.tkit.onecx.ai.provider.domain.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tkit.onecx.ai.provider.domain.models.GlobalScaffold;
import org.tkit.onecx.ai.provider.domain.models.Scaffold;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

@Mapper(uses = { OffsetDateTimeMapper.class, SkillMapper.class })
public interface ScaffoldMapper {

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "source", constant = "GLOBAL")
    @Mapping(target = "globalSkills", source = "globalScaffold.skills")
    Scaffold fromGlobal(GlobalScaffold globalScaffold);

}
