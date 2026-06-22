package org.tkit.onecx.ai.provider.domain.models;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import org.hibernate.annotations.TenantId;
import org.tkit.onecx.ai.provider.domain.models.enums.ExecutionState;
import org.tkit.quarkus.jpa.models.TraceableEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * Execution entity for tracking agentic orchestration state and lifecycle.
 *
 * Tracks:
 * - Execution state (PENDING, RUNNING, WAITING_TOOL, WAITING_AGENT, SUCCEEDED, FAILED, CANCELLED)
 * - Correlation metadata (tenantId, executionId, agentId, groupId)
 * - Timing information (start, end, duration)
 * - Error details and audit payload excerpts
 */
@NamedQuery(name = "Execution.findByExecutionId", query = "SELECT e FROM Execution e WHERE e.executionId = :executionId")
@Getter
@Setter
@Entity
@Table(name = "EXECUTION")
public class Execution extends TraceableEntity {

    @TenantId
    @Column(name = "TENANT_ID")
    private String tenantId;

    /**
     * Unique execution ID for request/response correlation and tracing.
     */
    @Column(name = "EXECUTION_ID", nullable = false, length = 50)
    private String executionId;

    /**
     * Current state of the execution.
     */
    @Column(name = "STATE", nullable = false)
    @Enumerated(EnumType.STRING)
    private ExecutionState state;

    /**
     * Reference to the Agent being executed.
     */
    @ManyToOne
    @JoinColumn(name = "AGENT_ID")
    private Agent agent;

    /**
     * Agent ID snapshot (for reference if agent is deleted).
     */
    @Column(name = "AGENT_ID_SNAPSHOT")
    private String agentIdSnapshot;

    /**
     * Agent version at execution time (optimistic locking).
     */
    @Column(name = "AGENT_VERSION_SNAPSHOT")
    private Integer agentVersionSnapshot;

    /**
     * Group ID if execution is scoped to a specific agent group.
     */
    @Column(name = "GROUP_ID")
    private String groupId;

    /**
     * Group name snapshot.
     */
    @Column(name = "GROUP_NAME_SNAPSHOT")
    private String groupNameSnapshot;

    /**
     * Timestamp when execution started.
     */
    @Column(name = "START_TIME")
    private OffsetDateTime startTime;

    /**
     * Timestamp when execution ended (terminal state reached).
     */
    @Column(name = "END_TIME")
    private OffsetDateTime endTime;

    /**
     * Duration in milliseconds (calculated from start/end time).
     */
    @Column(name = "DURATION_MS")
    private Long durationMs;

    /**
     * Number of tool invocations during this execution.
     */
    @Column(name = "TOOL_CALL_COUNT")
    private Integer toolCallCount;

    /**
     * Number of A2A agent calls during this execution.
     */
    @Column(name = "AGENT_CALL_COUNT")
    private Integer agentCallCount;

    /**
     * Error message if execution failed.
     */
    @Column(name = "ERROR_MESSAGE", length = 1000)
    private String errorMessage;

    /**
     * Error type/class if execution failed.
     */
    @Column(name = "ERROR_TYPE", length = 255)
    private String errorType;

    /**
     * Execution result/output (summary or full response).
     */
    @Column(name = "RESULT", columnDefinition = "TEXT")
    private String result;

    /**
     * Request payload excerpt (for auditability).
     */
    @Column(name = "REQUEST_EXCERPT", columnDefinition = "TEXT")
    private String requestExcerpt;

    /**
     * Flag indicating if execution was cancelled by user/operator.
     */
    @Column(name = "CANCELLED")
    private Boolean cancelled;
}
