package org.tkit.onecx.ai.provider.common.services.llm;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;

/**
 * Dispatches a single agent invocation to the correct provider-specific LLM service.
 */
@ApplicationScoped
public class AgentDispatchService {

    @Inject
    OllamaLlmService ollamaLlmService;

    public Response dispatch(Agent agent, ChatRequestDTOV1 chatRequestDTO, String executionId) {
        if (agent == null || agent.getModel() == null || agent.getModel().getProvider() == null) {
            throw new IllegalArgumentException("Agent has no associated model or provider");
        }

        Provider provider = agent.getModel().getProvider();
        AbstractLlmService service = getServiceForProvider(provider.getType());
        return service.chat(agent, chatRequestDTO, executionId);
    }

    public Response healthCheck(Provider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Provider cannot be null");
        }
        return switch (provider.getType()) {
            case OLLAMA -> Response.ok(ollamaLlmService.getHealthStatus(provider)).build();
            case OPENAI -> throw new IllegalArgumentException(
                    "Provider type not yet supported in current in-process runtime: " + provider.getType());
        };
    }

    private AbstractLlmService getServiceForProvider(ProviderType providerType) {
        return switch (providerType) {
            case OLLAMA -> ollamaLlmService;
            case OPENAI -> throw new IllegalArgumentException(
                    "Provider type not yet supported in current in-process runtime: " + providerType);
        };
    }
}
