package org.tkit.onecx.ai.provider.common.services.llm;

import java.time.Duration;
import java.util.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.HttpHeaders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tkit.onecx.ai.provider.common.exceptions.ChatException;
import org.tkit.onecx.ai.provider.common.exceptions.ChatExceptionBadRequest;
import org.tkit.onecx.ai.provider.common.models.ChatRequestModel;
import org.tkit.onecx.ai.provider.common.models.ChatResponseModel;
import org.tkit.onecx.ai.provider.common.services.mcp.McpToolRegistry;
import org.tkit.onecx.ai.provider.domain.models.Configuration;
import org.tkit.onecx.ai.provider.domain.models.MCPServer;
import org.tkit.onecx.ai.provider.domain.models.Provider;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import io.quarkiverse.langchain4j.jaxrsclient.JaxRsHttpClientBuilderFactory;

@ApplicationScoped
public class OllamaLlmService extends AbstractLlmService {

    private static final Logger log = LoggerFactory.getLogger(OllamaLlmService.class);

    @Override
    public ChatResponseModel chat(Configuration configuration, Provider provider, List<MCPServer> mcpServers,
            ChatRequestModel request) throws ChatException {

        // Build message list: history (if present) + current message
        List<ChatMessage> messages = new ArrayList<>();

        if (request.getConversation() != null) {
            messages.addAll(mapToLangChainMessages(request.getConversation().getHistory()));
        }
        messages.add(new UserMessage(request.getChatMessage().getMessage()));

        // Build the Ollama model
        var builder = OllamaChatModel.builder()
                .baseUrl(provider.getLlmUrl())
                .modelName(provider.getModelName())
                .timeout(Duration.ofSeconds(dispatchConfig.providerConfig().timeout()))
                .logRequests(dispatchConfig.providerConfig().logRequests())
                .logResponses(dispatchConfig.providerConfig().logResponse())
                .httpClientBuilder(new JaxRsHttpClientBuilderFactory().create());

        // Build custom headers for authentication
        if (provider.getApiKey() != null) {
            builder.customHeaders(Map.of(HttpHeaders.AUTHORIZATION, provider.getApiKey()));
        }
        var model = builder.build();

        // Create tool registry from MCP servers (if configured)
        McpToolRegistry toolRegistry = mcpService.createToolRegistry(mcpServers);
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
                throw new ChatExceptionBadRequest("Failed to get response from model");
            }

            // Handle tool execution loop
            int iterations = 0;
            while (chatResponse.aiMessage().hasToolExecutionRequests()
                    && iterations < dispatchConfig.mcpConfig().maxIterations()) {
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
                    throw new ChatExceptionBadRequest("Failed to get follow-up response from model during tool execution");
                }
            }

            if (iterations >= dispatchConfig.mcpConfig().maxIterations()) {
                log.warn("Reached maximum tool execution iterations ({})", dispatchConfig.mcpConfig().maxIterations());
            }

            // Get final response text

            var result = new ChatResponseModel();
            result.setConversationId(chatResponse.id());
            if (chatResponse.aiMessage() != null) {
                result.setMessage(chatResponse.aiMessage().text());
            }
            result.setType(ChatResponseModel.Type.ASSISTANT);

            return result;
        } catch (Exception e) {
            log.error("Unexpected error during chat processing", e);
            throw new ChatExceptionBadRequest("Unexpected error during chat processing: " + e.getMessage());
        } finally {
            toolRegistry.close();
        }
    }
}
