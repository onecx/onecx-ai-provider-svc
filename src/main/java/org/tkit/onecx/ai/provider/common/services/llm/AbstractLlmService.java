package org.tkit.onecx.ai.provider.common.services.llm;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.tkit.onecx.ai.provider.common.models.DispatchConfig;
import org.tkit.onecx.ai.provider.common.services.mcp.McpService;
import org.tkit.onecx.ai.provider.common.services.mcp.McpTool;
import org.tkit.onecx.ai.provider.common.services.mcp.McpToolRegistry;
import org.tkit.onecx.ai.provider.domain.models.Configuration;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatMessageDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public abstract class AbstractLlmService {

    @Inject
    McpService mcpService;

    @Inject
    DispatchConfig dispatchConfig;

    public abstract Response chat(Configuration configuration, ChatRequestDTOV1 chatRequestDTO);

    /**
     * Creates a tool registry from the MCP servers defined in the context.
     */
    protected McpToolRegistry createToolRegistry(Configuration aiConfiguration) {
        return mcpService.createToolRegistry(aiConfiguration);
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
    protected List<ChatMessage> executeToolRequests(ChatResponse response, McpToolRegistry toolRegistry) {
        List<ChatMessage> resultMessages = new ArrayList<>();

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

    protected List<ChatMessage> mapToLangChainMessages(List<ChatMessageDTOV1> history) {
        List<ChatMessage> chatMessageList = new ArrayList<>();
        for (ChatMessageDTOV1 msg : history) {
            switch (msg.getType()) {
                case USER -> chatMessageList.add(new UserMessage(msg.getMessage()));
                case ASSISTANT -> chatMessageList.add(new AiMessage(msg.getMessage()));
                case SYSTEM -> chatMessageList.add(new SystemMessage(msg.getMessage()));
            }
        }
        return chatMessageList;
    }

    protected ChatMessageDTOV1 mapToChatMessageResponseDTO(String responseMessage) {
        ChatMessageDTOV1 chatMessageDTOV1 = new ChatMessageDTOV1();
        chatMessageDTOV1.setMessage(responseMessage);
        chatMessageDTOV1.setType(ChatMessageDTOV1.TypeEnum.ASSISTANT);
        chatMessageDTOV1.setCreationDate(new Date().getTime());
        return chatMessageDTOV1;
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
