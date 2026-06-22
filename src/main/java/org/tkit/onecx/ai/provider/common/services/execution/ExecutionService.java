package org.tkit.onecx.ai.provider.common.services.execution;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.tkit.onecx.ai.provider.domain.daos.ExecutionDAO;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.Execution;
import org.tkit.onecx.ai.provider.domain.models.enums.ExecutionState;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing execution lifecycle and state transitions.
 *
 * Responsibilities:
 * - Create new execution records with unique IDs.
 * - Track state transitions (PENDING -> RUNNING -> terminal).
 * - Update execution result and metadata upon completion.
 * - Support cancellation and timeout handling.
 * - Maintain audit trail via TraceableEntity timestamps.
 */
@Slf4j
@ApplicationScoped
public class ExecutionService {

    @Inject
    ExecutionDAO executionDAO;

    /**
     * Creates a new execution record in PENDING state.
     *
     * @param agent the agent being executed
     * @param groupId optional group ID for A2A execution
     * @param requestExcerpt optional request payload excerpt for audit trail
     * @return the created execution entity
     */
    @Transactional
    public Execution createExecution(Agent agent, String groupId, String requestExcerpt) {
        if (agent == null) {
            throw new IllegalArgumentException("Agent cannot be null");
        }

        Execution execution = new Execution();
        execution.setExecutionId(generateExecutionId());
        execution.setTenantId(agent.getTenantId());
        execution.setAgent(agent);
        execution.setAgentIdSnapshot(agent.getId() != null ? agent.getId().toString() : null);
        execution.setGroupId(groupId);
        execution.setGroupNameSnapshot(resolveGroupNameSnapshot(agent, groupId));
        execution.setState(ExecutionState.PENDING);
        execution.setStartTime(null);
        execution.setEndTime(null);
        execution.setToolCallCount(0);
        execution.setAgentCallCount(0);
        execution.setCancelled(false);
        execution.setRequestExcerpt(requestExcerpt);

        log.info("Creating execution {} for agent {} in tenant {}",
                execution.getExecutionId(), agent.getId(), agent.getTenantId());

        executionDAO.create(execution);
        return execution;
    }

    /**
     * Transitions execution to RUNNING state and sets start time.
     *
     * @param executionId the execution ID
     * @return updated execution
     */
    @Transactional
    public Execution startExecution(String executionId) {
        Execution execution = executionDAO.findByExecutionId(executionId);
        if (execution == null) {
            log.warn("Execution {} not found for start transition", executionId);
            throw new IllegalStateException("Execution not found: " + executionId);
        }

        if (!ExecutionState.PENDING.equals(execution.getState())) {
            log.warn("Cannot start execution {} - current state is {}", executionId, execution.getState());
            throw new IllegalStateException("Execution cannot be started from state: " + execution.getState());
        }

        execution.setState(ExecutionState.RUNNING);
        execution.setStartTime(OffsetDateTime.now());

        log.info("Started execution {}", executionId);
        executionDAO.update(execution);
        return execution;
    }

    /**
     * Transitions execution to a waiting state (WAITING_TOOL or WAITING_AGENT).
     *
     * @param executionId the execution ID
     * @param waitState the state to transition to (WAITING_TOOL or WAITING_AGENT)
     * @return updated execution
     */
    @Transactional
    public Execution waitForResource(String executionId, ExecutionState waitState) {
        if (!ExecutionState.WAITING_TOOL.equals(waitState) && !ExecutionState.WAITING_AGENT.equals(waitState)) {
            throw new IllegalArgumentException("Invalid wait state: " + waitState);
        }

        Execution execution = executionDAO.findByExecutionId(executionId);
        if (execution == null) {
            throw new IllegalStateException("Execution not found: " + executionId);
        }

        execution.setState(waitState);
        log.info("Execution {} transitioned to {}", executionId, waitState);
        executionDAO.update(execution);
        return execution;
    }

    /**
     * Resumes execution from waiting state back to RUNNING.
     *
     * @param executionId the execution ID
     * @return updated execution
     */
    @Transactional
    public Execution resumeExecution(String executionId) {
        Execution execution = executionDAO.findByExecutionId(executionId);
        if (execution == null) {
            throw new IllegalStateException("Execution not found: " + executionId);
        }

        execution.setState(ExecutionState.RUNNING);
        log.info("Execution {} resumed from waiting", executionId);
        executionDAO.update(execution);
        return execution;
    }

