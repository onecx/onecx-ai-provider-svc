package org.tkit.onecx.ai.provider.common.services.agentic.runtime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.tkit.onecx.ai.provider.common.models.DispatchConfig;
import org.tkit.onecx.ai.provider.common.services.agentic.ScaffoldPromptComposer;
import org.tkit.onecx.ai.provider.common.services.agentic.a2a.AgentCard;
import org.tkit.onecx.ai.provider.common.services.agentic.a2a.ExternalAgentDiscoveryService;
import org.tkit.onecx.ai.provider.common.services.execution.ExecutionService;
import org.tkit.onecx.ai.provider.common.services.mcp.McpService;
import org.tkit.onecx.ai.provider.common.services.mcp.McpTool;
import org.tkit.onecx.ai.provider.common.services.mcp.McpToolRegistry;
import org.tkit.onecx.ai.provider.domain.daos.AgentDAO;
import org.tkit.onecx.ai.provider.domain.daos.ExternalAgentDAO;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.AgentGroup;
import org.tkit.onecx.ai.provider.domain.models.Execution;
import org.tkit.onecx.ai.provider.domain.models.ExternalAgent;
import org.tkit.onecx.ai.provider.domain.models.enums.AgentStatus;
import org.tkit.onecx.ai.provider.domain.models.enums.ExecutionState;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.tool.ToolExecutor;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatMessageDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class RuntimeAgentFactory {

    static final String INPUT_REQUEST = "request";

    @Inject
    AgentDAO agentDAO;

    @Inject
    ExternalAgentDAO externalAgentDAO;

    @Inject
    ChatModelFactory chatModelFactory;

    @Inject
    ScaffoldPromptComposer scaffoldPromptComposer;

    @Inject
    McpService mcpService;

    @Inject
    ExecutionService executionService;

    @Inject
    ExternalAgentDiscoveryService externalAgentDiscoveryService;

    @Inject
    DispatchConfig dispatchConfig;

    public RuntimeAgent rootAgent(Agent agent, ChatRequestDTOV1 request, String executionId) {
        return buildLocalAgent(agent, request, executionId, null, false);
    }

    public List<RuntimeAgent> agentsForGroup(Agent rootAgent, AgentGroup group, ChatRequestDTOV1 request,
            String parentExecutionId) {
        if (rootAgent == null || group == null || group.getId() == null) {
            return List.of();
        }

        String groupId = group.getId().toString();
        List<RuntimeAgent> agents = new ArrayList<>();
        for (Agent agent : agentDAO.findAgentsByGroupId(groupId)) {
            if (!isCallableLocalAgent(rootAgent, agent)) {
                continue;
            }
            agents.add(buildLocalAgent(agent, request, parentExecutionId, groupId, true));
        }

        List<ExternalAgent> externalAgents = externalAgentDAO.findExternalAgentsByGroupId(groupId);
        if (externalAgents == null) {
            externalAgents = List.of();
        }
        for (ExternalAgent externalAgent : externalAgents) {
            RuntimeAgent remoteAgent = buildRemoteAgent(externalAgent, parentExecutionId);
            if (remoteAgent != null) {
                agents.add(remoteAgent);
            }
        }

        agents.sort(Comparator.comparing(agent -> safeString(agent.name()).toLowerCase()));
        return agents;
    }

    private RuntimeAgent buildLocalAgent(Agent agent, ChatRequestDTOV1 request, String executionIdOrParent, String groupId,
            boolean childExecution) {
        ChatModel chatModel = chatModelFactory.createChatModel(agent);

        AtomicReference<String> activeExecutionId = new AtomicReference<>(childExecution ? null : executionIdOrParent);
        McpToolRegistry toolRegistry = mcpService.createToolRegistry(agent, activeExecutionId.get());
        Map<ToolSpecification, ToolExecutor> toolExecutors = toToolExecutors(toolRegistry, activeExecutionId);

        var builder = AgenticServices.agentBuilder(TextAgent.class)
                .name(runtimeName(agent))
                .description(runtimeDescription(agent))
                .outputKey("response")
                .chatModel(chatModel)
                .systemMessageProvider(input -> systemMessage(agent, requestFromInput(input, request)))
                .userMessageProvider(input -> userMessage(requestFromInput(input, request)))
                .maxSequentialToolsInvocations((int) dispatchConfig.mcpConfig().maxIterations());

        if (!toolExecutors.isEmpty()) {
            builder.tools(toolExecutors);
        }

        UntypedAgent agenticAgent = new TextAgentAdapter(builder.build());
        UntypedAgent trackedAgent = childExecution
                ? new LocalExecutionTrackingAgent(agenticAgent, agent, groupId, request, executionIdOrParent, activeExecutionId)
                : agenticAgent;
        return new RuntimeAgent(runtimeName(agent), runtimeDescription(agent), trackedAgent, toolRegistry::close);
    }

    private RuntimeAgent buildRemoteAgent(ExternalAgent externalAgent, String parentExecutionId) {
        if (!isCallableExternalAgent(externalAgent)) {
            return null;
        }
        if (!isBlank(externalAgent.getApiKey())) {
            log.warn("Skipping remote A2A agent '{}': authenticated A2A is not supported by the configured LangChain4j builder",
                    externalAgent.getName());
            return null;
        }

        AgentCard card = externalAgentDiscoveryService.fetchAgentCard(externalAgent.getDiscoveryUrl());
        if (card == null || !card.hasInvokeUrl()) {
            log.warn("Skipping remote A2A agent '{}': discovery failed or returned no invoke URL", externalAgent.getName());
            return null;
        }

        UntypedAgent a2aAgent = AgenticServices.a2aBuilder(card.url())
                .inputKeys(INPUT_REQUEST)
                .outputKey("response")
                .build();
        UntypedAgent trackedAgent = new RemoteExecutionTrackingAgent(a2aAgent, parentExecutionId, runtimeName(externalAgent));
        return new RuntimeAgent(runtimeName(externalAgent), runtimeDescription(externalAgent), trackedAgent, null);
    }

    private Map<ToolSpecification, ToolExecutor> toToolExecutors(McpToolRegistry toolRegistry,
            AtomicReference<String> activeExecutionId) {
        Map<ToolSpecification, ToolExecutor> executors = new LinkedHashMap<>();
        for (McpTool tool : toolRegistry.tools()) {
            executors.put(tool.toolSpecification(), (request, memoryId) -> {
                String executionId = activeExecutionId.get();
                transitionExecution(executionId, ExecutionState.WAITING_TOOL);
                try {
                    String result = tool.execute(request);
                    incrementToolCallCount(executionId);
                    return result;
                } finally {
                    resumeExecution(executionId);
                }
            });
        }
        return executors;
    }

    private boolean isCallableLocalAgent(Agent rootAgent, Agent candidate) {
        if (candidate == null || candidate.getId() == null || rootAgent == null || rootAgent.getId() == null) {
            return false;
        }
        if (Objects.equals(rootAgent.getId(), candidate.getId())) {
            return false;
        }
        return candidate.getStatus() == null || AgentStatus.LIVE.equals(candidate.getStatus());
    }

    private boolean isCallableExternalAgent(ExternalAgent externalAgent) {
        return externalAgent != null
                && Boolean.TRUE.equals(externalAgent.getEnabled())
                && !isBlank(externalAgent.getDiscoveryUrl());
    }

    private String userMessage(ChatRequestDTOV1 request) {
        StringBuilder message = new StringBuilder();
        if (request != null && request.getConversation() != null && request.getConversation().getHistory() != null
                && !request.getConversation().getHistory().isEmpty()) {
            message.append("Conversation history:")
                    .append(System.lineSeparator())
                    .append(formatHistory(request.getConversation().getHistory()))
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());
        }
        message.append(extractUserMessage(request));
        return message.toString();
    }

    private String systemMessage(Agent agent, ChatRequestDTOV1 request) {
        String composed = scaffoldPromptComposer.compose(agent, request);
        return !isBlank(composed) ? composed : "You are a helpful assistant.";
    }

    private String formatHistory(List<ChatMessageDTOV1> history) {
        return history.stream()
                .map(message -> safeString(message.getType()) + ": " + safeString(message.getMessage()))
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("");
    }

    @SuppressWarnings("unchecked")
    private ChatRequestDTOV1 requestFromInput(Object input, ChatRequestDTOV1 fallback) {
        if (input instanceof Map<?, ?> map) {
            Object request = ((Map<String, Object>) map).get(INPUT_REQUEST);
            if (request instanceof ChatRequestDTOV1 chatRequest) {
                return chatRequest;
            }
        }
        return fallback;
    }

    private String extractRequestExcerpt(ChatRequestDTOV1 request) {
        String message = extractUserMessage(request);
        if (isBlank(message)) {
            return null;
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    private String extractUserMessage(ChatRequestDTOV1 request) {
        if (request == null || request.getChatMessage() == null || request.getChatMessage().getMessage() == null) {
            return "";
        }
        return request.getChatMessage().getMessage();
    }

    private String runtimeName(Agent agent) {
        return !isBlank(agent.getName()) ? agent.getName() : "local-agent";
    }

    private String runtimeDescription(Agent agent) {
        return !isBlank(agent.getDescription()) ? agent.getDescription() : "Configured local agent";
    }

    private String runtimeName(ExternalAgent agent) {
        return !isBlank(agent.getName()) ? agent.getName() : "remote-agent";
    }

    private String runtimeDescription(ExternalAgent agent) {
        return !isBlank(agent.getDescription()) ? agent.getDescription() : "Discovered remote A2A agent";
    }

    private void transitionExecution(String executionId, ExecutionState state) {
        if (isBlank(executionId)) {
            return;
        }
        try {
            executionService.waitForResource(executionId, state);
        } catch (Exception ex) {
            log.warn("Unable to transition execution {} to {}", executionId, state, ex);
        }
    }

    private void resumeExecution(String executionId) {
        if (isBlank(executionId)) {
            return;
        }
        try {
            executionService.resumeExecution(executionId);
        } catch (Exception ex) {
            log.warn("Unable to resume execution {}", executionId, ex);
        }
    }

    private void incrementToolCallCount(String executionId) {
        if (isBlank(executionId)) {
            return;
        }
        try {
            executionService.incrementToolCallCount(executionId);
        } catch (Exception ex) {
            log.warn("Unable to increment tool call count for execution {}", executionId, ex);
        }
    }

    private void incrementParentAgentCount(String parentExecutionId) {
        if (isBlank(parentExecutionId)) {
            return;
        }
        try {
            executionService.incrementAgentCallCount(parentExecutionId);
        } catch (Exception ex) {
            log.warn("Unable to increment agent call count for execution {}", parentExecutionId, ex);
        }
    }

    private String safeString(Object value) {
        return value == null ? "" : value.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private final class LocalExecutionTrackingAgent implements UntypedAgent {

        private final UntypedAgent delegate;
        private final Agent agent;
        private final String groupId;
        private final ChatRequestDTOV1 request;
        private final String parentExecutionId;
        private final AtomicReference<String> activeExecutionId;

        private LocalExecutionTrackingAgent(UntypedAgent delegate, Agent agent, String groupId, ChatRequestDTOV1 request,
                String parentExecutionId, AtomicReference<String> activeExecutionId) {
            this.delegate = delegate;
            this.agent = agent;
            this.groupId = groupId;
            this.request = request;
            this.parentExecutionId = parentExecutionId;
            this.activeExecutionId = activeExecutionId;
        }

        @Override
        public Object invoke(Map<String, Object> input) {
            return invokeWithAgenticScope(input).result();
        }

        @Override
        public ResultWithAgenticScope<String> invokeWithAgenticScope(Map<String, Object> input) {
            transitionExecution(parentExecutionId, ExecutionState.WAITING_AGENT);
            Execution execution = executionService.createExecution(agent, groupId, extractRequestExcerpt(request));
            String executionId = execution.getExecutionId();
            activeExecutionId.set(executionId);
            try {
                executionService.startExecution(executionId);
                ResultWithAgenticScope<String> result = delegate.invokeWithAgenticScope(input);
                executionService.succeedExecution(executionId, result.result());
                incrementParentAgentCount(parentExecutionId);
                return result;
            } catch (Exception ex) {
                log.warn("Local agent '{}' invocation failed", runtimeName(agent), ex);
                try {
                    executionService.failExecution(executionId, ex.getClass().getSimpleName(), ex.getMessage());
                } catch (Exception ignored) {
                    // Execution state may already be terminal.
                }
                throw ex;
            } finally {
                activeExecutionId.set(null);
                resumeExecution(parentExecutionId);
            }
        }

        @Override
        public AgenticScope getAgenticScope(Object memoryId) {
            return delegate.getAgenticScope(memoryId);
        }

        @Override
        public boolean evictAgenticScope(Object memoryId) {
            return delegate.evictAgenticScope(memoryId);
        }
    }

    private final class RemoteExecutionTrackingAgent implements UntypedAgent {

        private final UntypedAgent delegate;
        private final String parentExecutionId;
        private final String name;

        private RemoteExecutionTrackingAgent(UntypedAgent delegate, String parentExecutionId, String name) {
            this.delegate = delegate;
            this.parentExecutionId = parentExecutionId;
            this.name = name;
        }

        @Override
        public Object invoke(Map<String, Object> input) {
            return invokeWithAgenticScope(input).result();
        }

        @Override
        public ResultWithAgenticScope<String> invokeWithAgenticScope(Map<String, Object> input) {
            transitionExecution(parentExecutionId, ExecutionState.WAITING_AGENT);
            try {
                ResultWithAgenticScope<String> result = delegate.invokeWithAgenticScope(input);
                incrementParentAgentCount(parentExecutionId);
                return result;
            } catch (Exception ex) {
                log.warn("Remote A2A agent '{}' invocation failed", name, ex);
                throw ex;
            } finally {
                resumeExecution(parentExecutionId);
            }
        }

        @Override
        public AgenticScope getAgenticScope(Object memoryId) {
            return delegate.getAgenticScope(memoryId);
        }

        @Override
        public boolean evictAgenticScope(Object memoryId) {
            return delegate.evictAgenticScope(memoryId);
        }
    }

    private static final class TextAgentAdapter implements UntypedAgent {

        private final TextAgent delegate;

        private TextAgentAdapter(TextAgent delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Map<String, Object> input) {
            return delegate.invoke();
        }

        @Override
        public ResultWithAgenticScope<String> invokeWithAgenticScope(Map<String, Object> input) {
            return new ResultWithAgenticScope<>(null, delegate.invoke());
        }

        @Override
        public AgenticScope getAgenticScope(Object memoryId) {
            return null;
        }

        @Override
        public boolean evictAgenticScope(Object memoryId) {
            return false;
        }
    }
}
