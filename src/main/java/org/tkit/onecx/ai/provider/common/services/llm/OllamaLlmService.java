package org.tkit.onecx.ai.provider.common.services.llm;

import java.time.Duration;
import java.util.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.tkit.onecx.ai.provider.common.services.agentic.ScaffoldPromptComposer;
import org.tkit.onecx.ai.provider.common.services.mcp.McpToolRegistry;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.Provider;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import io.quarkiverse.langchain4j.jaxrsclient.JaxRsHttpClientBuilderFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class OllamaLlmService extends AbstractLlmService {

    private static final String HEALTHY = "HEALTHY";
    private static final String UNHEALTHY = "UNHEALTHY";
    private static final String HEALTH_CHECK_PROMPT = "ping";

    @Inject
    ScaffoldPromptComposer scaffoldPromptComposer;

    @Override
    public Response chat(Agent agent, ChatRequestDTOV1 chatRequestDTO, String executionId) {
        // Resolve agent's model and provider
        var model = agent.getModel();
        if (model == null || model.getProvider() == null) {
            log.error("Agent {} has no associated model or provider", agent.getId());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Agent has no associated model or provider")
                    .build();
        }
        Provider provider = model.getProvider();

        // Build message list: history (if present) + current message
        List<ChatMessage> messages = new ArrayList<>();

        String systemPrompt = scaffoldPromptComposer.compose(agent, chatRequestDTO);
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new SystemMessage(systemPrompt));
        }

        if (chatRequestDTO.getConversation() != null
                && chatRequestDTO.getConversation().getHistory() != null
                && !chatRequestDTO.getConversation().getHistory().isEmpty()) {
            messages.addAll(mapToLangChainMessages(chatRequestDTO.getConversation().getHistory()));
        }

        messages.add(new UserMessage(chatRequestDTO.getChatMessage().getMessage()));

        OllamaChatModel ollamaModel = buildModel(provider, model.getModelIdentifier());

        // Create tool registry from agent tools (if configured)
        McpToolRegistry toolRegistry = createToolRegistry(agent, executionId);
        try {
            List<ToolSpecification> toolSpecifications = toolRegistry.getToolSpecifications();

            // Build initial chat request
            ChatRequest.Builder chatRequestBuilder = ChatRequest.builder()
                    .messages(messages);

            if (!toolSpecifications.isEmpty()) {
                log.info("Using {} tool(s) for this request", toolSpecifications.size());
                chatRequestBuilder.toolSpecifications(toolSpecifications);
            }

            ChatRequest chatRequest = chatRequestBuilder.build();
            ChatResponse chatResponse = modelChatRequestWithRetries(ollamaModel, chatRequest);

            if (chatResponse == null) {
                log.error("Failed to get response from model after retries");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Failed to get response from model")
                        .build();
            }

            // Handle tool execution loop
            int iterations = 0;
            while (hasToolExecutionRequests(chatResponse) && iterations < dispatchConfig.mcpConfig().maxIterations()) {
                iterations++;
                log.info("Tool execution iteration {}", iterations);

                try {
                    transitionToWaitingTool(executionId);
                    // Execute tools and get result messages
                    List<ChatMessage> toolResultMessages = executeToolRequests(agent, executionId, chatResponse, toolRegistry);
                    messages.addAll(toolResultMessages);
                    transitionToRunning(executionId);
                } catch (Exception e) {
                    transitionToRunning(executionId);
                    continue;
                }

                // Send follow-up request with tool results
                ChatRequest followUpRequest = ChatRequest.builder()
                        .messages(messages)
                        .toolSpecifications(toolSpecifications)
                        .build();

                chatResponse = modelChatRequestWithRetries(ollamaModel, followUpRequest);

                // Check if follow-up request failed
                if (chatResponse == null) {
                    log.error("Failed to get follow-up response from model during tool execution iteration {}", iterations);
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Failed to get follow-up response from model during tool execution")
                            .build();
                }
            }

            if (iterations >= dispatchConfig.mcpConfig().maxIterations()) {
                log.warn("Reached maximum tool execution iterations ({})", dispatchConfig.mcpConfig().maxIterations());
            }

            // Get final response text
            String responseMessage = chatResponse.aiMessage().text();
            var responseDTO = mapToChatMessageResponseDTO(responseMessage);
            return Response.ok(responseDTO).build();
        } catch (Exception e) {
            log.error("Unexpected error during chat processing", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Unexpected error: " + e.getMessage())
                    .build();
        } finally {
            toolRegistry.close();
        }
    }

    @Override
    public String getHealthStatus(Provider provider) {
        if (provider == null || provider.getLlmUrl() == null || provider.getLlmUrl().isBlank()) {
            log.warn("Provider configuration incomplete for health check");
            return UNHEALTHY;
        }
        try {
            // Note: for health check, we need a model. We'll use a default model name for the health check
            OllamaChatModel ollamaModel = buildModel(provider, "llama2");

            ChatRequest healthCheckRequest = ChatRequest.builder()
                    .messages(List.of(new UserMessage(HEALTH_CHECK_PROMPT)))
                    .build();
            ChatResponse response = modelChatRequestWithRetries(ollamaModel, healthCheckRequest);
            if (response == null || response.aiMessage() == null) {
                log.warn("Ollama health check failed for provider '{}' at '{}'", provider.getName(), provider.getLlmUrl());
                return UNHEALTHY;
            }
            return HEALTHY;
        } catch (Exception e) {
            log.warn("Ollama health check failed for provider '{}' at '{}': {}", provider.getName(), provider.getLlmUrl(),
                    e.getMessage());
            return UNHEALTHY;
        }
    }

    private OllamaChatModel buildModel(Provider provider, String modelIdentifier) {
        return OllamaChatModel.builder()
                .baseUrl(provider.getLlmUrl())
                .modelName(modelIdentifier)
                .customHeaders(createCustomHeaders(provider))
                .timeout(Duration.ofSeconds(dispatchConfig.providerConfig().timeout()))
                .logRequests(dispatchConfig.providerConfig().logRequests())
                .logResponses(dispatchConfig.providerConfig().logResponse())
                .httpClientBuilder(new JaxRsHttpClientBuilderFactory().create())
                .build();
    }

    private Map<String, String> createCustomHeaders(Provider provider) {
        Map<String, String> customHeaders = new HashMap<>();
        if (provider.getApiKey() != null && !provider.getApiKey().isBlank()) {
            customHeaders.put("Authorization", provider.getApiKey());
        }
        return customHeaders;
    }

}
