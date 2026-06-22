package org.tkit.onecx.ai.provider.domain.models.enums;

/**
 * Execution state transitions for agentic orchestration.
 *
 * State machine:
 * - PENDING -> RUNNING -> (WAITING_TOOL | WAITING_AGENT | SUCCEEDED | FAILED | CANCELLED)
 * - WAITING_TOOL -> RUNNING | FAILED | CANCELLED
 * - WAITING_AGENT -> RUNNING | FAILED | CANCELLED
 */
public enum ExecutionState {
    /**
     * Execution is queued, not yet started.
     */
    PENDING,

    /**
     * Execution is in progress (LLM call, tool orchestration, A2A planning).
     */
    RUNNING,

    /**
     * Execution is waiting for a tool call result.
     */
    WAITING_TOOL,

    /**
     * Execution is waiting for an agent/group to complete.
     */
    WAITING_AGENT,

    /**
     * Execution completed successfully.
     */
    SUCCEEDED,

    /**
     * Execution failed due to error or resource exhaustion.
     */
    FAILED,

    /**
     * Execution was cancelled by client or operator.
     */
    CANCELLED
}
