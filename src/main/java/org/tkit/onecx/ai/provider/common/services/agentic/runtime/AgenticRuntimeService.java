package org.tkit.onecx.ai.provider.common.services.agentic.runtime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.tkit.onecx.ai.provider.common.services.execution.ExecutionService;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.AgentGroup;
import org.tkit.onecx.ai.provider.domain.models.Execution;
import org.tkit.onecx.ai.provider.domain.models.enums.AgentGroupOrchestrationMode;
import org.tkit.onecx.ai.provider.domain.models.enums.AgentGroupResponseStrategy;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.agent.ErrorRecoveryResult;
import dev.langchain4j.agentic.scope.AgentInvocation;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorContextStrategy;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class AgenticRuntimeService {

    @Inject
    ExecutionService executionService;

    @Inject
    RuntimeAgentFactory runtimeAgentFactory;

    public AgenticRuntimeResult invokeRoot(Agent agent, ChatRequestDTOV1 request) {
        if (agent == null) {
            return new AgenticRuntimeResult(null, "Agent cannot be null", false);
        }

        Execution execution = executionService.createExecution(agent, null, extractRequestExcerpt(request));
        String executionId = execution.getExecutionId();
        try {
            executionService.startExecution(executionId);

            String finalResponse;
            if (Boolean.TRUE.equals(agent.getA2aEnabled()) && agent.getGroups() != null && !agent.getGroups().isEmpty()) {
                finalResponse = executeGroups(agent, request, executionId);
                if (isBlank(finalResponse)) {
                    finalResponse = invokeRootAgent(agent, request, executionId);
                }
            } else {
                finalResponse = invokeRootAgent(agent, request, executionId);
            }

            executionService.succeedExecution(executionId, finalResponse);
            return new AgenticRuntimeResult(executionId, finalResponse, true);
        } catch (Exception ex) {
            log.warn("Agentic runtime failed for root agent '{}': {}: {}", agent.getName(),
                    ex.getClass().getSimpleName(), ex.getMessage());
            log.debug("Agentic runtime failure details for root agent '{}'", agent.getName(), ex);
            try {
                executionService.failExecution(executionId, ex.getClass().getSimpleName(), ex.getMessage());
            } catch (Exception ignored) {
                // Execution may already be terminal.
            }
            return new AgenticRuntimeResult(executionId,
                    "The agent could not complete the request. Please try again.", false);
        }
    }

    private String invokeRootAgent(Agent agent, ChatRequestDTOV1 request, String executionId) {
        try (RuntimeAgent rootAgent = runtimeAgentFactory.rootAgent(agent, request, executionId)) {
            return invokeSingleAgent(rootAgent, request, executionId, null, "root");
        }
    }

    private String executeGroups(Agent agent, ChatRequestDTOV1 request, String parentExecutionId) {
        List<AgentGroup> groups = agent.getGroups().stream()
                .filter(group -> group != null && group.getId() != null)
                .sorted(Comparator.comparing(group -> safeString(group.getName()).toLowerCase()))
                .toList();

        return groups.stream()
                .map(group -> executeGroup(agent, group, request, parentExecutionId))
                .filter(result -> !isBlank(result))
                .map(String::trim)
                .findFirst()
                .orElse("");
    }

    private String executeGroup(Agent rootAgent, AgentGroup group, ChatRequestDTOV1 request, String parentExecutionId) {
        AgentGroupOrchestrationMode mode = group.getOrchestrationMode() != null
                ? group.getOrchestrationMode()
                : AgentGroupOrchestrationMode.LEAD_DELEGATES;

        if (AgentGroupOrchestrationMode.LEAD_DELEGATES.equals(mode)) {
            return executeLeadDelegatesGroup(rootAgent, group, request, parentExecutionId);
        }
        if (AgentGroupOrchestrationMode.SUPERVISOR_ROUTED.equals(mode)) {
            return executeSupervisorRoutedGroup(rootAgent, group, request, parentExecutionId);
        }

        List<RuntimeAgent> peerAgents = runtimeAgentFactory.agentsForGroup(rootAgent, group, request, parentExecutionId);
        if (peerAgents.isEmpty()) {
            return "";
        }
        return switch (mode) {
            case LEAD_DELEGATES -> "";
            case SUPERVISOR_ROUTED -> "";
            case SEQUENTIAL -> executeWorkflowGroup(rootAgent, group, peerAgents, request, parentExecutionId, true);
            case PARALLEL -> executeWorkflowGroup(rootAgent, group, peerAgents, request, parentExecutionId, false);
        };
    }

    private String executeLeadDelegatesGroup(Agent rootAgent, AgentGroup group, ChatRequestDTOV1 request,
            String parentExecutionId) {
        List<RuntimeAgentDelegate> peerAgents = runtimeAgentFactory.delegatesForGroup(rootAgent, group, request,
                parentExecutionId);
        if (peerAgents.isEmpty()) {
            return "";
        }
        try (RuntimeAgent leadAgent = runtimeAgentFactory.leadAgent(rootAgent, request, parentExecutionId, peerAgents)) {
            return invokeSingleAgent(leadAgent, request, parentExecutionId, group.getId(), "lead-delegates");
        }
    }

    private String executeSupervisorRoutedGroup(Agent rootAgent, AgentGroup group, ChatRequestDTOV1 request,
            String parentExecutionId) {
        List<RuntimeAgent> runtimeAgents = new ArrayList<>();
        try {
            RuntimeAgent rootRuntimeAgent = runtimeAgentFactory.rootAgent(rootAgent, request, parentExecutionId);
            runtimeAgents.add(rootRuntimeAgent);
            runtimeAgents.addAll(runtimeAgentFactory.agentsForGroup(rootAgent, group, request, parentExecutionId));
            if (runtimeAgents.isEmpty()) {
                return "";
            }

            runtimeAgents.sort(Comparator.comparing(agent -> safeString(agent.name()).toLowerCase()));
            runtimeAgents.forEach(agent -> log.info(
                    "Invoking agent: kind=supervisor-candidate, executionId={}, parentExecutionId={}, groupId={}, agent={}",
                    parentExecutionId, parentExecutionId, group.getId(), agent.name()));

            SupervisorAgent supervisor = AgenticServices.supervisorBuilder()
                    .name("a2a-supervisor-" + safeString(group.getId()))
                    .description("Routes the user request to the most relevant configured agents")
                    .chatModel(runtimeAgentFactory.chatModel(rootAgent))
                    .subAgents(runtimeAgents.stream().map(RuntimeAgent::agent).toList())
                    .responseStrategy(toSupervisorResponseStrategy(group.getResponseStrategy()))
                    .contextGenerationStrategy(SupervisorContextStrategy.CHAT_MEMORY_AND_SUMMARIZATION)
                    .maxAgentsInvocations(Math.max(1, runtimeAgents.size()))
                    .requestGenerator(scope -> supervisorRequest(rootAgent, group, request, runtimeAgents))
                    .errorHandler(error -> {
                        log.warn("Supervisor agent '{}' failed: {}", error.agentName(),
                                error.exception() != null ? error.exception().getMessage() : null);
                        log.debug("Supervisor agent failure details for '{}'", error.agentName(),
                                error.exception());
                        return ErrorRecoveryResult.result("");
                    })
                    .build();

            String supervisorRequest = supervisorRequest(rootAgent, group, request, runtimeAgents);
            var result = supervisor.invokeWithAgenticScope(supervisorRequest);
            return result != null && result.result() != null ? result.result() : "";
        } finally {
            runtimeAgents.forEach(RuntimeAgent::close);
        }
    }

    private String executeWorkflowGroup(Agent rootAgent, AgentGroup group, List<RuntimeAgent> peerAgents,
            ChatRequestDTOV1 request, String parentExecutionId, boolean sequential) {
        try (RuntimeAgent rootRuntimeAgent = runtimeAgentFactory.rootAgent(rootAgent, request, parentExecutionId)) {
            List<RuntimeAgent> runtimeAgents = new ArrayList<>();
            runtimeAgents.add(rootRuntimeAgent);
            runtimeAgents.addAll(peerAgents);
            runtimeAgents.sort(Comparator.comparing(agent -> safeString(agent.name()).toLowerCase()));
            runtimeAgents.forEach(agent -> log.info(
                    "Invoking agent: kind={}, executionId={}, parentExecutionId={}, groupId={}, agent={}",
                    sequential ? "sequential-workflow" : "parallel-workflow", parentExecutionId, parentExecutionId,
                    group.getId(), agent.name()));
            return sequential
                    ? executeSequentialGroup(group, runtimeAgents, request)
                    : executeParallelGroup(group, runtimeAgents, request);
        } finally {
            peerAgents.forEach(RuntimeAgent::close);
        }
    }

    private String executeSequentialGroup(AgentGroup group, List<RuntimeAgent> runtimeAgents, ChatRequestDTOV1 request) {
        UntypedAgent workflow = AgenticServices.sequenceBuilder()
                .name("a2a-sequence-" + safeString(group.getId()))
                .description("Runs explicitly ordered agents for the configured group")
                .subAgents(runtimeAgents.stream().map(RuntimeAgent::agent).toList())
                .beforeCall(scope -> scope.writeStates(agentInput(request)))
                .output(this::outputFromScope)
                .build();
        Object result = workflow.invoke(agentInput(request));
        return result != null ? result.toString() : "";
    }

    private String executeParallelGroup(AgentGroup group, List<RuntimeAgent> runtimeAgents, ChatRequestDTOV1 request) {
        UntypedAgent workflow = AgenticServices.parallelBuilder()
                .name("a2a-parallel-" + safeString(group.getId()))
                .description("Runs explicitly additive agents for the configured group")
                .subAgents(runtimeAgents.stream().map(RuntimeAgent::agent).toList())
                .beforeCall(scope -> scope.writeStates(agentInput(request)))
                .output(this::outputFromScope)
                .build();
        Object result = workflow.invoke(agentInput(request));
        return result != null ? result.toString() : "";
    }

    private String outputFromScope(AgenticScope scope) {
        if (scope == null || scope.agentInvocations() == null) {
            return "";
        }
        return scope.agentInvocations().stream()
                .map(AgentInvocation::output)
                .filter(output -> output != null && !isBlank(output.toString()))
                .map(output -> output.toString().trim())
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String supervisorRequest(Agent rootAgent, AgentGroup group, ChatRequestDTOV1 request,
            List<RuntimeAgent> runtimeAgents) {
        StringBuilder sb = new StringBuilder();
        sb.append("Route and answer the current user request using the most relevant configured agents.")
                .append(System.lineSeparator())
                .append("Do not call agents that are unrelated to the request.")
                .append(System.lineSeparator())
                .append("If no peer agent clearly fits better, call the initially dispatched agent.")
                .append(System.lineSeparator())
                .append("The initially dispatched agent is the fallback for general, conversational, ambiguous, or unmatched requests.")
                .append(System.lineSeparator())
                .append("Return one final assistant message.")
                .append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("Current user message:")
                .append(System.lineSeparator())
                .append(extractUserMessage(request))
                .append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("Initially dispatched agent: ")
                .append(safeString(rootAgent.getName()))
                .append(System.lineSeparator());
        if (!isBlank(group.getDescription())) {
            sb.append("Group description: ").append(group.getDescription().trim()).append(System.lineSeparator());
        }
        if (!isBlank(group.getRoutingInstructions())) {
            sb.append("Group routing instructions: ").append(group.getRoutingInstructions().trim())
                    .append(System.lineSeparator());
        }
        sb.append("Available agents:");
        for (RuntimeAgent agent : runtimeAgents) {
            sb.append(System.lineSeparator())
                    .append("- ")
                    .append(safeString(agent.name()));
            if (!isBlank(agent.description())) {
                sb.append(": ").append(agent.description().trim());
            }
        }
        return sb.toString();
    }

    private SupervisorResponseStrategy toSupervisorResponseStrategy(AgentGroupResponseStrategy strategy) {
        if (strategy == null) {
            return SupervisorResponseStrategy.SUMMARY;
        }
        return switch (strategy) {
            case LAST -> SupervisorResponseStrategy.LAST;
            case SUMMARY -> SupervisorResponseStrategy.SUMMARY;
            case SCORED -> SupervisorResponseStrategy.SCORED;
        };
    }

    private Map<String, Object> agentInput(ChatRequestDTOV1 request) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put(RuntimeAgentFactory.INPUT_REQUEST, request);
        input.put("message", extractUserMessage(request));
        return input;
    }

    private String invokeSingleAgent(RuntimeAgent agent, ChatRequestDTOV1 request, String executionId, String groupId,
            String kind) {
        log.info("Invoking agent: kind={}, executionId={}, parentExecutionId={}, groupId={}, agent={}", kind, executionId,
                null, groupId, agent.name());
        Object result = agent.invoker()
                .invokeWithAgenticScope(agentInput(request))
                .result();
        return result != null ? result.toString() : "";
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

    private String safeString(Object value) {
        return value == null ? "" : value.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
