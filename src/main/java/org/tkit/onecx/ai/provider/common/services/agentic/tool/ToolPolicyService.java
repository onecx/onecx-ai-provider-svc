package org.tkit.onecx.ai.provider.common.services.agentic.tool;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.Tool;

import lombok.extern.slf4j.Slf4j;

/**
 * Enforces tool-call policy constraints before MCP tool invocation.
 *
 * Constraints enforced:
 * - Allow-list: only tools explicitly associated with the agent are permitted.
 * - Recursion depth: max depth to prevent infinite loops.
 * - Timeout budget: cumulative timeout for all tool invocations.
 * - Retry budget: max retries per tool call.
 */
@Slf4j
@ApplicationScoped
public class ToolPolicyService {

    private static final int DEFAULT_MAX_RECURSION_DEPTH = 10;
    private static final long DEFAULT_TIMEOUT_BUDGET_MS = 300_000; // 5 minutes
    private static final int DEFAULT_RETRY_BUDGET = 3;

    /**
     * Validates if a tool is allowed for use by an agent.
     *
     * @param agent the agent making the tool call
     * @param toolId the tool ID to validate
     * @return true if the tool is in the agent's allowed list, false otherwise
     */
    public boolean isToolAllowed(Agent agent, String toolId) {
        if (agent == null || toolId == null || toolId.isBlank()) {
            log.warn("Attempted to validate null or blank parameters for tool permission");
            return false;
        }

        if (agent.getTools() == null || agent.getTools().isEmpty()) {
            log.debug("Agent '{}' has no tools configured", agent.getName());
            return false;
        }

        boolean allowed = agent.getTools().stream()
                .map(Tool::getId)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .anyMatch(toolId::equals);

        if (!allowed) {
            log.debug("Tool '{}' is not in the allow-list for agent '{}'", toolId, agent.getName());
        }

        return allowed;
    }

    /**
     * Gets the set of allowed tool IDs for an agent.
     *
     * @param agent the agent
     * @return set of allowed tool IDs (empty if no tools configured)
     */
    public Set<String> getAllowedToolIds(Agent agent) {
        if (agent == null || agent.getTools() == null) {
            return Set.of();
        }

        return agent.getTools().stream()
                .map(Tool::getId)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    /**
     * Validates recursion depth constraint.
     *
     * @param currentDepth current recursion depth
     * @param maxDepth maximum allowed depth (or null for default)
     * @return true if within depth limit
     */
    public boolean isWithinRecursionDepth(int currentDepth, Integer maxDepth) {
        int limit = maxDepth != null ? maxDepth : DEFAULT_MAX_RECURSION_DEPTH;

        if (currentDepth > limit) {
            log.warn("Recursion depth exceeded: current={}, limit={}", currentDepth, limit);
            return false;
        }

        return true;
    }

    /**
     * Validates if remaining time is sufficient for a tool call.
     *
     * @param remainingTimeMs remaining time budget in milliseconds
     * @param requiredTimeMs required time for the tool call
     * @return true if sufficient time available
     */
    public boolean hasTimeRemaining(long remainingTimeMs, long requiredTimeMs) {
        if (remainingTimeMs < requiredTimeMs) {
            log.warn("Timeout budget exceeded: remaining={}ms, required={}ms", remainingTimeMs, requiredTimeMs);
            return false;
        }

        return true;
    }

    /**
     * Calculates retry allowance for a tool call.
     *
     * @param remainingRetries remaining retry budget
     * @return true if retries available
     */
    public boolean hasRetriesRemaining(int remainingRetries) {
        if (remainingRetries <= 0) {
            log.warn("Retry budget exhausted");
            return false;
        }

        return true;
    }

    /**
     * Gets the default maximum recursion depth.
     */
    public int getDefaultMaxRecursionDepth() {
        return DEFAULT_MAX_RECURSION_DEPTH;
    }

    /**
     * Gets the default timeout budget in milliseconds.
     */
    public long getDefaultTimeoutBudgetMs() {
        return DEFAULT_TIMEOUT_BUDGET_MS;
    }

    /**
     * Gets the default retry budget.
     */
    public int getDefaultRetryBudget() {
        return DEFAULT_RETRY_BUDGET;
    }
}
