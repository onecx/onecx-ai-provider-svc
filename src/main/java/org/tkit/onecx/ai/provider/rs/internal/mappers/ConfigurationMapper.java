package org.tkit.onecx.ai.provider.rs.internal.mappers;

import java.util.HashSet;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.tkit.onecx.ai.provider.domain.criteria.ConfigurationSearchCriteria;
import org.tkit.onecx.ai.provider.domain.models.Configuration;
import org.tkit.onecx.ai.provider.domain.models.Filter;
import org.tkit.onecx.ai.provider.domain.models.MCPServer;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.quarkus.jpa.daos.PageResult;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.provider.rs.internal.model.*;

@Mapper(uses = { OffsetDateTimeMapper.class })
public interface ConfigurationMapper {

    ConfigurationSearchCriteria mapCriteria(ConfigurationSearchCriteriaDTO aiConfigurationSearchCriteriaDTO);

    @Mapping(target = "removeStreamItem", ignore = true)
    ConfigurationPageResultDTO mapPage(PageResult<Configuration> result);

    ConfigurationAbstractDTO mapToAbstract(Configuration configuration);

    @Mapping(target = "removeMcpServersItem", ignore = true)
    @Mapping(target = "llmProvider", source = "provider")
    ConfigurationDTO map(Configuration item);

    ConfigurationFilterDTO map(Filter filter);

    Filter map(ConfigurationFilterDTO filterDTO);

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "provider", ignore = true)
    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "modificationUser", ignore = true)
    @Mapping(target = "modificationDate", ignore = true)
    @Mapping(target = "modificationCount", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creationUser", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    void updateAIConfiguration(UpdateConfigurationRequestDTO updateAIConfigurationRequestDTO,
            @MappingTarget Configuration aiConfiguration);

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "provider", source = "llmProvider")
    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "modificationUser", ignore = true)
    @Mapping(target = "modificationDate", ignore = true)
    @Mapping(target = "modificationCount", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creationUser", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    Configuration mapCreate(CreateConfigurationRequestDTO createConfigurationRequestDTO);

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    Provider mapProvider(ProviderDTO providerDTO);

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "controlTraceabilityManual", ignore = true)
    MCPServer mapMCPServer(MCPServerDTO mcpServerDTO);

    @Mapping(target = "modificationCount", source = "updateDTO.modificationCount")
    @Mapping(target = "name", source = "updateDTO.name")
    @Mapping(target = "description", source = "updateDTO.description")
    @Mapping(target = "provider", source = "provider")
    @Mapping(target = "mcpServers", source = "mcpServers")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "persisted", ignore = true)
    @Mapping(target = "modificationUser", ignore = true)
    @Mapping(target = "modificationDate", ignore = true)
    void mapUpdate(@MappingTarget Configuration aiConfiguration, UpdateConfigurationRequestDTO updateDTO,
            HashSet<MCPServer> mcpServers, Provider provider);
}
