package org.tkit.onecx.ai.provider.common.services.llm;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tkit.onecx.ai.provider.common.exceptions.ChatException;
import org.tkit.onecx.ai.provider.common.exceptions.ChatExceptionNotFound;
import org.tkit.onecx.ai.provider.common.models.ChatRequestModel;
import org.tkit.onecx.ai.provider.common.models.ChatResponseModel;
import org.tkit.onecx.ai.provider.domain.daos.ConfigurationDAO;
import org.tkit.onecx.ai.provider.domain.daos.MCPServerDAO;
import org.tkit.onecx.ai.provider.domain.daos.ProviderDAO;
import org.tkit.onecx.ai.provider.domain.models.MCPServer;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;

/**
 * Factory that selects the appropriate LLM service based on the provider configuration.
 */
@ApplicationScoped
public class LlmServiceFactory {

    private static final Logger log = LoggerFactory.getLogger(LlmServiceFactory.class.getName());

    @Inject
    OllamaLlmService ollamaLlmService;

    @Inject
    ConfigurationDAO configurationService;

    @Inject
    ProviderDAO providerDAO;

    @Inject
    MCPServerDAO mcpServerDAO;

    /**
     * Routes the chat request to the appropriate LLM service based on provider type.
     */
    public ChatResponseModel chat(ChatRequestModel chatRequest) throws ChatException {

        String filterKey = null;
        String filterValue = null;
        var context = chatRequest.getRequestContext();
        if (context != null) {
            filterKey = context.getFilterKey();
            filterValue = context.getFilterValue();
        }

        var configuration = configurationService.findConfigurationsByFilter(filterKey, filterValue);
        if (configuration == null) {
            log.error("No configuration found for request context: {}", chatRequest.getRequestContext());
            throw new ChatExceptionNotFound("No configuration found for the given request context");
        }
        if (configuration.getLlmProvider() == null) {
            log.error("Configuration provider key is empty. Request context: {}", chatRequest.getRequestContext());
            throw new ChatExceptionNotFound("Configuration provider key is empty");
        }
        var provider = providerDAO.findByKey(configuration.getLlmProvider());
        if (provider == null) {
            log.error("No configuration provider found for key: {}", configuration.getLlmProvider());
            throw new ChatExceptionNotFound("No configuration provider found for key: " + configuration.getLlmProvider());
        }
        List<MCPServer> mcpServers = List.of();
        if (!configuration.getMcpServers().isEmpty()) {
            mcpServers = mcpServerDAO.findByKeys(configuration.getMcpServers());
        }

        AbstractLlmService service = getServiceForProvider(provider.getType());
        return service.chat(configuration, provider, mcpServers, chatRequest);
    }

    /**
     * Returns the appropriate service for the given provider type.
     */
    private AbstractLlmService getServiceForProvider(ProviderType providerType) {
        log.info("Routing chat request to {} service", providerType);
        return switch (providerType) {
            case OLLAMA -> ollamaLlmService;
        };
    }
}
