package org.tkit.onecx.ai.provider.common.services.llm;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.tkit.onecx.ai.provider.common.config.DispatchConfig;
import org.tkit.onecx.ai.provider.common.exceptions.ChatException;
import org.tkit.onecx.ai.provider.common.models.*;
import org.tkit.onecx.ai.provider.common.models.ChatMessage;
import org.tkit.onecx.ai.provider.common.services.mcp.McpService;
import org.tkit.onecx.ai.provider.common.services.mcp.McpToolRegistry;
import org.tkit.onecx.ai.provider.domain.models.Configuration;
import org.tkit.onecx.ai.provider.domain.models.MCPServer;
import org.tkit.onecx.ai.provider.domain.models.Provider;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractLlmService {

    @Inject
    McpService mcpService;

    @Inject
    DispatchConfig dispatchConfig;

    @Inject
    Executor executor;

    public abstract ChatResponseModel chat(Configuration configuration, Provider provider, List<MCPServer> mcpServers,
            ChatRequestModel chatRequest) throws ChatException;

    /**
     * Executes all tool requests from the LLM response and returns the results as messages.
     *
     * @param response The LLM response containing tool execution requests
     * @param toolRegistry The registry containing available tools
     * @return List of messages including the AI message and tool execution results
     */
    protected List<dev.langchain4j.data.message.ChatMessage> executeToolRequests(ChatResponse response,
            McpToolRegistry toolRegistry) {
        List<dev.langchain4j.data.message.ChatMessage> resultMessages = new ArrayList<>();

        AiMessage aiMessage = response.aiMessage();
        resultMessages.add(aiMessage);

        for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
            String toolName = toolRequest.name();

            log.info("LLM requested tool execution: {} with arguments: {}", toolName, toolRequest.arguments());

            var toolOpt = toolRegistry.findByName(toolName);
            if (toolOpt.isEmpty()) {
                log.error("Tool '{}' not found in registry", toolName);
                resultMessages.add(ToolExecutionResultMessage.from(
                        toolRequest,
                        "Error: Tool '" + toolName + "' not found"));
                continue;
            }
            String result = executor.executeToolRequestWithRetry(toolOpt.get(), toolRequest);
            log.info("Tool '{}' executed successfully", toolName);

            resultMessages.add(ToolExecutionResultMessage.from(toolRequest, result));
        }

        return resultMessages;
    }

    protected List<dev.langchain4j.data.message.ChatMessage> mapToLangChainMessages(List<ChatMessage> history) {
        List<dev.langchain4j.data.message.ChatMessage> chatMessageList = new ArrayList<>();
        for (ChatMessage msg : history) {
            switch (msg.getType()) {
                case USER -> chatMessageList.add(new UserMessage(msg.getMessage()));
                case ASSISTANT -> chatMessageList.add(new AiMessage(msg.getMessage()));
                case SYSTEM -> chatMessageList.add(new SystemMessage(msg.getMessage()));
                default -> {
                    // ignore
                }
            }
        }
        return chatMessageList;
    }

    @Retry
    @Fallback(fallbackMethod = "modelChatFallback")
    protected ChatResponse modelChatRequestWithRetries(ChatModel chatModel, ChatRequest chatRequest) {
        return chatModel.chat(chatRequest);
    }

    @SuppressWarnings("java:S1172")
    protected ChatResponse modelChatFallback(ChatModel chatModel, ChatRequest chatRequest) {
        log.error("Chat request failed after retries. Unable to get response from LLM model");
        return null;
    }
}
