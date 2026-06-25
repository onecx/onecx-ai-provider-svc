package org.tkit.onecx.ai.provider.common.services.agentic.runtime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import dev.langchain4j.agentic.observability.AgentInvocationError;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.observability.AgentResponse;
import dev.langchain4j.agentic.scope.AgentInvocation;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
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
        return buildLocalAgent(agent, request, executionId, null, false, List.of());
    }

    public RuntimeAgent leadAgent(Agent agent, ChatRequestDTOV1 request, String executionId,
            List<RuntimeAgent> delegateAgents) {
        return buildLocalAgent(agent, request, executionId, null, false,
                delegateAgents != null ? delegateAgents : List.of());
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
            agents.add(buildLocalAgent(agent, request, parentExecutionId, groupId, true, List.of()));
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
            boolean childExecution, List<RuntimeAgent> delegateAgents) {
        ChatModel chatModel = chatModelFactory.createChatModel(agent);

        AtomicReference<String> activeExecutionId = new AtomicReference<>(childExecution ? null : executionIdOrParent);
        McpToolRegistry toolRegistry = mcpService.createToolRegistry(agent, activeExecutionId.get());
        Map<ToolSpecification, ToolExecutor> toolExecutors = toToolExecutors(toolRegistry, activeExecutionId);
        List<RuntimeAgent> delegates = delegateAgents != null ? delegateAgents : List.of();
        toolExecutors.putAll(toDelegateToolExecutors(delegates));

        var builder = AgenticServices.agentBuilder(TextAgent.class)
                .name(runtimeName(agent))
                .description(runtimeDescription(agent))
                .outputKey("response")
                .chatModel(chatModel)
                .systemMessageProvider(input -> systemMessage(agent, requestFromInput(input, request), delegates))
                .userMessageProvider(input -> userMessage(requestFromInput(input, request)))
                .maxSequentialToolsInvocations((int) dispatchConfig.mcpConfig().maxIterations());

        if (childExecution) {
            builder.listener(new ExecutionTrackingAgentListener(agent, groupId, request, executionIdOrParent,
                    activeExecutionId));
        }

        if (!toolExecutors.isEmpty()) {
            builder.tools(toolExecutors);
        }

        TextAgent agenticAgent = builder.build();
        UntypedAgent invoker = new TextAgentAdapter(agenticAgent);
        return new RuntimeAgent(runtimeName(agent), runtimeDescription(agent), agenticAgent, invoker,
                () -> closeAll(toolRegistry, delegates));
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
                .listener(new RemoteExecutionTrackingAgentListener(parentExecutionId, runtimeName(externalAgent)))
                .build();
        return new RuntimeAgent(runtimeName(externalAgent), runtimeDescription(externalAgent), a2aAgent, null);
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

    private Map<ToolSpecification, ToolExecutor> toDelegateToolExecutors(List<RuntimeAgent> delegateAgents) {
        if (delegateAgents == null || delegateAgents.isEmpty()) {
            return Map.of();
        }

        Map<String, Long> duplicateCounts = delegateAgents.stream()
                .map(agent -> delegateToolBaseName(agent.name()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        Map<String, Integer> seen = new LinkedHashMap<>();

        Map<ToolSpecification, ToolExecutor> executors = new LinkedHashMap<>();
        for (RuntimeAgent delegate : delegateAgents) {
            String baseName = delegateToolBaseName(delegate.name());
            int index = seen.merge(baseName, 1, Integer::sum);
            String toolName = duplicateCounts.getOrDefault(baseName, 0L) > 1 ? baseName + "_" + index : baseName;
            ToolSpecification specification = ToolSpecification.builder()
                    .name(toolName)
                    .description(delegateToolDescription(delegate))
                    .parameters(JsonObjectSchema.builder()
                            .addStringProperty("message",
                                    "A focused request for this agent. Include all context the agent needs.")
                            .required("message")
                            .additionalProperties(false)
                            .build())
                    .build();
            executors.put(specification, (request, memoryId) -> invokeDelegate(delegate));
        }
        return executors;
    }

    private String invokeDelegate(RuntimeAgent delegate) {
        UntypedAgent workflow = AgenticServices.sequenceBuilder()
                .name("delegate-agent-" + safeString(delegate.name()))
                .description("Runs a selected peer agent as an optional delegate")
                .subAgents(List.of(delegate.agent()))
                .output(this::outputFromScope)
                .build();
        Object result = workflow.invoke(Map.of());
        return result != null ? result.toString() : "";
    }

    private String outputFromScope(AgenticScope scope) {
        if (scope == null || scope.agentInvocations() == null) {
            return "";
        }
        List<AgentInvocation> invocations = scope.agentInvocations();
        if (invocations.isEmpty()) {
            return "";
        }
        Object result = invocations.getLast().output();
        return result != null ? result.toString() : "";
    }

    private String delegateToolBaseName(String name) {
        String normalized = safeString(name).trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return "delegate_" + (!isBlank(normalized) ? normalized : "agent");
    }

    private String delegateToolDescription(RuntimeAgent delegate) {
        return "Call agent '%s' only when the request clearly matches this agent's specialty. Specialty: %s"
                .formatted(safeString(delegate.name()), !isBlank(delegate.description())
                        ? delegate.description().trim()
                        : "configured peer agent");
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

    private String systemMessage(Agent agent, ChatRequestDTOV1 request, List<RuntimeAgent> delegateAgents) {
        String composed = scaffoldPromptComposer.compose(agent, request);
        String base = !isBlank(composed) ? composed : "You are a helpful assistant.";
        if (delegateAgents == null || delegateAgents.isEmpty()) {
            return base;
        }
        return base + System.lineSeparator() + System.lineSeparator() + delegationPolicy(delegateAgents);
    }

    private String delegationPolicy(List<RuntimeAgent> delegateAgents) {
        StringBuilder sb = new StringBuilder(
                """
                        Optional peer agents are available as tools.
                        Use a peer agent only when its specialty clearly matches the user's request.
                        Do not call a peer merely because it is available.
                        If you call a peer, use its result as private working context and return one final assistant message.
                        Do not expose tool names, agent names, transcripts, or intermediate routing details unless the user asks for them.
                        Available peer agents:""");
        for (RuntimeAgent delegate : delegateAgents) {
            sb.append(System.lineSeparator())
                    .append("- ")
                    .append(safeString(delegate.name()));
            if (!isBlank(delegate.description())) {
                sb.append(": ").append(delegate.description().trim());
            }
        }
        return sb.toString();
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

    private void closeAll(McpToolRegistry toolRegistry, List<RuntimeAgent> delegateAgents) {
        try {
            toolRegistry.close();
        } finally {
            if (delegateAgents != null) {
                delegateAgents.forEach(RuntimeAgent::close);
            }
        }
    }

    private String safeString(Object value) {
        return value == null ? "" : value.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private final class ExecutionTrackingAgentListener implements AgentListener {

        private final Agent agent;
        private final String groupId;
        private final ChatRequestDTOV1 request;
        private final String parentExecutionId;
        private final AtomicReference<String> activeExecutionId;
        private final AtomicReference<String> childExecutionId = new AtomicReference<>();

        private ExecutionTrackingAgentListener(Agent agent, String groupId, ChatRequestDTOV1 request,
                String parentExecutionId, AtomicReference<String> activeExecutionId) {
            this.agent = agent;
            this.groupId = groupId;
            this.request = request;
            this.parentExecutionId = parentExecutionId;
            this.activeExecutionId = activeExecutionId;
        }

        @Override
        public void beforeAgentInvocation(AgentRequest agentRequest) {
            transitionExecution(parentExecutionId, ExecutionState.WAITING_AGENT);
            Execution execution = executionService.createExecution(agent, groupId, extractRequestExcerpt(request));
            String executionId = execution.getExecutionId();
            childExecutionId.set(executionId);
            activeExecutionId.set(executionId);
            executionService.startExecution(executionId);
        }

        @Override
        public void afterAgentInvocation(AgentResponse agentResponse) {
            String executionId = childExecutionId.get();
            try {
                if (!isBlank(executionId)) {
                    executionService.succeedExecution(executionId,
                            agentResponse != null && agentResponse.output() != null ? agentResponse.output().toString() : "");
                }
                incrementParentAgentCount(parentExecutionId);
            } finally {
                childExecutionId.set(null);
                activeExecutionId.set(null);
                resumeExecution(parentExecutionId);
            }
        }

        @Override
        public void onAgentInvocationError(AgentInvocationError error) {
            String executionId = childExecutionId.get();
            Throwable cause = error != null ? error.error() : null;
            log.warn("Local agent '{}' invocation failed", runtimeName(agent), cause);
            try {
                if (!isBlank(executionId)) {
                    executionService.failExecution(executionId,
                            cause != null ? cause.getClass().getSimpleName() : "AgentInvocationError",
                            cause != null ? cause.getMessage() : null);
                }
            } finally {
                childExecutionId.set(null);
                activeExecutionId.set(null);
                resumeExecution(parentExecutionId);
            }
        }

        @Override
        public boolean inheritedBySubagents() {
            return false;
        }
    }

    private final class RemoteExecutionTrackingAgentListener implements AgentListener {

        private final String parentExecutionId;
        private final String name;

        private RemoteExecutionTrackingAgentListener(String parentExecutionId, String name) {
            this.parentExecutionId = parentExecutionId;
            this.name = name;
        }

        @Override
        public void beforeAgentInvocation(AgentRequest agentRequest) {
            transitionExecution(parentExecutionId, ExecutionState.WAITING_AGENT);
        }

        @Override
        public void afterAgentInvocation(AgentResponse agentResponse) {
            try {
                incrementParentAgentCount(parentExecutionId);
            } finally {
                resumeExecution(parentExecutionId);
            }
        }

        @Override
        public void onAgentInvocationError(AgentInvocationError error) {
            log.warn("Remote A2A agent '{}' invocation failed", name, error != null ? error.error() : null);
            resumeExecution(parentExecutionId);
        }

        @Override
        public boolean inheritedBySubagents() {
            return false;
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
