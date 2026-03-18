package org.tkit.onecx.ai.provider.common.services.llm;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.tkit.onecx.ai.provider.common.config.DispatchConfig;
import org.tkit.onecx.ai.provider.common.exceptions.ChatException;
import org.tkit.onecx.ai.provider.common.models.*;
import org.tkit.onecx.ai.provider.common.services.mcp.McpService;
import org.tkit.onecx.ai.provider.common.services.mcp.McpTool;
import org.tkit.onecx.ai.provider.common.services.mcp.McpToolRegistry;
import org.tkit.onecx.ai.provider.domain.models.Configuration;
import org.tkit.onecx.ai.provider.domain.models.MCPServer;
import org.tkit.onecx.ai.provider.domain.models.Provider;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
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

    public abstract ChatResponse chat(Configuration configuration, Provider provider, List<MCPServer> mcpServers,
            ChatRequestModel chatRequest) throws ChatException;

    /**
     * Creates a tool registry from the MCP servers defined in the context.
     */
    protected McpToolRegistry createToolRegistry(List<MCPServer> mcpServers) {
        return mcpService.createToolRegistry(mcpServers);
    }

    /**
     * Checks if the LLM response contains tool execution requests.
     */
    protected boolean hasToolExecutionRequests(ChatResponse response) {
        AiMessage aiMessage = response.aiMessage();
        return aiMessage != null
                && aiMessage.hasToolExecutionRequests()
                && aiMessage.toolExecutionRequests() != null
                && !aiMessage.toolExecutionRequests().isEmpty();
    }

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
            String result = executeToolRequestWithRetry(toolOpt.get(), toolRequest);
            log.info("Tool '{}' executed successfully", toolName);

            resultMessages.add(ToolExecutionResultMessage.from(toolRequest, result));
        }

        return resultMessages;
    }

    @Retry
    @Fallback(fallbackMethod = "toolExecutionFallback")
    protected String executeToolRequestWithRetry(McpTool tool, ToolExecutionRequest toolRequest) {
        return tool.execute(toolRequest);
    }

    protected String toolExecutionFallback(McpTool tool, ToolExecutionRequest toolRequest) {
        log.error("Tool execution failed after {} retries for tool: {}", dispatchConfig.mcpConfig().maxToolExecutionRetries(),
                toolRequest.name());
        return "Error: Tool execution failed for '" + toolRequest.name() + "'";
    }

    protected List<dev.langchain4j.data.message.ChatMessage> mapToLangChainMessages(List<ChatMessage> history) {
        List<dev.langchain4j.data.message.ChatMessage> chatMessageList = new ArrayList<>();
        for (ChatMessage msg : history) {
            switch (msg.getType()) {
                case USER -> chatMessageList.add(new UserMessage(msg.getMessage()));
                case ASSISTANT -> chatMessageList.add(new AiMessage(msg.getMessage()));
                case SYSTEM -> chatMessageList.add(new SystemMessage(msg.getMessage()));
            }
        }
        return chatMessageList;
    }

    @Retry
    @Fallback(fallbackMethod = "modelChatFallback")
    protected ChatResponse modelChatRequestWithRetries(ChatModel chatModel, ChatRequest chatRequest) {
        return chatModel.chat(chatRequest);
    }

    protected ChatResponse modelChatFallback(ChatModel chatModel, ChatRequest chatRequest) {
        log.error("Chat request failed after retries. Unable to get response from LLM model");
        return null;
    }
}
