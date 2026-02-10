package org.tkit.onecx.ai.provider.common.services.configuration;

import java.util.HashSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.tkit.onecx.ai.provider.domain.daos.ConfigurationDAO;
import org.tkit.onecx.ai.provider.domain.daos.MCPServerDAO;
import org.tkit.onecx.ai.provider.domain.daos.ProviderDAO;
import org.tkit.onecx.ai.provider.domain.models.Configuration;
import org.tkit.onecx.ai.provider.domain.models.MCPServer;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ConfigurationMapper;
import org.tkit.onecx.ai.provider.rs.internal.mappers.MCPServerMapper;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ProviderMapper;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.RequestContextDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.UpdateConfigurationRequestDTO;

@ApplicationScoped
public class ConfigurationService {

    @Inject
    ConfigurationDAO configurationDAO;

    @Inject
    ProviderDAO providerDAO;

    @Inject
    MCPServerDAO mcpServerDAO;

    @Inject
    ConfigurationMapper configurationMapper;

    @Inject
    MCPServerMapper mcpServerMapper;

    @Inject
    ProviderMapper providerMapper;

    @Transactional
    public Configuration createConfiguration(Configuration configuration) {
        if (configuration.getProvider() != null) {
            var provider = providerDAO.findById(configuration.getProvider().getId());
            if (provider == null) {
                provider = providerDAO.create(configuration.getProvider());
            }
            configuration.setProvider(provider);

        }
        var mcpServers = configuration.getMcpServers();
        var mcpServersToAdd = new HashSet<MCPServer>();
        mcpServers.forEach(mcpServer -> {
            var existingMcpServer = mcpServerDAO.findById(mcpServer.getId());
            if (existingMcpServer == null) {
                existingMcpServer = mcpServerDAO.create(mcpServer);
            }
            mcpServersToAdd.add(existingMcpServer);
        });
        var mcpServerList = mcpServerDAO.create(mcpServersToAdd.stream().toList());
        configuration.setMcpServers(new HashSet<>(mcpServerList.toList()));
        return configurationDAO.create(configuration);
    }

    public Configuration updateConfiguration(UpdateConfigurationRequestDTO updateAIConfigurationRequestDTO, String configId) {
        //check if configuration exists
        var aiConfiguration = configurationDAO.findById(configId);
        if (aiConfiguration == null) {
            return null;
        }

        Provider provider = null;
        if (updateAIConfigurationRequestDTO.getLlmProvider() != null) {
            provider = providerDAO.findById(updateAIConfigurationRequestDTO.getLlmProvider().getId());
            if (provider == null) {
                provider = providerDAO.create(providerMapper.map(updateAIConfigurationRequestDTO.getLlmProvider()));
            }
        }

        var mcpServers = updateAIConfigurationRequestDTO.getMcpServers();
        var mcpServersToAdd = new HashSet<MCPServer>();
        mcpServers.forEach(mcpServer -> {
            var existingMcpServer = mcpServerDAO.findById(mcpServer.getId());
            if (existingMcpServer == null) {
                existingMcpServer = mcpServerDAO.create(mcpServerMapper.map(mcpServer));
            }
            mcpServersToAdd.add(existingMcpServer);
        });
        configurationMapper.mapUpdate(aiConfiguration, updateAIConfigurationRequestDTO,
                new HashSet<>(mcpServersToAdd),
                provider);

        return configurationDAO.update(aiConfiguration);
    }

    public Configuration findConfigurationsByRequestContext(RequestContextDTOV1 requestContext) {

        String filterKey = (requestContext != null && requestContext.getFilter() != null
                && requestContext.getFilter().getKey() != null)
                        ? requestContext.getFilter().getKey().value()
                        : null;
        String filterValue = (requestContext != null && requestContext.getFilter() != null
                && requestContext.getFilter().getValue() != null)
                        ? requestContext.getFilter().getValue()
                        : null;

        var configurations = configurationDAO.findAllConfigurationsByFilterKey(filterKey);

        if (filterValue == null) {
            return configurations.stream()
                    .filter(c -> c.getFilter() == null || c.getFilter().getValue() == null)
                    .findFirst()
                    .orElse(null);
        }

        return configurations.stream()
                .filter(c -> c.getFilter() != null
                        && c.getFilter().getValue() != null
                        && filterValue.matches(c.getFilter().getValue().replace("*", ".*")))
                .max((c1, c2) -> {
                    int len1 = c1.getFilter().getValue().replace("*", "").length();
                    int len2 = c2.getFilter().getValue().replace("*", "").length();
                    return Integer.compare(len1, len2);
                })
                .orElse(null);
    }
}
