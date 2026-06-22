package org.tkit.onecx.ai.provider.common.services.agentic.a2a;

import org.tkit.onecx.ai.provider.domain.models.Agent;

/**
 * Builds an execution plan for agent-to-agent (A2A) group orchestration.
 */
public interface A2AGroupPlanner {

    A2AExecutionPlan plan(Agent agent);
}
