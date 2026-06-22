package org.tkit.onecx.ai.provider.common.services.llm;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.tkit.onecx.ai.provider.common.services.agent.AgentService;
import org.tkit.onecx.ai.provider.common.services.agentic.a2a.DefaultA2AGroupPlanner;
import org.tkit.onecx.ai.provider.common.services.execution.ExecutionService;
import org.tkit.onecx.ai.provider.domain.models.Execution;
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
    ExecutionService executionService;

    @Inject
    DefaultA2AGroupPlanner a2aGroupPlanner;

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
        // Resolve provider from agent's model
        var model = agent.getModel();
        if (model == null || model.getProvider() == null) {
            log.error("Agent {} has no associated model or provider", agent.getId());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Agent has no associated model or provider")
                    .build();
        }

        Execution execution = executionService.createExecution(agent, null, extractRequestExcerpt(chatRequestDTO));
        String executionId = execution.getExecutionId();

        AbstractLlmService service = getServiceForProvider(model.getProvider().getType());
        log.info("Routing chat request to {} service with executionId={}", model.getProvider().getType(), executionId);

        try {
            executionService.startExecution(executionId);

            if (Boolean.TRUE.equals(agent.getA2aEnabled())) {
                a2aGroupPlanner.plan(agent);
                log.info("A2A planning invoked for executionId={}", executionId);
            }

            Response serviceResponse = service.chat(agent, chatRequestDTO, executionId);
            int status = serviceResponse.getStatus();
            if (status >= 200 && status < 300) {
                executionService.succeedExecution(executionId, extractResponseExcerpt(serviceResponse));
            } else {
                executionService.failExecution(executionId, "HttpStatus:" + status, "Dispatch returned non-success status");
            }

            return Response.fromResponse(serviceResponse)
                    .header("X-Execution-Id", executionId)
                    .build();
        } catch (Exception e) {
            executionService.failExecution(executionId, e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    private String extractRequestExcerpt(ChatRequestDTOV1 chatRequestDTO) {
        if (chatRequestDTO == null || chatRequestDTO.getChatMessage() == null
                || chatRequestDTO.getChatMessage().getMessage() == null) {
            return null;
        }
        String message = chatRequestDTO.getChatMessage().getMessage();
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    private String extractResponseExcerpt(Response response) {
        if (response == null || response.getEntity() == null) {
            return null;
        }
        String text = response.getEntity().toString();
        return text.length() <= 500 ? text : text.substring(0, 500);
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
