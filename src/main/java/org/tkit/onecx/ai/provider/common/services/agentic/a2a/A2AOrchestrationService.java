package org.tkit.onecx.ai.provider.common.services.agentic.a2a;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.tkit.onecx.ai.provider.common.models.DispatchConfig;
import org.tkit.onecx.ai.provider.common.services.execution.ExecutionService;
import org.tkit.onecx.ai.provider.common.services.llm.AgentDispatchService;
import org.tkit.onecx.ai.provider.domain.daos.AgentDAO;
import org.tkit.onecx.ai.provider.domain.daos.ExternalAgentDAO;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.Execution;
import org.tkit.onecx.ai.provider.domain.models.ExternalAgent;
import org.tkit.onecx.ai.provider.domain.models.enums.AgentStatus;
import org.tkit.onecx.ai.provider.domain.models.enums.ExecutionState;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatMessageDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import lombok.extern.slf4j.Slf4j;

/**
 * Executes root agents and their A2A group members, including optional external agent cards.
 */
@Slf4j
@ApplicationScoped
public class A2AOrchestrationService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Inject
    AgentDAO agentDAO;

    @Inject
    ExternalAgentDAO externalAgentDAO;

    @Inject
    ExecutionService executionService;

    @Inject
    DefaultA2AGroupPlanner a2aGroupPlanner;

    @Inject
    SequentialA2AExecutor sequentialA2AExecutor;

    @Inject
    AgentDispatchService agentDispatchService;

    @Inject
    DispatchConfig dispatchConfig;

    @Inject
    ExternalAgentDiscoveryService externalAgentDiscoveryService;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Invokes the root agent and, when enabled, recursively orchestrates its A2A groups.
     */
    public Response invokeRoot(Agent agent, ChatRequestDTOV1 chatRequestDTO) {
        if (agent == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Agent cannot be null")
                    .build();
        }

        Execution execution = executionService.createExecution(agent, null, extractRequestExcerpt(chatRequestDTO));
        String executionId = execution.getExecutionId();
        try {
            executionService.startExecution(executionId);
            AgentInvocationResult result = invokeAgent(agent, chatRequestDTO, null, executionId, 0, new LinkedHashSet<>(),
                    true);
            String responseText = result.responseText() != null ? result.responseText() : "";
            if (!result.successful()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(responseText.isBlank() ? "Agent invocation failed" : responseText)
                        .header("X-Execution-Id", executionId)
                        .build();
            }
            return Response.ok(mapToChatMessageResponseDTO(responseText))
                    .header("X-Execution-Id", executionId)
                    .build();
        } catch (Exception ex) {
            executionService.failExecution(executionId, ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }

    private AgentInvocationResult invokeAgent(Agent agent, ChatRequestDTOV1 chatRequestDTO, String groupId,
            String executionId, int depth, Set<String> activePath, boolean reuseExistingExecution) {
        if (agent == null) {
            return new AgentInvocationResult(null, "", false);
        }

        String agentKey = agentKey(agent);
        if (activePath.contains(agentKey)) {
            log.warn("Skipping recursive A2A invocation for agent '{}' because it is already in the active path",
                    agent.getName());
            return new AgentInvocationResult(null, "", true);
        }

        if (!isCallable(agent)) {
            log.debug("Skipping non-callable agent '{}' with status '{}'", agent.getName(), agent.getStatus());
            return new AgentInvocationResult(null, "", true);
        }

        activePath.add(agentKey);
        String currentExecutionId = executionId;
        try {
            if (!reuseExistingExecution) {
                Execution execution = executionService.createExecution(agent, groupId, extractRequestExcerpt(chatRequestDTO));
                currentExecutionId = execution.getExecutionId();
                executionService.startExecution(currentExecutionId);
            }

            Response singleResponse = agentDispatchService.dispatch(agent, chatRequestDTO, currentExecutionId);
            if (!isSuccess(singleResponse)) {
                handleFailedExecution(currentExecutionId, singleResponse);
                return new AgentInvocationResult(currentExecutionId, extractResponseText(singleResponse), false);
            }

            String responseText = extractResponseText(singleResponse);
            String mergedText = responseText;
            if (Boolean.TRUE.equals(agent.getA2aEnabled()) && isWithinDepthLimit(depth)) {
                mergedText = orchestrateChildren(agent, chatRequestDTO, currentExecutionId, depth, activePath, responseText);
            }

            if (!reuseExistingExecution || currentExecutionId != null) {
                executionService.succeedExecution(currentExecutionId, mergedText);
            }
            return new AgentInvocationResult(currentExecutionId, mergedText, true);
        } catch (Exception ex) {
            log.warn("Agent invocation failed for '{}'", agent.getName(), ex);
            try {
                if (currentExecutionId != null && !currentExecutionId.isBlank()) {
                    executionService.failExecution(currentExecutionId, ex.getClass().getSimpleName(), ex.getMessage());
                }
            } catch (Exception ignore) {
                // parent execution may already be failed or may not exist in edge cases
            }
            return new AgentInvocationResult(currentExecutionId, "", false);
        } finally {
            activePath.remove(agentKey);
        }
    }

    private String orchestrateChildren(Agent agent, ChatRequestDTOV1 chatRequestDTO, String executionId, int depth,
            Set<String> activePath, String baseText) {
        var plan = a2aGroupPlanner.plan(agent);
        if (plan == null || plan.units() == null || plan.units().isEmpty()) {
            return baseText;
        }

        String childrenText = sequentialA2AExecutor.execute(plan,
                unit -> executeGroupUnit(agent, chatRequestDTO, executionId, unit, depth + 1, activePath));

        if (childrenText == null || childrenText.isBlank()) {
            return baseText;
        }

        if (baseText == null || baseText.isBlank()) {
            return childrenText.trim();
        }

        return baseText.trim() + System.lineSeparator() + System.lineSeparator() + childrenText.trim();
    }

    private String executeGroupUnit(Agent agent, ChatRequestDTOV1 chatRequestDTO, String parentExecutionId,
            A2AExecutionUnit unit, int depth, Set<String> activePath) {
        List<String> results = new ArrayList<>();
        List<InvocationTarget> targets = resolveTargets(agent, unit);

        for (InvocationTarget target : targets) {
            try {
                if (target.externalAgent() != null) {
                    results.addAll(invokeExternalTarget(parentExecutionId, chatRequestDTO, unit, target.externalAgent()));
                    continue;
                }

                if (target.agent() == null) {
                    continue;
                }

                transitionParentToWaiting(parentExecutionId);

                AgentInvocationResult childResult = invokeAgent(target.agent(), chatRequestDTO, unit.groupId(),
                        null, depth, activePath, false);
                if (childResult.responseText() != null && !childResult.responseText().isBlank()) {
                    results.add(formatTargetResult(target.agent().getName(), unit.groupName(), childResult.responseText()));
                }
                if (childResult.successful()) {
                    incrementParentAgentCount(parentExecutionId);
                }
            } finally {
                resumeParentIfNeeded(parentExecutionId);
            }
        }

        return String.join(System.lineSeparator(), results);
    }

    private List<InvocationTarget> resolveTargets(Agent agent, A2AExecutionUnit unit) {
        List<InvocationTarget> targets = new ArrayList<>();

        if (unit != null && unit.groupId() != null && !unit.groupId().isBlank()) {
            for (Agent other : agentDAO.findAgentsByGroupId(unit.groupId())) {
                if (other == null || other.getId() == null || agent.getId() == null) {
                    continue;
                }
                if (Objects.equals(agent.getId(), other.getId())) {
                    continue;
                }
                if (!isCallable(other)) {
                    continue;
                }
                targets.add(new InvocationTarget(other, null));
            }
        }

        if (unit != null && unit.groupId() != null && !unit.groupId().isBlank()) {
            List<ExternalAgent> externalAgents = externalAgentDAO.findExternalAgentsByGroupId(unit.groupId());
            if (externalAgents == null) {
                externalAgents = List.of();
            }
            for (ExternalAgent externalAgent : externalAgents) {
                if (externalAgent == null || !Boolean.TRUE.equals(externalAgent.getEnabled())
                        || isBlank(externalAgent.getDiscoveryUrl())) {
                    continue;
                }
                targets.add(new InvocationTarget(null, externalAgent));
            }
        }

        targets.sort((left, right) -> {
            String leftName = left.agent() != null ? left.agent().getName() : left.externalAgent().getName();
            String rightName = right.agent() != null ? right.agent().getName() : right.externalAgent().getName();
            return safeString(leftName).compareToIgnoreCase(safeString(rightName));
        });

        return targets;
    }

    private List<String> invokeExternalTarget(String parentExecutionId, ChatRequestDTOV1 chatRequestDTO,
            A2AExecutionUnit unit, ExternalAgent externalAgent) {
        List<String> results = new ArrayList<>();
        transitionParentToWaiting(parentExecutionId);

        try {
            String responseText = invokeExternalAgent(externalAgent, chatRequestDTO);
            if (responseText != null && !responseText.isBlank()) {
                results.add(formatTargetResult(externalAgent.getName(), unit.groupName(), responseText));
                incrementParentAgentCount(parentExecutionId);
            }
        } finally {
            resumeParentIfNeeded(parentExecutionId);
        }

        return results;
    }

    /**
     * Invokes an external agent using strict A2A protocol:
     * <ol>
     * <li>GET {@code discoveryUrl} → parse agent card to obtain the actual invocation endpoint</li>
     * <li>POST the chat request to the discovered invocation endpoint</li>
     * </ol>
     */
    private String invokeExternalAgent(ExternalAgent externalAgent, ChatRequestDTOV1 chatRequestDTO) {
        // Step 1: discover the agent card to resolve the actual invocation endpoint
        AgentCard card = externalAgentDiscoveryService.fetchAgentCard(externalAgent.getDiscoveryUrl());
        if (card == null) {
            log.warn("Skipping external agent '{}': agent card discovery failed for discovery URL '{}'",
                    externalAgent.getName(), externalAgent.getDiscoveryUrl());
            return null;
        }

        // Step 2: invoke the discovered endpoint
        try {
            String body = objectMapper.writeValueAsString(chatRequestDTO);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(card.url()))
                    .timeout(Duration.ofSeconds(dispatchConfig.providerConfig().timeout()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));

            if (!isBlank(externalAgent.getApiKey())) {
                builder.header("Authorization", externalAgent.getApiKey());
            }

            log.debug("Invoking external agent '{}' at discovered endpoint '{}'", externalAgent.getName(), card.url());
            HttpResponse<String> response = HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("External agent '{}' returned non-success status {} from '{}'",
                        externalAgent.getName(), response.statusCode(), card.url());
                return null;
            }

            return extractResponseText(response.body());
        } catch (JsonProcessingException e) {
            log.warn("Unable to serialize chat request for external agent '{}'", externalAgent.getName(), e);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("External agent '{}' invocation interrupted", externalAgent.getName(), e);
            return null;
        } catch (IOException e) {
            log.warn("External agent '{}' invocation failed at '{}'", externalAgent.getName(), card.url(), e);
            return null;
        }
    }

    private void transitionParentToWaiting(String executionId) {
        if (isBlank(executionId)) {
            return;
        }
        try {
            executionService.waitForResource(executionId, ExecutionState.WAITING_AGENT);
        } catch (Exception ex) {
            log.warn("Unable to transition execution {} to WAITING_AGENT: {}", executionId, ex.getMessage());
        }
    }

    private void resumeParentIfNeeded(String executionId) {
        if (isBlank(executionId)) {
            return;
        }
        try {
            executionService.resumeExecution(executionId);
        } catch (Exception ex) {
            log.warn("Unable to resume execution {} after agent call: {}", executionId, ex.getMessage());
        }
    }

    private void incrementParentAgentCount(String executionId) {
        if (isBlank(executionId)) {
            return;
        }
        try {
            executionService.incrementAgentCallCount(executionId);
        } catch (Exception ex) {
            log.warn("Unable to increment agent call counter for execution {}: {}", executionId, ex.getMessage());
        }
    }

    private boolean isSuccess(Response response) {
        return response != null && response.getStatus() >= 200 && response.getStatus() < 300;
    }

    private void handleFailedExecution(String executionId, Response response) {
        String errorType = response != null ? "HttpStatus:" + response.getStatus() : "HttpStatus:unknown";
        String errorMessage = response != null && response.getEntity() != null ? response.getEntity().toString()
                : "Dispatch returned non-success status";
        executionService.failExecution(executionId, errorType, errorMessage);
    }

    private boolean isWithinDepthLimit(int depth) {
        DispatchConfig.A2AConfig config = dispatchConfig != null ? dispatchConfig.a2aConfig() : null;
        int maxDepth = config != null ? config.maxDepth() : 10;
        return depth <= maxDepth;
    }

    private boolean isCallable(Agent agent) {
        if (agent == null) {
            return false;
        }
        return agent.getStatus() == null || AgentStatus.LIVE.equals(agent.getStatus());
    }

    private ChatMessageDTOV1 mapToChatMessageResponseDTO(String responseMessage) {
        ChatMessageDTOV1 chatMessageDTOV1 = new ChatMessageDTOV1();
        chatMessageDTOV1.setMessage(responseMessage);
        chatMessageDTOV1.setType(ChatMessageDTOV1.TypeEnum.ASSISTANT);
        return chatMessageDTOV1;
    }

    private String extractResponseText(Response response) {
        if (response == null || response.getEntity() == null) {
            return "";
        }
        Object entity = response.getEntity();
        if (entity instanceof ChatMessageDTOV1 chatMessageDTOV1) {
            return chatMessageDTOV1.getMessage() != null ? chatMessageDTOV1.getMessage() : "";
        }
        return entity.toString();
    }

    private String extractResponseText(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }
        try {
            ChatMessageDTOV1 dto = objectMapper.readValue(responseBody, ChatMessageDTOV1.class);
            if (dto.getMessage() != null) {
                return dto.getMessage();
            }
        } catch (Exception ignore) {
            // Not JSON or not the expected response structure; fall back to the raw body.
        }
        return responseBody;
    }

    private String extractRequestExcerpt(ChatRequestDTOV1 chatRequestDTO) {
        if (chatRequestDTO == null || chatRequestDTO.getChatMessage() == null
                || chatRequestDTO.getChatMessage().getMessage() == null) {
            return null;
        }
        String message = chatRequestDTO.getChatMessage().getMessage();
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    private String formatTargetResult(String targetName, String groupName, String responseText) {
        return "[" + (isBlank(groupName) ? "A2A group" : groupName) + "] "
                + (isBlank(targetName) ? "agent" : targetName)
                + System.lineSeparator()
                + responseText.trim();
    }

    private String agentKey(Agent agent) {
        if (agent == null) {
            return "";
        }
        if (agent.getId() != null) {
            return String.valueOf(agent.getId());
        }
        return safeString(agent.getName());
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record InvocationTarget(Agent agent, ExternalAgent externalAgent) {
    }
}
