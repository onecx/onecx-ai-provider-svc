package org.tkit.onecx.ai.provider.common.services.llm;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.tkit.onecx.ai.provider.common.services.agent.AgentService;
import org.tkit.onecx.ai.provider.common.services.agentic.a2a.A2AOrchestrationService;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory that selects the appropriate LLM service based on the agent configuration.
 */
@Slf4j
@ApplicationScoped
public class LlmServiceFactory {

    @Inject
    OllamaLlmService ollamaLlmService;

    @Inject
    AgentService agentService;

    @Inject
    A2AOrchestrationService a2aOrchestrationService;

    /**
     * Routes the chat request to the appropriate LLM service based on provider type.
     */
    public Response chat(ChatRequestDTOV1 chatRequestDTO) {
        var agent = agentService.findAgentByRequestContext(chatRequestDTO.getRequestContext());
        if (agent == null) {
            log.error("No agent found for request context: {}", chatRequestDTO.getRequestContext());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("No agent found for the given request context")
                    .build();
        }
        return a2aOrchestrationService.invokeRoot(agent, chatRequestDTO);
    }

    public String getProviderHealthStatus(Provider provider) {
        AbstractLlmService service = getServiceForProvider(provider.getType());
        return service.getHealthStatus(provider);
    }

    /**
     * Returns the appropriate service for the given provider type.
     */
    private AbstractLlmService getServiceForProvider(ProviderType providerType) {
        return switch (providerType) {
            case OLLAMA -> ollamaLlmService;
            case OPENAI -> throw new IllegalArgumentException(
                    "Provider type not yet supported in current in-process runtime: " + providerType);
        };
    }
}
