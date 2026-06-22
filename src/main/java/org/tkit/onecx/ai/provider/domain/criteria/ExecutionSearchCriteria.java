package org.tkit.onecx.ai.provider.domain.criteria;

import java.time.OffsetDateTime;

import org.tkit.onecx.ai.provider.domain.models.enums.ExecutionState;

import lombok.Getter;
import lombok.Setter;

/**
 * Search criteria for querying executions.
 *
 * Supports filtering by state, agent, time range, and status flags.
 */
@Getter
@Setter
public class ExecutionSearchCriteria {

    /**
     * Execution ID for exact match.
     */
    private String executionId;

    /**
     * Agent ID for filtering.
     */
    private Object agentId;

    /**
     * Agent name for filtering.
     */
    private String agentName;

    /**
     * Current execution state.
     */
    private ExecutionState state;

    /**
     * Group ID for filtering.
     */
    private String groupId;

    /**
     * Start time range - from.
     */
    private OffsetDateTime startTimeFrom;

    /**
     * Start time range - to.
     */
    private OffsetDateTime startTimeTo;

    /**
     * End time range - from.
     */
    private OffsetDateTime endTimeFrom;

    /**
     * End time range - to.
     */
    private OffsetDateTime endTimeTo;

    /**
     * Filter for only terminal states (SUCCEEDED, FAILED, CANCELLED).
     */
    private Boolean isTerminal;

    /**
     * Filter for only running/pending states.
     */
    private Boolean isRunning;

    /**
     * Filter for only failed executions.
     */
    private Boolean isFailed;
}
