package org.tkit.onecx.ai.provider.rs.internal.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.tkit.onecx.ai.provider.domain.criteria.ConfigurationSearchCriteria;
import org.tkit.onecx.ai.provider.domain.models.Configuration;
import org.tkit.onecx.ai.provider.domain.models.Filter;
import org.tkit.quarkus.jpa.daos.PageResult;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.model.*;

@Mapper(uses = { OffsetDateTimeMapper.class })
public interface ConfigurationMapper {

    ConfigurationSearchCriteria mapCriteria(ConfigurationSearchCriteriaDTO configurationSearchCriteriaDTO);

    @Mapping(target = "removeStreamItem", ignore = true)
    ConfigurationPageResultDTO mapPage(PageResult<Configuration> result);

    ConfigurationAbstractDTO mapToAbstract(Configuration configuration);

    @Mapping(target = "removeMcpServersItem", ignore = true)
    ConfigurationDTO map(Configuration item);

    ConfigurationFilterDTO map(Filter filter);

    Filter map(ConfigurationFilterDTO filterDTO);

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "modificationUser", ignore = true)
    @Mapping(target = "modificationDate", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creationUser", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    void updateConfiguration(UpdateConfigurationRequestDTO updateConfigurationRequestDTO,
            @MappingTarget Configuration configuration);

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "modificationUser", ignore = true)
    @Mapping(target = "modificationDate", ignore = true)
    @Mapping(target = "modificationCount", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creationUser", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    Configuration mapCreate(CreateConfigurationRequestDTO createConfigurationRequestDTO);

}
