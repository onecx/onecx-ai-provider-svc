package org.tkit.onecx.ai.provider.common.services.llm;

import java.time.Duration;
import java.util.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

import org.tkit.onecx.ai.provider.common.services.mcp.McpToolRegistry;
import org.tkit.onecx.ai.provider.domain.models.Configuration;
import org.tkit.onecx.ai.provider.domain.models.Provider;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
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

    @Override
    public Response chat(Configuration configuration, ChatRequestDTOV1 chatRequestDTO) {
        // Resolve configuration by queryContext
        Provider provider = configuration.getProvider();
        // Build message list: history (if present) + current message
        List<ChatMessage> messages = new ArrayList<>();

        if (chatRequestDTO.getConversation() != null
                && chatRequestDTO.getConversation().getHistory() != null
                && !chatRequestDTO.getConversation().getHistory().isEmpty()) {
            messages.addAll(mapToLangChainMessages(chatRequestDTO.getConversation().getHistory()));
        }

        messages.add(new UserMessage(chatRequestDTO.getChatMessage().getMessage()));

        OllamaChatModel model = buildModel(provider);

        // Create tool registry from MCP servers (if configured)
        McpToolRegistry toolRegistry = createToolRegistry(configuration);
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
            ChatResponse chatResponse = modelChatRequestWithRetries(model, chatRequest);

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
                    // Execute tools and get result messages
                    List<ChatMessage> toolResultMessages = executeToolRequests(chatResponse, toolRegistry);
                    messages.addAll(toolResultMessages);
                } catch (Exception e) {
                    continue;
                }

                // Send follow-up request with tool results
                ChatRequest followUpRequest = ChatRequest.builder()
                        .messages(messages)
                        .toolSpecifications(toolSpecifications)
                        .build();

                chatResponse = modelChatRequestWithRetries(model, followUpRequest);

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
        if (provider == null || provider.getLlmUrl() == null || provider.getLlmUrl().isBlank()
                || provider.getModelName() == null || provider.getModelName().isBlank()) {
            log.warn("Provider configuration incomplete for health check");
            return UNHEALTHY;
        }
        try {
            OllamaChatModel model = buildModel(provider);

            ChatRequest healthCheckRequest = ChatRequest.builder()
                    .messages(List.of(new UserMessage(HEALTH_CHECK_PROMPT)))
                    .build();
            ChatResponse response = modelChatRequestWithRetries(model, healthCheckRequest);
            if (response == null || response.aiMessage() == null) {
                log.warn("Ollama model health check failed for model '{}' at '{}'", provider.getModelName(),
                        provider.getLlmUrl());
                return UNHEALTHY;
            }
            return HEALTHY;
        } catch (Exception e) {
            log.warn("Ollama model health check failed for model '{}' at '{}': {}",
                    provider.getModelName(), provider.getLlmUrl(), e.getMessage());
            return UNHEALTHY;
        }
    }

    private OllamaChatModel buildModel(Provider provider) {
        return OllamaChatModel.builder()
                .baseUrl(provider.getLlmUrl())
                .modelName(provider.getModelName())
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