    /**
     * Marks execution as succeeded and calculates duration.
     *
     * @param executionId the execution ID
     * @param result the execution result/output
     * @return updated execution
     */
    @Transactional
    public Execution succeedExecution(String executionId, String result) {
        Execution execution = executionDAO.findByExecutionId(executionId);
        if (execution == null) {
            throw new IllegalStateException("Execution not found: " + executionId);
        }

        OffsetDateTime endTime = OffsetDateTime.now();
        execution.setState(ExecutionState.SUCCEEDED);
        execution.setEndTime(endTime);
        execution.setResult(result);

        if (execution.getStartTime() != null) {
            long durationMs = java.time.Duration.between(execution.getStartTime(), endTime).toMillis();
            execution.setDurationMs(durationMs);
        }

        log.info("Execution {} succeeded with duration {}ms", executionId, execution.getDurationMs());
        executionDAO.update(execution);
        return execution;
    }

    /**
     * Marks execution as failed with error details.
     *
     * @param executionId the execution ID
     * @param errorType the error type/class
     * @param errorMessage the error message
     * @return updated execution
     */
    @Transactional
    public Execution failExecution(String executionId, String errorType, String errorMessage) {
        Execution execution = executionDAO.findByExecutionId(executionId);
        if (execution == null) {
            throw new IllegalStateException("Execution not found: " + executionId);
        }

        OffsetDateTime endTime = OffsetDateTime.now();
        execution.setState(ExecutionState.FAILED);
        execution.setEndTime(endTime);
        execution.setErrorType(errorType);
        execution.setErrorMessage(errorMessage);

        if (execution.getStartTime() != null) {
            long durationMs = java.time.Duration.between(execution.getStartTime(), endTime).toMillis();
            execution.setDurationMs(durationMs);
        }

        log.warn("Execution {} failed: type={}, message={}", executionId, errorType, errorMessage);
        executionDAO.update(execution);
        return execution;
    }

    /**
     * Marks execution as cancelled.
     *
     * @param executionId the execution ID
     * @return updated execution
     */
    @Transactional
    public Execution cancelExecution(String executionId) {
        Execution execution = executionDAO.findByExecutionId(executionId);
        if (execution == null) {
            throw new IllegalStateException("Execution not found: " + executionId);
        }

        OffsetDateTime endTime = OffsetDateTime.now();
        execution.setState(ExecutionState.CANCELLED);
        execution.setEndTime(endTime);
        execution.setCancelled(true);

        if (execution.getStartTime() != null) {
            long durationMs = java.time.Duration.between(execution.getStartTime(), endTime).toMillis();
            execution.setDurationMs(durationMs);
        }

        log.info("Execution {} cancelled after {}ms", executionId, execution.getDurationMs());
        executionDAO.update(execution);
        return execution;
    }

    /**
     * Increments tool call counter.
     *
     * @param executionId the execution ID
     * @return updated execution
     */
    @Transactional
    public Execution incrementToolCallCount(String executionId) {
        Execution execution = executionDAO.findByExecutionId(executionId);
        if (execution == null) {
            throw new IllegalStateException("Execution not found: " + executionId);
        }

        int count = execution.getToolCallCount() != null ? execution.getToolCallCount() : 0;
        execution.setToolCallCount(count + 1);
        executionDAO.update(execution);
        return execution;
    }

    /**
     * Increments agent call counter (for A2A tracking).
     *
     * @param executionId the execution ID
     * @return updated execution
     */
    @Transactional
    public Execution incrementAgentCallCount(String executionId) {
        Execution execution = executionDAO.findByExecutionId(executionId);
        if (execution == null) {
            throw new IllegalStateException("Execution not found: " + executionId);
        }

        int count = execution.getAgentCallCount() != null ? execution.getAgentCallCount() : 0;
        execution.setAgentCallCount(count + 1);
        executionDAO.update(execution);
        return execution;
    }

    /**
     * Retrieves execution by ID.
     *
     * @param executionId the execution ID
     * @return the execution, or null if not found
     */
    public Execution getExecution(String executionId) {
        return executionDAO.findByExecutionId(executionId);
    }

    /**
     * Generates a unique execution ID using UUID.
     *
     * @return unique execution ID
     */
    private String generateExecutionId() {
        return "exec-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String resolveGroupNameSnapshot(Agent agent, String groupId) {
        if (agent == null || groupId == null || groupId.isBlank() || agent.getGroups() == null) {
            return null;
        }

        return agent.getGroups().stream()
                .filter(group -> group != null && group.getId() != null && groupId.equals(group.getId().toString()))
                .map(group -> group.getName())
                .findFirst()
                .orElse(null);
    }
}
