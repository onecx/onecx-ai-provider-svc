package org.tkit.onecx.ai.provider.rs.internal.mappers;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.tkit.onecx.ai.provider.domain.models.DangerPattern;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.model.CreateDangerPatternRequestDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.DangerPatternDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.UpdateDangerPatternRequestDTO;

@Mapper(uses = { OffsetDateTimeMapper.class })
public interface DangerPatternMapper {

    @Mapping(target = "id", source = "id")
    DangerPatternDTO map(DangerPattern pattern);

    List<DangerPatternDTO> map(List<DangerPattern> patterns);

    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "modificationUser", ignore = true)
    @Mapping(target = "modificationDate", ignore = true)
    @Mapping(target = "modificationCount", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creationUser", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    DangerPattern create(CreateDangerPatternRequestDTO dto);

    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "modificationUser", ignore = true)
    @Mapping(target = "modificationDate", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creationUser", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    void update(@MappingTarget DangerPattern pattern, UpdateDangerPatternRequestDTO dto);
}
