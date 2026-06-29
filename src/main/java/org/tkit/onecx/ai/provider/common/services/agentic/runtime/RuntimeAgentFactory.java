package org.tkit.onecx.ai.provider.common.services.agentic.runtime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.internal.AgentExecutor;
import dev.langchain4j.agentic.internal.AgentInvoker;
import dev.langchain4j.agentic.internal.AgentSpecsProvider;
import dev.langchain4j.agentic.observability.AgentInvocationError;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.observability.AgentResponse;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
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

    @Inject
    ObjectMapper objectMapper;

    public RuntimeAgent rootAgent(Agent agent, ChatRequestDTOV1 request, String executionId) {
        return buildLocalAgent(agent, request, executionId, null, false, List.of());
    }

    public RuntimeAgent leadAgent(Agent agent, ChatRequestDTOV1 request, String executionId,
            List<RuntimeAgentDelegate> delegateAgents) {
        List<RuntimeAgentDelegate> delegates = delegateAgents != null ? delegateAgents : List.of();
        List<RuntimeAgentDelegate> strongMatches = strongMatchingDelegates(request, delegates);
        return buildLocalAgent(agent, request, executionId, null, false, delegates, strongMatches);
    }

    public ChatModel chatModel(Agent agent) {
        return chatModelFactory.createChatModel(agent);
    }

    public List<RuntimeAgent> agentsForGroup(Agent rootAgent, AgentGroup group, ChatRequestDTOV1 request,
            String parentExecutionId) {
        return delegatesForGroup(rootAgent, group, request, parentExecutionId).stream()
                .map(RuntimeAgentDelegate::open)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<RuntimeAgent> supervisorCandidatesForGroup(Agent rootAgent, AgentGroup group, ChatRequestDTOV1 request,
            String parentExecutionId) {
        if (rootAgent == null) {
            return List.of();
        }
        List<RuntimeAgent> candidates = new ArrayList<>();
        candidates.add(lazySupervisorCandidate(runtimeName(rootAgent), runtimeDescription(rootAgent),
                () -> rootAgent(rootAgent, request, parentExecutionId), parentExecutionId,
                group != null ? group.getId() : null, extractUserMessage(request)));
        for (RuntimeAgentDelegate delegate : delegatesForGroup(rootAgent, group, request, parentExecutionId)) {
            candidates.add(lazySupervisorCandidate(delegate.name(), delegate.description(), delegate::open, parentExecutionId,
                    group != null ? group.getId() : null, extractUserMessage(request)));
        }
        candidates.sort(Comparator.comparing(agent -> safeString(agent.name()).toLowerCase()));
        return candidates;
    }

    public List<RuntimeAgentDelegate> delegatesForGroup(Agent rootAgent, AgentGroup group, ChatRequestDTOV1 request,
            String parentExecutionId) {
        if (rootAgent == null || group == null || group.getId() == null) {
            return List.of();
        }

        String groupId = group.getId().toString();
        List<RuntimeAgentDelegate> agents = new ArrayList<>();
        for (Agent agent : agentDAO.findAgentsByGroupId(groupId)) {
            if (!isCallableLocalAgent(rootAgent, agent)) {
                continue;
            }
            agents.add(new RuntimeAgentDelegate(
                    runtimeName(agent),
                    runtimeDescription(agent),
                    () -> buildLocalAgent(agent, request, parentExecutionId, groupId, true, List.of())));
        }

        List<ExternalAgent> externalAgents = externalAgentDAO.findExternalAgentsByGroupId(groupId);
        if (externalAgents == null) {
            externalAgents = List.of();
        }
        for (ExternalAgent externalAgent : externalAgents) {
            if (isCallableExternalAgent(externalAgent)) {
                agents.add(new RuntimeAgentDelegate(
                        runtimeName(externalAgent),
                        runtimeDescription(externalAgent),
                        () -> buildRemoteAgent(externalAgent, parentExecutionId)));
            }
        }

        agents.sort(Comparator.comparing(agent -> safeString(agent.name()).toLowerCase()));
        return agents;
    }

    private RuntimeAgent buildLocalAgent(Agent agent, ChatRequestDTOV1 request, String executionIdOrParent, String groupId,
            boolean childExecution, List<RuntimeAgentDelegate> delegateAgents) {
        return buildLocalAgent(agent, request, executionIdOrParent, groupId, childExecution, delegateAgents, List.of());
    }

    private RuntimeAgent buildLocalAgent(Agent agent, ChatRequestDTOV1 request, String executionIdOrParent, String groupId,
            boolean childExecution, List<RuntimeAgentDelegate> delegateAgents,
            List<RuntimeAgentDelegate> requiredDelegates) {
        log.info(
                "Building runtime agent: agent={}, executionIdOrParent={}, groupId={}, childExecution={}, delegateTools={}, requiredDelegates={}",
                runtimeName(agent), executionIdOrParent, groupId, childExecution,
                delegateAgents != null ? delegateAgents.size() : 0,
                requiredDelegates != null ? requiredDelegates.size() : 0);
        ChatModel chatModel = chatModelFactory.createChatModel(agent);

        AtomicReference<String> activeExecutionId = new AtomicReference<>(childExecution ? null : executionIdOrParent);
        McpToolRegistry toolRegistry = mcpService.createToolRegistry(agent, activeExecutionId.get());
        Map<ToolSpecification, ToolExecutor> toolExecutors = toToolExecutors(toolRegistry, activeExecutionId);
        List<RuntimeAgentDelegate> delegates = delegateAgents != null ? delegateAgents : List.of();
        toolExecutors.putAll(toDelegateToolExecutors(delegates));
        Set<String> toolNames = toolExecutors.keySet().stream()
                .map(ToolSpecification::name)
                .collect(Collectors.toSet());
        ChatModel effectiveChatModel = toolNames.isEmpty()
                ? chatModel
                : new TextToolCallNormalizingChatModel(chatModel, toolNames);

        var builder = AiServices.builder(LocalChatAgent.class)
                .chatModel(effectiveChatModel)
                .systemMessage(systemMessage(agent, request, delegates, requiredDelegates))
                .userMessageProvider(input -> userMessage(request, inputMessage(input, extractUserMessage(request))))
                .maxSequentialToolsInvocations(maxSequentialToolInvocations(agent));

        if (!toolExecutors.isEmpty()) {
            builder.tools(toolExecutors);
        }

        LocalChatAgent chatAgent = builder.build();
        AgentListener listener = childExecution
                ? new ExecutionTrackingAgentListener(agent, groupId, request, executionIdOrParent, activeExecutionId)
                : null;
        LocalAgenticAction action = new LocalAgenticAction(runtimeName(agent), runtimeDescription(agent), chatAgent,
                listener);
        AgentExecutor agentExecutor = action.toAgentExecutor();
        return new RuntimeAgent(runtimeName(agent), runtimeDescription(agent), agentExecutor,
                new AgenticWorkflowInvocationAdapter(runtimeName(agent), agentExecutor),
                () -> closeAll(toolRegistry, delegates));
    }

    private RuntimeAgent lazySupervisorCandidate(String name, String description, Supplier<RuntimeAgent> supplier,
            String parentExecutionId, Object groupId, String fallbackMessage) {
        LazySupervisorAgenticAction action = new LazySupervisorAgenticAction(name, description, supplier,
                parentExecutionId, groupId != null ? groupId.toString() : null, fallbackMessage);
        AgentExecutor agentExecutor = action.toAgentExecutor();
        return new RuntimeAgent(name, description, agentExecutor,
                new AgenticWorkflowInvocationAdapter(name, agentExecutor), null);
    }

    private int maxSequentialToolInvocations(Agent agent) {
        long configured = dispatchConfig != null && dispatchConfig.mcpConfig() != null
                ? dispatchConfig.mcpConfig().maxIterations()
                : 3;
        if (configured < 1) {
            log.warn("Invalid MCP max-iterations={} for agent '{}'; using 1", configured, runtimeName(agent));
            return 1;
        }
        if (configured > Integer.MAX_VALUE) {
            log.warn("MCP max-iterations={} for agent '{}' exceeds supported range; using {}", configured,
                    runtimeName(agent), Integer.MAX_VALUE);
            return Integer.MAX_VALUE;
        }
        return (int) configured;
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
                .inputKeys("message")
                .outputKey("response")
                .listener(new RemoteExecutionTrackingAgentListener(parentExecutionId, runtimeName(externalAgent)))
                .build();
        return new RuntimeAgent(runtimeName(externalAgent), runtimeDescription(externalAgent), a2aAgent,
                new AgenticWorkflowInvocationAdapter(runtimeName(externalAgent), a2aAgent), null);
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

    private Map<ToolSpecification, ToolExecutor> toDelegateToolExecutors(List<RuntimeAgentDelegate> delegateAgents) {
        if (delegateAgents == null || delegateAgents.isEmpty()) {
            return Map.of();
        }

        Map<String, Long> duplicateCounts = delegateAgents.stream()
                .map(agent -> delegateToolBaseName(agent.name()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        Map<String, Integer> seen = new LinkedHashMap<>();

        Map<ToolSpecification, ToolExecutor> executors = new LinkedHashMap<>();
        for (RuntimeAgentDelegate delegate : delegateAgents) {
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
            executors.put(specification,
                    (request, memoryId) -> invokeDelegate(delegate, extractToolMessage(request.arguments())));
        }
        return executors;
    }

    private List<RuntimeAgentDelegate> strongMatchingDelegates(ChatRequestDTOV1 request,
            List<RuntimeAgentDelegate> delegateAgents) {
        if (delegateAgents == null || delegateAgents.isEmpty()) {
            return List.of();
        }
        String normalizedMessage = normalizeForRouting(extractUserMessage(request));
        if (isBlank(normalizedMessage)) {
            return List.of();
        }
        return delegateAgents.stream()
                .filter(delegate -> stronglyMatchesDelegate(normalizedMessage, delegate))
                .sorted(Comparator.comparing(delegate -> safeString(delegate.name()).toLowerCase()))
                .toList();
    }

    private boolean stronglyMatchesDelegate(String normalizedMessage, RuntimeAgentDelegate delegate) {
        for (String token : routingTokens(delegate.name())) {
            if (containsRoutingToken(normalizedMessage, token)) {
                return true;
            }
        }
        for (String token : routingTokens(delegate.description())) {
            if (containsRoutingToken(normalizedMessage, token)) {
                return true;
            }
        }
        return false;
    }

    private List<String> routingTokens(String value) {
        if (isBlank(value)) {
            return List.of();
        }
        return java.util.Arrays.stream(normalizeForRouting(value).split(" "))
                .filter(token -> token.length() >= 3)
                .filter(token -> !GENERIC_ROUTING_TOKENS.contains(token))
                .distinct()
                .toList();
    }

    private boolean containsRoutingToken(String normalizedMessage, String token) {
        return (" " + normalizedMessage + " ").contains(" " + token + " ");
    }

    private List<ToolExecutionRequest> extractTextToolCalls(String text, Set<String> availableToolNames) {
        if (isBlank(text) || availableToolNames == null || availableToolNames.isEmpty()) {
            return List.of();
        }
        List<ToolExecutionRequest> requests = new ArrayList<>();
        for (String candidate : jsonCandidates(text)) {
            JsonNode root;
            try {
                root = objectMapper.readTree(candidate);
            } catch (Exception ex) {
                continue;
            }
            if (root.isArray()) {
                for (JsonNode item : root) {
                    addTextToolCall(item, availableToolNames, requests);
                }
            } else {
                addTextToolCall(root, availableToolNames, requests);
            }
            if (!requests.isEmpty()) {
                return requests;
            }
        }
        return List.of();
    }

    private List<String> jsonCandidates(String text) {
        List<String> candidates = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current == '[' || current == '{') {
                String candidate = balancedJsonAt(text, i);
                if (candidate != null) {
                    candidates.add(candidate);
                }
            }
        }
        return candidates;
    }

    private String balancedJsonAt(String text, int start) {
        char open = text.charAt(start);
        char close = open == '[' ? ']' : '}';
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char current = text.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\' && inString) {
                escaped = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == open) {
                depth++;
            } else if (current == close) {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    private void addTextToolCall(JsonNode item, Set<String> availableToolNames, List<ToolExecutionRequest> requests) {
        if (item == null || !item.isObject()) {
            return;
        }
        String name = textField(item, "name");
        if (isBlank(name)) {
            name = textField(item, "tool");
        }
        if (isBlank(name)) {
            name = textField(item, "tool_name");
        }
        if (!availableToolNames.contains(name)) {
            return;
        }
        String arguments = toolArguments(item);
        requests.add(ToolExecutionRequest.builder()
                .id("text-tool-call-" + (requests.size() + 1))
                .name(name)
                .arguments(arguments)
                .build());
    }

    private String textField(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private String toolArguments(JsonNode item) {
        JsonNode arguments = item.get("arguments");
        if (arguments == null) {
            arguments = item.get("args");
        }
        if (arguments == null || arguments.isNull()) {
            return "{}";
        }
        if (arguments.isTextual()) {
            String value = arguments.asText();
            return !isBlank(value) ? value : "{}";
        }
        try {
            return objectMapper.writeValueAsString(arguments);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private String invokeDelegate(RuntimeAgentDelegate delegate, String message) {
        try (RuntimeAgent runtimeAgent = delegate.open()) {
            if (runtimeAgent == null) {
                return "";
            }
            return invokeDelegate(runtimeAgent, message);
        } catch (Exception ex) {
            Throwable rootCause = rootCause(ex);
            log.warn("Delegate agent '{}' failed: {}: {}", delegate.name(), rootCause.getClass().getSimpleName(),
                    rootCause.getMessage());
            log.debug("Delegate agent '{}' failure details", delegate.name(), ex);
            return "The peer agent '%s' could not complete the delegated request. Continue with the available information."
                    .formatted(safeString(delegate.name()));
        }
    }

    private String invokeDelegate(RuntimeAgent delegate, String message) {
        Object result = delegate.invoker()
                .invokeWithAgenticScope(Map.of("message", safeString(message)))
                .result();
        return result != null ? result.toString() : "";
    }

    private String delegateToolBaseName(String name) {
        String normalized = safeString(name).trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return "delegate_" + (!isBlank(normalized) ? normalized : "agent");
    }

    private String delegateToolDescription(RuntimeAgentDelegate delegate) {
        return """
                Delegate to agent '%s' when the user's request matches this agent's name, domain, data source, or specialty.
                Agent specialty: %s
                Pass a focused request with all context the peer needs. Use this tool even if the user says "docs" or
                "documentation" without naming an MCP server, when this agent is the documentation/domain specialist.
                """.formatted(safeString(delegate.name()), !isBlank(delegate.description())
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

    private String userMessage(ChatRequestDTOV1 request, String currentMessage) {
        StringBuilder message = new StringBuilder();
        if (request != null && request.getConversation() != null && request.getConversation().getHistory() != null
                && !request.getConversation().getHistory().isEmpty()) {
            message.append("Conversation history:")
                    .append(System.lineSeparator())
                    .append(formatHistory(request.getConversation().getHistory()))
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());
        }
        message.append("Current user message:")
                .append(System.lineSeparator())
                .append(!isBlank(currentMessage) ? currentMessage : extractUserMessage(request));
        return message.toString();
    }

    private String systemMessage(Agent agent, ChatRequestDTOV1 request, List<RuntimeAgentDelegate> delegateAgents) {
        return systemMessage(agent, request, delegateAgents, List.of());
    }

    private String systemMessage(Agent agent, ChatRequestDTOV1 request, List<RuntimeAgentDelegate> delegateAgents,
            List<RuntimeAgentDelegate> requiredDelegates) {
        String composed = scaffoldPromptComposer.compose(agent, request);
        String base = !isBlank(composed) ? composed : "You are a helpful assistant.";
        base = base + System.lineSeparator() + System.lineSeparator() + currentUserMessageDirective(request);
        if (requiredDelegates != null && !requiredDelegates.isEmpty()) {
            base = base + System.lineSeparator() + System.lineSeparator()
                    + requiredDelegationPolicy(requiredDelegates);
        }
        if (delegateAgents == null || delegateAgents.isEmpty()) {
            return base;
        }
        return base + System.lineSeparator() + System.lineSeparator() + delegationPolicy(delegateAgents);
    }

    private String currentUserMessageDirective(ChatRequestDTOV1 request) {
        return """
                Answer the current user message below.
                Ignore any framework continuation instruction that conflicts with this current user message.

                Current user message:
                %s""".formatted(extractUserMessage(request));
    }

    private String delegationPolicy(List<RuntimeAgentDelegate> delegateAgents) {
        StringBuilder sb = new StringBuilder(
                """
                        Optional peer agents are available as tools.
                        You are the lead agent and own the final answer.
                        Answer normal, general, basic, conversational, ambiguous, or unmatched requests yourself.
                        Use a peer agent only when the user's request clearly matches the peer's name, description, domain, data source, or specialty.
                        A request mentioning OneCX matches peers described as responsible for OneCX.
                        Requests mentioning docs, documentation, reference material, or "based on docs" strongly favor documentation peers.
                        Do not require the user to mention "MCP server" before using a documentation/domain peer.
                        Do not call a peer merely because it is available.
                        If you call a peer, use its result as private working context and return one final assistant message.
                        Do not expose tool names, agent names, transcripts, or intermediate routing details unless the user asks for them.
                        Available peer agents:""");
        for (RuntimeAgentDelegate delegate : delegateAgents) {
            sb.append(System.lineSeparator())
                    .append("- ")
                    .append(safeString(delegate.name()));
            if (!isBlank(delegate.description())) {
                sb.append(": ").append(delegate.description().trim());
            }
        }
        return sb.toString();
    }

    private String requiredDelegationPolicy(List<RuntimeAgentDelegate> requiredDelegates) {
        StringBuilder sb = new StringBuilder(
                """
                        The current user request strongly matches these peer agents by configured name, description, domain, or specialty.
                        You must delegate to these matching peer agents before producing the final answer.
                        Use their results as private working context and then return one final assistant message.
                        Do not expose agent names, tool names, routing details, or intermediate transcripts unless the user asks for them.
                        Required peer agents:""");
        for (RuntimeAgentDelegate delegate : requiredDelegates) {
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

    @SuppressWarnings("unchecked")
    private String inputMessage(Object input, String fallback) {
        if (input instanceof Map<?, ?> map) {
            Object message = ((Map<String, Object>) map).get("message");
            if (message != null && !isBlank(message.toString())) {
                return message.toString();
            }
        }
        return fallback;
    }

    private String extractToolMessage(String arguments) {
        if (isBlank(arguments)) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(arguments);
            JsonNode message = root.get("message");
            return message != null && !message.isNull() ? message.asText() : arguments;
        } catch (Exception ex) {
            log.debug("Unable to parse delegate tool arguments as JSON", ex);
            return arguments;
        }
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
            log.warn("Unable to transition execution {} to {}: {}: {}", executionId, state, ex.getClass().getSimpleName(),
                    ex.getMessage());
            log.debug("Execution transition failure details for {}", executionId, ex);
        }
    }

    private void resumeExecution(String executionId) {
        if (isBlank(executionId)) {
            return;
        }
        try {
            executionService.resumeExecution(executionId);
        } catch (Exception ex) {
            log.warn("Unable to resume execution {}: {}: {}", executionId, ex.getClass().getSimpleName(),
                    ex.getMessage());
            log.debug("Execution resume failure details for {}", executionId, ex);
        }
    }

    private void incrementToolCallCount(String executionId) {
        if (isBlank(executionId)) {
            return;
        }
        try {
            executionService.incrementToolCallCount(executionId);
        } catch (Exception ex) {
            log.warn("Unable to increment tool call count for execution {}: {}: {}", executionId,
                    ex.getClass().getSimpleName(), ex.getMessage());
            log.debug("Execution tool-call count failure details for {}", executionId, ex);
        }
    }

    private void incrementParentAgentCount(String parentExecutionId) {
        if (isBlank(parentExecutionId)) {
            return;
        }
        try {
            executionService.incrementAgentCallCount(parentExecutionId);
        } catch (Exception ex) {
            log.warn("Unable to increment agent call count for execution {}: {}: {}", parentExecutionId,
                    ex.getClass().getSimpleName(), ex.getMessage());
            log.debug("Execution agent-call count failure details for {}", parentExecutionId, ex);
        }
    }

    private void closeAll(McpToolRegistry toolRegistry, List<RuntimeAgentDelegate> delegateAgents) {
        try {
            toolRegistry.close();
        } finally {
            // Lazy delegates are opened and closed inside their tool executor.
        }
    }

    private String safeString(Object value) {
        return value == null ? "" : value.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizeForRouting(String value) {
        return safeString(value).toLowerCase()
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String errorType(Throwable cause) {
        return cause != null ? cause.getClass().getSimpleName() : "AgentInvocationError";
    }

    private String errorMessage(Throwable cause) {
        return cause != null ? cause.getMessage() : null;
    }

    private long durationSince(Long startedAt) {
        return startedAt != null ? System.currentTimeMillis() - startedAt : -1;
    }

    private Throwable rootCause(Throwable throwable) {
        if (throwable == null) {
            return new RuntimeException("unknown failure");
        }
        Throwable result = throwable;
        while (result.getCause() != null && result.getCause() != result) {
            result = result.getCause();
        }
        return result;
    }

    private static final Set<String> GENERIC_ROUTING_TOKENS = Set.of(
            "agent", "assistant", "bot", "peer", "local", "remote", "configured", "general", "default",
            "documentation", "docs", "doc", "expert", "specialist", "answer", "answers", "question", "questions",
            "related", "responsible", "domain", "data", "source");

    private final class ExecutionTrackingAgentListener implements AgentListener {

        private final Agent agent;
        private final String groupId;
        private final ChatRequestDTOV1 request;
        private final String parentExecutionId;
        private final AtomicReference<String> activeExecutionId;
        private final AtomicReference<String> childExecutionId = new AtomicReference<>();
        private final AtomicReference<Long> invocationStartedAt = new AtomicReference<>();

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
            invocationStartedAt.set(System.currentTimeMillis());
            log.info("Invoking agent: kind=local-delegate, executionId={}, parentExecutionId={}, groupId={}, agent={}",
                    executionId, parentExecutionId, groupId, runtimeName(agent));
            executionService.startExecution(executionId);
        }

        @Override
        public void afterAgentInvocation(AgentResponse agentResponse) {
            String executionId = childExecutionId.get();
            try {
                String output = agentResponse != null && agentResponse.output() != null ? agentResponse.output().toString()
                        : "";
                if (!isBlank(executionId)) {
                    executionService.succeedExecution(executionId, output);
                }
                log.info(
                        "Completed agent invocation: kind=local-delegate, executionId={}, parentExecutionId={}, groupId={}, agent={}, status=SUCCEEDED, durationMs={}, resultPresent={}",
                        executionId, parentExecutionId, groupId, runtimeName(agent), durationSince(invocationStartedAt.get()),
                        !isBlank(output));
                incrementParentAgentCount(parentExecutionId);
            } finally {
                childExecutionId.set(null);
                activeExecutionId.set(null);
                invocationStartedAt.set(null);
                resumeExecution(parentExecutionId);
            }
        }

        @Override
        public void onAgentInvocationError(AgentInvocationError error) {
            String executionId = childExecutionId.get();
            Throwable cause = error != null ? error.error() : null;
            log.warn(
                    "Completed agent invocation: kind=local-delegate, executionId={}, parentExecutionId={}, groupId={}, agent={}, status=FAILED, durationMs={}, errorType={}, message={}",
                    executionId, parentExecutionId, groupId, runtimeName(agent), durationSince(invocationStartedAt.get()),
                    errorType(cause), errorMessage(cause));
            log.debug("Local agent '{}' invocation failure details", runtimeName(agent), cause);
            try {
                if (!isBlank(executionId)) {
                    executionService.failExecution(executionId,
                            cause != null ? cause.getClass().getSimpleName() : "AgentInvocationError",
                            cause != null ? cause.getMessage() : null);
                }
            } finally {
                childExecutionId.set(null);
                activeExecutionId.set(null);
                invocationStartedAt.set(null);
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
        private final AtomicReference<Long> invocationStartedAt = new AtomicReference<>();

        private RemoteExecutionTrackingAgentListener(String parentExecutionId, String name) {
            this.parentExecutionId = parentExecutionId;
            this.name = name;
        }

        @Override
        public void beforeAgentInvocation(AgentRequest agentRequest) {
            transitionExecution(parentExecutionId, ExecutionState.WAITING_AGENT);
            invocationStartedAt.set(System.currentTimeMillis());
            log.info("Invoking agent: kind=remote-a2a, executionId={}, parentExecutionId={}, groupId={}, agent={}",
                    null, parentExecutionId, null, name);
        }

        @Override
        public void afterAgentInvocation(AgentResponse agentResponse) {
            try {
                String output = agentResponse != null && agentResponse.output() != null ? agentResponse.output().toString()
                        : "";
                log.info(
                        "Completed agent invocation: kind=remote-a2a, executionId={}, parentExecutionId={}, groupId={}, agent={}, status=SUCCEEDED, durationMs={}, resultPresent={}",
                        null, parentExecutionId, null, name, durationSince(invocationStartedAt.get()), !isBlank(output));
                incrementParentAgentCount(parentExecutionId);
            } finally {
                invocationStartedAt.set(null);
                resumeExecution(parentExecutionId);
            }
        }

        @Override
        public void onAgentInvocationError(AgentInvocationError error) {
            Throwable cause = error != null ? error.error() : null;
            log.warn(
                    "Completed agent invocation: kind=remote-a2a, executionId={}, parentExecutionId={}, groupId={}, agent={}, status=FAILED, durationMs={}, errorType={}, message={}",
                    null, parentExecutionId, null, name, durationSince(invocationStartedAt.get()), errorType(cause),
                    errorMessage(cause));
            log.debug("Remote A2A agent '{}' invocation failure details", name, cause);
            invocationStartedAt.set(null);
            resumeExecution(parentExecutionId);
        }

        @Override
        public boolean inheritedBySubagents() {
            return false;
        }
    }

    private interface LocalChatAgent {

        String chat(@UserMessage String message);
    }

    private final class TextToolCallNormalizingChatModel implements ChatModel {

        private final ChatModel delegate;
        private final Set<String> availableToolNames;

        private TextToolCallNormalizingChatModel(ChatModel delegate, Set<String> availableToolNames) {
            this.delegate = delegate;
            this.availableToolNames = availableToolNames;
        }

        @Override
        public ChatResponse doChat(ChatRequest chatRequest) {
            ChatResponse response = delegate.chat(chatRequest);
            if (response == null || response.aiMessage() == null || response.aiMessage().hasToolExecutionRequests()) {
                return response;
            }
            List<ToolExecutionRequest> toolRequests = extractTextToolCalls(response.aiMessage().text(), availableToolNames);
            if (toolRequests.isEmpty()) {
                return response;
            }
            log.info("Converted assistant text into tool calls: tools={}",
                    toolRequests.stream().map(ToolExecutionRequest::name).toList());
            return response.toBuilder()
                    .aiMessage(AiMessage.from(toolRequests))
                    .build();
        }
    }

    public static final class LocalAgenticAction implements AgentSpecsProvider {

        private static final java.lang.reflect.Method INVOKE_METHOD = invokeMethod();

        private final String name;
        private final String description;
        private final LocalChatAgent chatAgent;
        private final AgentListener listener;

        private LocalAgenticAction(String name, String description, LocalChatAgent chatAgent, AgentListener listener) {
            this.name = name;
            this.description = description;
            this.chatAgent = chatAgent;
            this.listener = listener;
        }

        public String invoke(AgenticScope scope) {
            Object fallback = scope != null ? scope.readState("message") : null;
            String resolvedMessage = fallback != null ? fallback.toString() : "";
            if (blank(resolvedMessage) && scope != null) {
                resolvedMessage = scope.contextAsConversation();
            }
            return chatAgent.chat(resolvedMessage);
        }

        private AgentExecutor toAgentExecutor() {
            return new AgentExecutor(AgentInvoker.fromSpec(this, INVOKE_METHOD, name), this);
        }

        @Override
        public String outputKey() {
            return "response";
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public boolean async() {
            return false;
        }

        @Override
        public AgentListener listener() {
            return listener;
        }

        private static java.lang.reflect.Method invokeMethod() {
            try {
                return LocalAgenticAction.class.getMethod("invoke", AgenticScope.class);
            } catch (NoSuchMethodException ex) {
                throw new IllegalStateException("Unable to resolve local agentic action method", ex);
            }
        }

        private static boolean blank(String value) {
            return value == null || value.trim().isEmpty();
        }
    }

    public static final class LazySupervisorAgenticAction implements AgentSpecsProvider {

        private static final java.lang.reflect.Method INVOKE_METHOD = invokeMethod();

        private final String name;
        private final String description;
        private final Supplier<RuntimeAgent> supplier;
        private final String parentExecutionId;
        private final String groupId;
        private final String fallbackMessage;

        private LazySupervisorAgenticAction(String name, String description, Supplier<RuntimeAgent> supplier,
                String parentExecutionId, String groupId, String fallbackMessage) {
            this.name = name;
            this.description = description;
            this.supplier = supplier;
            this.parentExecutionId = parentExecutionId;
            this.groupId = groupId;
            this.fallbackMessage = fallbackMessage;
        }

        public String invoke(AgenticScope scope) {
            long startedAt = System.currentTimeMillis();
            log.info("Invoking agent: kind=supervisor-selected, executionId={}, parentExecutionId={}, groupId={}, agent={}",
                    parentExecutionId, parentExecutionId, groupId, name);
            try (RuntimeAgent runtimeAgent = supplier.get()) {
                if (runtimeAgent == null) {
                    log.info(
                            "Completed agent invocation: kind=supervisor-selected, executionId={}, parentExecutionId={}, groupId={}, agent={}, status=SKIPPED, durationMs={}",
                            parentExecutionId, parentExecutionId, groupId, name, System.currentTimeMillis() - startedAt);
                    return "";
                }
                Object result = runtimeAgent.invoker()
                        .invokeWithAgenticScope(Map.of("message", resolveMessage(scope)))
                        .result();
                log.info(
                        "Completed agent invocation: kind=supervisor-selected, executionId={}, parentExecutionId={}, groupId={}, agent={}, status=SUCCEEDED, durationMs={}, resultPresent={}",
                        parentExecutionId, parentExecutionId, groupId, name, System.currentTimeMillis() - startedAt,
                        result != null && !blank(result.toString()));
                return result != null ? result.toString() : "";
            } catch (Exception ex) {
                log.warn(
                        "Completed agent invocation: kind=supervisor-selected, executionId={}, parentExecutionId={}, groupId={}, agent={}, status=FAILED, durationMs={}, errorType={}, message={}",
                        parentExecutionId, parentExecutionId, groupId, name, System.currentTimeMillis() - startedAt,
                        ex.getClass().getSimpleName(), ex.getMessage());
                log.debug("Supervisor-selected agent '{}' invocation failure details", name, ex);
                throw ex;
            }
        }

        private AgentExecutor toAgentExecutor() {
            return new AgentExecutor(AgentInvoker.fromSpec(this, INVOKE_METHOD, name), this);
        }

        @Override
        public String outputKey() {
            return "response";
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public boolean async() {
            return false;
        }

        @Override
        public AgentListener listener() {
            return null;
        }

        private String resolveMessage(AgenticScope scope) {
            Object message = scope != null ? scope.readState("message") : null;
            if (message != null && !blank(message.toString())) {
                return message.toString();
            }
            return fallbackMessage != null ? fallbackMessage : "";
        }

        private static java.lang.reflect.Method invokeMethod() {
            try {
                return LazySupervisorAgenticAction.class.getMethod("invoke", AgenticScope.class);
            } catch (NoSuchMethodException ex) {
                throw new IllegalStateException("Unable to resolve lazy supervisor agent method", ex);
            }
        }

        private static boolean blank(String value) {
            return value == null || value.trim().isEmpty();
        }
    }

    private static final class AgenticWorkflowInvocationAdapter implements UntypedAgent {

        private final String name;
        private final Object delegate;

        private AgenticWorkflowInvocationAdapter(String name, Object delegate) {
            this.name = name;
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Map<String, Object> input) {
            return invokeWithAgenticScope(input).result();
        }

        @Override
        public ResultWithAgenticScope<String> invokeWithAgenticScope(Map<String, Object> input) {
            Map<String, Object> safeInput = input != null ? input : Map.of();
            UntypedAgent workflow = AgenticServices.sequenceBuilder()
                    .name("single-agent-" + safeName(name))
                    .description("Invokes one configured agent with explicit runtime input")
                    .subAgents(List.of(delegate))
                    .beforeCall(scope -> scope.writeStates(safeInput))
                    .output(AgenticWorkflowInvocationAdapter::lastOutput)
                    .build();
            ResultWithAgenticScope<String> result = workflow.invokeWithAgenticScope(safeInput);
            return result != null ? result : new ResultWithAgenticScope<>(null, "");
        }

        @Override
        public AgenticScope getAgenticScope(Object memoryId) {
            return null;
        }

        @Override
        public boolean evictAgenticScope(Object memoryId) {
            return false;
        }

        private static String lastOutput(AgenticScope scope) {
            if (scope == null || scope.agentInvocations() == null || scope.agentInvocations().isEmpty()) {
                return "";
            }
            return scope.agentInvocations().stream()
                    .map(invocation -> invocation.output())
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .filter(output -> !output.isBlank())
                    .reduce((previous, current) -> current)
                    .orElse("");
        }

        private static String safeName(String name) {
            String normalized = name != null ? name.toLowerCase().replaceAll("[^a-z0-9]+", "-") : "";
            normalized = normalized.replaceAll("^-+|-+$", "");
            return !normalized.isBlank() ? normalized : "agent";
        }
    }

}
