package org.tkit.onecx.ai.provider.common.services.agentic.a2a;

import java.util.List;

/**
 * Immutable A2A plan with selected strategy and ordered execution units.
 */
public record A2AExecutionPlan(
        A2AStrategy strategy,
        List<A2AExecutionUnit> units) {
}
