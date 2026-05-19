package org.tkit.onecx.ai.provider.common.services.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.domain.daos.ConfigurationDAO;
import org.tkit.onecx.ai.provider.domain.daos.MCPServerDAO;
import org.tkit.onecx.ai.provider.domain.daos.ProviderDAO;
import org.tkit.onecx.ai.provider.domain.models.Configuration;
import org.tkit.onecx.ai.provider.domain.models.Filter;
import org.tkit.onecx.ai.provider.domain.models.MCPServer;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.FilterKey;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ConfigurationMapper;
import org.tkit.onecx.ai.provider.rs.internal.mappers.MCPServerMapper;
import org.tkit.onecx.ai.provider.rs.internal.mappers.ProviderMapper;
import org.tkit.onecx.ai.provider.test.AbstractTest;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.RequestContextDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.MCPServerDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ProviderDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.UpdateConfigurationRequestDTO;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ConfigurationServiceTest extends AbstractTest {

    private ConfigurationService service;
    private ConfigurationDAO configurationDAO;
    private ProviderDAO providerDAO;
    private MCPServerDAO mcpServerDAO;
    private ConfigurationMapper configurationMapper;
    private MCPServerMapper mcpServerMapper;
    private ProviderMapper providerMapper;

    @BeforeEach
    void init() {
        service = new ConfigurationService();
        configurationDAO = mock(ConfigurationDAO.class);
        providerDAO = mock(ProviderDAO.class);
        mcpServerDAO = mock(MCPServerDAO.class, RETURNS_DEEP_STUBS);
        configurationMapper = mock(ConfigurationMapper.class);
        mcpServerMapper = mock(MCPServerMapper.class);
        providerMapper = mock(ProviderMapper.class);

        service.configurationDAO = configurationDAO;
        service.providerDAO = providerDAO;
        service.mcpServerDAO = mcpServerDAO;
        service.configurationMapper = configurationMapper;
        service.mcpServerMapper = mcpServerMapper;
        service.providerMapper = providerMapper;
    }

    @Test
    void createConfiguration_withExistingProvider_andMixedMcpServers() {
        Configuration configuration = new Configuration();
        Provider provider = new Provider();
        provider.setId("p-1");
        configuration.setProvider(provider);

        MCPServer mcpExistingInput = mcp("mcp-1");
        MCPServer mcpNewInput = mcp("mcp-2");
        configuration.setMcpServers(Set.of(mcpExistingInput, mcpNewInput));

        Provider existingProvider = new Provider();
        existingProvider.setId("p-1");
        when(providerDAO.findById("p-1")).thenReturn(existingProvider);

        MCPServer existingMcp = mcp("mcp-1");
        MCPServer createdMcp = mcp("mcp-2");
        when(mcpServerDAO.findById("mcp-1")).thenReturn(existingMcp);
        when(mcpServerDAO.findById("mcp-2")).thenReturn(null);
        when(mcpServerDAO.create(mcpNewInput)).thenReturn(createdMcp);
        when(mcpServerDAO.create(org.mockito.ArgumentMatchers.<MCPServer> anyList()).toList())
                .thenReturn(List.of(existingMcp, createdMcp));
        when(configurationDAO.create(configuration)).thenReturn(configuration);

        Configuration result = service.createConfiguration(configuration);

        assertThat(result).isSameAs(configuration);
        assertThat(configuration.getProvider()).isSameAs(existingProvider);
        assertThat(configuration.getMcpServers()).containsExactlyInAnyOrder(existingMcp, createdMcp);
        verify(providerDAO, never()).create(any(Provider.class));
    }

    @Test
    void createConfiguration_withNullProvider_createsProvider() {
        Configuration configuration = new Configuration();
        Provider providerInput = new Provider();
        providerInput.setId("p-new");
        configuration.setProvider(providerInput);

        MCPServer mcpInput = mcp("mcp-1");
        configuration.setMcpServers(Set.of(mcpInput));

        Provider createdProvider = new Provider();
        createdProvider.setId("p-new");
        MCPServer existingMcp = mcp("mcp-1");

        when(providerDAO.findById("p-new")).thenReturn(null);
        when(providerDAO.create(providerInput)).thenReturn(createdProvider);
        when(mcpServerDAO.findById("mcp-1")).thenReturn(existingMcp);
        when(mcpServerDAO.create(org.mockito.ArgumentMatchers.<MCPServer> anyList()).toList())
                .thenReturn(List.of(existingMcp));
        when(configurationDAO.create(configuration)).thenReturn(configuration);

        Configuration result = service.createConfiguration(configuration);

        assertThat(result).isSameAs(configuration);
        verify(providerDAO).create(providerInput);
    }

    @Test
    void createConfiguration_withoutProvider_skipsProviderLookup() {
        Configuration configuration = new Configuration();
        configuration.setProvider(null);

        MCPServer mcpInput = mcp("mcp-1");
        configuration.setMcpServers(Set.of(mcpInput));

        MCPServer existingMcp = mcp("mcp-1");
        when(mcpServerDAO.findById("mcp-1")).thenReturn(existingMcp);
        when(mcpServerDAO.create(org.mockito.ArgumentMatchers.<MCPServer> anyList()).toList())
                .thenReturn(List.of(existingMcp));
        when(configurationDAO.create(configuration)).thenReturn(configuration);

        Configuration result = service.createConfiguration(configuration);

        assertThat(result).isSameAs(configuration);
        assertThat(configuration.getMcpServers()).containsExactly(existingMcp);
        verifyNoInteractions(providerDAO);
    }

    @Test
    void updateConfiguration_returnsNull_whenConfigNotFound() {
        UpdateConfigurationRequestDTO update = mock(UpdateConfigurationRequestDTO.class);
        when(configurationDAO.findById("missing")).thenReturn(null);

        Configuration result = service.updateConfiguration(update, "missing");

        assertThat(result).isNull();
    }

    @Test
    void updateConfiguration_withNullLlmProvider_usesNullProviderInMapUpdate() {
        Configuration existingConfig = new Configuration();
        UpdateConfigurationRequestDTO update = mock(UpdateConfigurationRequestDTO.class);
        MCPServerDTO mcpDto = mock(MCPServerDTO.class);
        when(mcpDto.getId()).thenReturn("mcp-1");
        when(update.getLlmProvider()).thenReturn(null);
        when(update.getMcpServers()).thenReturn(List.of(mcpDto));

        MCPServer existingMcp = mcp("mcp-1");
        when(configurationDAO.findById("cfg-1")).thenReturn(existingConfig);
        when(mcpServerDAO.findById("mcp-1")).thenReturn(existingMcp);
        when(configurationDAO.update(existingConfig)).thenReturn(existingConfig);

        Configuration result = service.updateConfiguration(update, "cfg-1");

        assertThat(result).isSameAs(existingConfig);
        verify(configurationMapper).mapUpdate(eq(existingConfig), eq(update),
                org.mockito.ArgumentMatchers.<HashSet<MCPServer>> any(), eq(null));
        verify(providerDAO, never()).findById(any());
    }

    @Test
    void updateConfiguration_withMissingProvider_createsProvider_andCreatesMissingMcp() {
        Configuration existingConfig = new Configuration();
        UpdateConfigurationRequestDTO update = mock(UpdateConfigurationRequestDTO.class, RETURNS_DEEP_STUBS);
        ProviderDTO providerDto = mock(ProviderDTO.class);
        MCPServerDTO mcpDto = mock(MCPServerDTO.class);

        when(providerDto.getId()).thenReturn("provider-1");
        when(update.getLlmProvider()).thenReturn(providerDto);
        when(update.getMcpServers()).thenReturn(List.of(mcpDto));
        when(mcpDto.getId()).thenReturn("mcp-1");

        Provider mappedProvider = new Provider();
        mappedProvider.setId("provider-1");
        Provider createdProvider = new Provider();
        createdProvider.setId("provider-1");

        MCPServer mappedMcp = mcp("mcp-1");
        MCPServer createdMcp = mcp("mcp-1");

        when(configurationDAO.findById("cfg-1")).thenReturn(existingConfig);
        when(providerDAO.findById("provider-1")).thenReturn(null);
        when(providerMapper.map(providerDto)).thenReturn(mappedProvider);
        when(providerDAO.create(mappedProvider)).thenReturn(createdProvider);

        when(mcpServerDAO.findById("mcp-1")).thenReturn(null);
        when(mcpServerMapper.map(mcpDto)).thenReturn(mappedMcp);
        when(mcpServerDAO.create(mappedMcp)).thenReturn(createdMcp);

        when(configurationDAO.update(existingConfig)).thenReturn(existingConfig);

        Configuration result = service.updateConfiguration(update, "cfg-1");

        assertThat(result).isSameAs(existingConfig);
        verify(configurationMapper).mapUpdate(eq(existingConfig), eq(update),
                org.mockito.ArgumentMatchers.<HashSet<MCPServer>> any(), eq(createdProvider));
    }

    @Test
    void updateConfiguration_withExistingProvider_doesNotCreateProvider() {
        Configuration existingConfig = new Configuration();
        UpdateConfigurationRequestDTO update = mock(UpdateConfigurationRequestDTO.class, RETURNS_DEEP_STUBS);
        ProviderDTO providerDto = mock(ProviderDTO.class);

        when(providerDto.getId()).thenReturn("provider-1");
        when(update.getLlmProvider()).thenReturn(providerDto);
        when(update.getMcpServers()).thenReturn(List.of());

        Provider existingProvider = new Provider();
        existingProvider.setId("provider-1");

        when(configurationDAO.findById("cfg-1")).thenReturn(existingConfig);
        when(providerDAO.findById("provider-1")).thenReturn(existingProvider);
        when(configurationDAO.update(existingConfig)).thenReturn(existingConfig);

        Configuration result = service.updateConfiguration(update, "cfg-1");

        assertThat(result).isSameAs(existingConfig);
        verify(providerDAO, never()).create(any(Provider.class));
        verify(configurationMapper).mapUpdate(eq(existingConfig), eq(update),
                org.mockito.ArgumentMatchers.<HashSet<MCPServer>> any(), eq(existingProvider));
    }

    @Test
    void findConfigurationsByRequestContext_whenRequestContextNull() {
        Configuration noFilter = new Configuration();
        when(configurationDAO.findAllConfigurationsByFilterKey(null))
                .thenReturn(List.of(noFilter));

        Configuration result = service.findConfigurationsByRequestContext(null);

        assertThat(result).isSameAs(noFilter);
    }

    @Test
    void findConfigurationsByRequestContext_whenRequestFilterNull() {
        RequestContextDTOV1 context = mock(RequestContextDTOV1.class);
        when(context.getFilter()).thenReturn(null);

        Configuration noFilter = new Configuration();
        when(configurationDAO.findAllConfigurationsByFilterKey(null))
                .thenReturn(List.of(noFilter));

        Configuration result = service.findConfigurationsByRequestContext(context);

        assertThat(result).isSameAs(noFilter);
    }

    @Test
    void findConfigurationsByRequestContext_whenFilterValueNull_returnsFirstWithoutFilterValue() {
        Configuration withFilterValue = configWithFilter("onecx-*");
        Configuration withoutFilter = new Configuration();

        when(configurationDAO.findAllConfigurationsByFilterKey(null)).thenReturn(List.of(withFilterValue, withoutFilter));

        Configuration result = service.findConfigurationsByRequestContext(null);

        assertThat(result).isSameAs(withoutFilter);
    }

    @Test
    void findConfigurationsByRequestContext_whenValuePresent_skipsConfigWithNullFilter() {
        RequestContextDTOV1 context = mock(RequestContextDTOV1.class, RETURNS_DEEP_STUBS);
        when(context.getFilter().getKey().value()).thenReturn("APP_ID");
        when(context.getFilter().getValue()).thenReturn("onecx-app-admin");

        Configuration withoutFilter = new Configuration();
        withoutFilter.setFilter(null);
        Configuration specific = configWithFilter("onecx-app-*");

        when(configurationDAO.findAllConfigurationsByFilterKey("APP_ID"))
                .thenReturn(List.of(withoutFilter, specific));

        Configuration result = service.findConfigurationsByRequestContext(context);

        assertThat(result).isSameAs(specific);
    }

    @Test
    void findConfigurationsByRequestContext_whenValuePresent_skipsConfigWithNullFilterValue() {
        RequestContextDTOV1 context = mock(RequestContextDTOV1.class, RETURNS_DEEP_STUBS);
        when(context.getFilter().getKey().value()).thenReturn("APP_ID");
        when(context.getFilter().getValue()).thenReturn("onecx-app-admin");

        Configuration withNullValue = new Configuration();
        Filter filterNull = new Filter();
        filterNull.setKey(FilterKey.APP_ID);
        filterNull.setValue(null);
        withNullValue.setFilter(filterNull);

        Configuration specific = configWithFilter("onecx-app-*");

        when(configurationDAO.findAllConfigurationsByFilterKey("APP_ID"))
                .thenReturn(List.of(withNullValue, specific));

        Configuration result = service.findConfigurationsByRequestContext(context);

        assertThat(result).isSameAs(specific);
    }

    @Test
    void findConfigurationsByRequestContext_whenValuePresent_returnsMostSpecificMatch() {
        RequestContextDTOV1 context = mock(RequestContextDTOV1.class, RETURNS_DEEP_STUBS);
        when(context.getFilter().getKey().value()).thenReturn("APP_ID");
        when(context.getFilter().getValue()).thenReturn("onecx-app-admin");

        Configuration broad = configWithFilter("onecx-*");
        Configuration specific = configWithFilter("onecx-app-*");
        Configuration nonMatching = configWithFilter("other-*");

        when(configurationDAO.findAllConfigurationsByFilterKey("APP_ID"))
                .thenReturn(List.of(broad, specific, nonMatching));

        Configuration result = service.findConfigurationsByRequestContext(context);

        assertThat(result).isSameAs(specific);
    }

    @Test
    void findConfigurationsByRequestContext_whenNoMatch_returnsNull() {
        RequestContextDTOV1 context = mock(RequestContextDTOV1.class, RETURNS_DEEP_STUBS);
        when(context.getFilter().getKey().value()).thenReturn("APP_ID");
        when(context.getFilter().getValue()).thenReturn("no-match");

        when(configurationDAO.findAllConfigurationsByFilterKey("APP_ID"))
                .thenReturn(List.of(configWithFilter("onecx-*"), configWithFilter("another-*")));

        Configuration result = service.findConfigurationsByRequestContext(context);

        assertThat(result).isNull();
    }

    @Test
    void findConfigurationsByRequestContext_whenFilterKeyNull_usesNullKey() {
        RequestContextDTOV1 context = mock(RequestContextDTOV1.class, RETURNS_DEEP_STUBS);
        when(context.getFilter().getKey()).thenReturn(null);
        when(context.getFilter().getValue()).thenReturn(null);

        Configuration withoutFilterValue = new Configuration();
        Filter filterWithoutValue = new Filter();
        filterWithoutValue.setValue(null);
        withoutFilterValue.setFilter(filterWithoutValue);

        when(configurationDAO.findAllConfigurationsByFilterKey(null)).thenReturn(List.of(withoutFilterValue));

        Configuration result = service.findConfigurationsByRequestContext(context);

        assertThat(result).isSameAs(withoutFilterValue);
    }

    private static MCPServer mcp(String id) {
        MCPServer mcp = new MCPServer();
        mcp.setId(id);
        return mcp;
    }

    private static Configuration configWithFilter(String pattern) {
        Configuration configuration = new Configuration();
        Filter filter = new Filter();
        filter.setKey(FilterKey.APP_ID);
        filter.setValue(pattern);
        configuration.setFilter(filter);
        return configuration;
    }
}
