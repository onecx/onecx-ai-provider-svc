package org.tkit.onecx.ai.provider.common.services.agentic.runtime;

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
import dev.langchain4j.agentic.scope.AgentInvocation;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class AgenticRuntimeService {

    private static final String INPUT_ROOT_RESPONSE = "rootResponse";
    private static final String INPUT_COLLABORATION = "collaboration";

    @Inject
    ExecutionService executionService;

    @Inject
    RuntimeAgentFactory runtimeAgentFactory;

    @Inject
    ChatModelFactory chatModelFactory;

    public AgenticRuntimeResult invokeRoot(Agent agent, ChatRequestDTOV1 request) {
        if (agent == null) {
            return new AgenticRuntimeResult(null, "Agent cannot be null", false);
        }

        Execution execution = executionService.createExecution(agent, null, extractRequestExcerpt(request));
        String executionId = execution.getExecutionId();
        try (RuntimeAgent rootAgent = runtimeAgentFactory.rootAgent(agent, request, executionId)) {
            executionService.startExecution(executionId);

            String rootResponse = invokeAgent(rootAgent.agent(), request);
            String finalResponse = rootResponse;
            if (Boolean.TRUE.equals(agent.getA2aEnabled()) && agent.getGroups() != null && !agent.getGroups().isEmpty()) {
                String collaboration = executeGroups(agent, request, executionId);
                finalResponse = synthesizeFinalResponse(agent, request, rootResponse, collaboration);
            }

            executionService.succeedExecution(executionId, finalResponse);
            return new AgenticRuntimeResult(executionId, finalResponse, true);
        } catch (Exception ex) {
            log.warn("Agentic runtime failed for root agent '{}'", agent.getName(), ex);
            try {
                executionService.failExecution(executionId, ex.getClass().getSimpleName(), ex.getMessage());
            } catch (Exception ignored) {
                // Execution may already be terminal.
            }
            return new AgenticRuntimeResult(executionId, ex.getMessage(), false);
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
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String executeGroup(Agent rootAgent, AgentGroup group, ChatRequestDTOV1 request, String parentExecutionId) {
        List<RuntimeAgent> runtimeAgents = runtimeAgentFactory.agentsForGroup(rootAgent, group, request, parentExecutionId);
        if (runtimeAgents.isEmpty()) {
            return "";
        }

        try {
            AgentGroupOrchestrationMode mode = group.getOrchestrationMode() != null
                    ? group.getOrchestrationMode()
                    : AgentGroupOrchestrationMode.SUPERVISOR_ROUTED;

            return switch (mode) {
                case SUPERVISOR_ROUTED -> executeSupervisorRoutedGroup(rootAgent, group, runtimeAgents, request);
                case SEQUENTIAL -> executeSequentialGroup(group, runtimeAgents, request);
                case PARALLEL -> executeParallelGroup(group, runtimeAgents, request);
            };
        } finally {
            runtimeAgents.forEach(RuntimeAgent::close);
        }
    }

    private String executeSupervisorRoutedGroup(Agent rootAgent, AgentGroup group, List<RuntimeAgent> runtimeAgents,
            ChatRequestDTOV1 request) {
        SupervisorAgent supervisor = AgenticServices.supervisorBuilder()
                .chatModel(chatModelFactory.createChatModel(rootAgent))
                .name("a2a-supervisor-" + safeString(group.getId()))
                .description("Routes the user request to only relevant local or remote agents")
                .supervisorContext(buildSupervisorContext(group, runtimeAgents))
                .maxAgentsInvocations(runtimeAgents.size())
                .responseStrategy(mapResponseStrategy(group.getResponseStrategy()))
                .subAgents(runtimeAgents.stream().map(RuntimeAgent::agent).toList())
                .build();
        Object result = supervisor.invoke(extractUserMessage(request));
        return result != null ? result.toString() : "";
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

    private String synthesizeFinalResponse(Agent agent, ChatRequestDTOV1 request, String rootText, String collaborationText) {
        if (isBlank(collaborationText)) {
            return rootText != null ? rootText : "";
        }
        if (isBlank(rootText)) {
            return collaborationText.trim();
        }

        try {
            UntypedAgent synthesizer = AgenticServices.agentBuilder()
                    .name("response-synthesizer")
                    .description("Combines root and specialist outputs into one assistant response")
                    .outputKey("response")
                    .chatModel(chatModelFactory.createChatModel(agent))
                    .systemMessage("""
                            You combine specialist agent outputs into one concise assistant answer.
                            Do not include agent names, group labels, transcripts, or intermediate chain text.
                            Use specialist output only when it is relevant to the user request.
                            """)
                    .userMessageProvider(input -> synthesisPrompt(input, request))
                    .build();
            Object response = synthesizer.invoke(synthesisInput(request, rootText, collaborationText));
            if (response != null && !isBlank(response.toString())) {
                return response.toString();
            }
        } catch (Exception ex) {
            log.warn("Final response synthesis failed; returning collaboration output", ex);
        }
        return collaborationText.trim();
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

    private Map<String, Object> agentInput(ChatRequestDTOV1 request) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put(RuntimeAgentFactory.INPUT_REQUEST, request);
        input.put("message", extractUserMessage(request));
        return input;
    }

    private Map<String, Object> synthesisInput(ChatRequestDTOV1 request, String rootText, String collaborationText) {
        Map<String, Object> input = agentInput(request);
        input.put(INPUT_ROOT_RESPONSE, rootText);
        input.put(INPUT_COLLABORATION, collaborationText);
        return input;
    }

    @SuppressWarnings("unchecked")
    private String synthesisPrompt(Object input, ChatRequestDTOV1 fallbackRequest) {
        Map<String, Object> map = input instanceof Map<?, ?> value ? (Map<String, Object>) value : Map.of();
        return """
                User request:
                %s

                Root agent response:
                %s

                Specialist collaboration output:
                %s

                Return one final assistant message.
                """.formatted(
                extractUserMessage(requestFromMap(map, fallbackRequest)),
                safeString(map.get(INPUT_ROOT_RESPONSE)),
                safeString(map.get(INPUT_COLLABORATION)));
    }

    private ChatRequestDTOV1 requestFromMap(Map<String, Object> input, ChatRequestDTOV1 fallbackRequest) {
        Object request = input.get(RuntimeAgentFactory.INPUT_REQUEST);
        return request instanceof ChatRequestDTOV1 chatRequest ? chatRequest : fallbackRequest;
    }

    private String invokeAgent(UntypedAgent agent, ChatRequestDTOV1 request) {
        Object result = agent.invoke(agentInput(request));
        return result != null ? result.toString() : "";
    }

    private String buildSupervisorContext(AgentGroup group, List<RuntimeAgent> runtimeAgents) {
        StringBuilder sb = new StringBuilder("Call only agents relevant to the user's request.")
                .append(System.lineSeparator())
                .append("Do not call unrelated agents just because they are in this group.")
                .append(System.lineSeparator())
                .append("Group: ")
                .append(safeString(group.getName()));
        if (!isBlank(group.getDescription())) {
            sb.append(System.lineSeparator()).append("Group description: ").append(group.getDescription().trim());
        }
        if (!isBlank(group.getRoutingInstructions())) {
            sb.append(System.lineSeparator()).append("Routing instructions: ").append(group.getRoutingInstructions().trim());
        }
        sb.append(System.lineSeparator()).append("Available agents:");
        for (RuntimeAgent agent : runtimeAgents) {
            sb.append(System.lineSeparator()).append("- ").append(safeString(agent.name()));
            if (!isBlank(agent.description())) {
                sb.append(": ").append(agent.description().trim());
            }
        }
        return sb.toString();
    }

    private SupervisorResponseStrategy mapResponseStrategy(AgentGroupResponseStrategy responseStrategy) {
        if (responseStrategy == null) {
            return SupervisorResponseStrategy.LAST;
        }
        return switch (responseStrategy) {
            case SUMMARY -> SupervisorResponseStrategy.SUMMARY;
            case SCORED -> SupervisorResponseStrategy.SCORED;
            case LAST -> SupervisorResponseStrategy.LAST;
        };
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
