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
import org.tkit.onecx.ai.provider.common.services.agentic.tool.ToolPolicyService;
import org.tkit.onecx.ai.provider.common.services.execution.ExecutionService;
import org.tkit.onecx.ai.provider.common.services.mcp.McpService;
import org.tkit.onecx.ai.provider.common.services.mcp.McpTool;
import org.tkit.onecx.ai.provider.common.services.mcp.McpToolRegistry;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ExecutionState;

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

    @Inject
    ToolPolicyService toolPolicyService;

    @Inject
    ExecutionService executionService;

    protected static final String HEALTHY = "HEALTHY";
    protected static final String UNHEALTHY = "UNHEALTHY";
    protected static final String HEALTH_CHECK_PROMPT = "ping";

    public abstract Response chat(Agent agent, ChatRequestDTOV1 chatRequestDTO, String executionId);

    /**
     * Creates a tool registry from the tools defined in the agent.
     */
    protected McpToolRegistry createToolRegistry(Agent agent, String executionId) {
        return mcpService.createToolRegistry(agent, executionId);
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
    protected List<ChatMessage> executeToolRequests(Agent agent, String executionId, ChatResponse response,
            McpToolRegistry toolRegistry) {
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
            McpTool tool = toolOpt.get();

            if (toolPolicyService != null && tool.toolId() != null
                    && !toolPolicyService.isToolAllowed(agent, tool.toolId())) {
                log.warn("Tool '{}' denied by policy for agent '{}'", toolName, agent != null ? agent.getName() : null);
                resultMessages.add(ToolExecutionResultMessage.from(
                        toolRequest,
                        "Error: Tool '" + toolName + "' is not allowed for this agent"));
                continue;
            }

            transitionToWaitingTool(executionId);
            String result = executeToolRequestWithRetry(toolOpt.get(), toolRequest);
            transitionToRunning(executionId);
            incrementToolCallCount(executionId);
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

    protected void transitionToWaitingTool(String executionId) {
        if (executionService == null || executionId == null || executionId.isBlank()) {
            return;
        }
        try {
            executionService.waitForResource(executionId, ExecutionState.WAITING_TOOL);
        } catch (Exception e) {
            log.warn("Unable to set execution {} to WAITING_TOOL: {}", executionId, e.getMessage());
        }
    }

    protected void transitionToRunning(String executionId) {
        if (executionService == null || executionId == null || executionId.isBlank()) {
            return;
        }
        try {
            executionService.resumeExecution(executionId);
        } catch (Exception e) {
            log.warn("Unable to set execution {} to RUNNING: {}", executionId, e.getMessage());
        }
    }

    protected void incrementToolCallCount(String executionId) {
        if (executionService == null || executionId == null || executionId.isBlank()) {
            return;
        }
        try {
            executionService.incrementToolCallCount(executionId);
        } catch (Exception e) {
            log.warn("Unable to increment tool call count for execution {}: {}", executionId, e.getMessage());
        }
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

    protected ChatResponse modelChatRequest(ChatModel chatModel, ChatRequest chatRequest) {
        log.info("CHATREQUEST: {}", chatRequest.toString());
        try {
            return chatModel.chat(chatRequest);
        } catch (Exception e) {
            log.error("Error during chat request: {}", e.getMessage(), e);
            throw e;
        }
    }

    public abstract String getHealthStatus(Provider provider);
}
