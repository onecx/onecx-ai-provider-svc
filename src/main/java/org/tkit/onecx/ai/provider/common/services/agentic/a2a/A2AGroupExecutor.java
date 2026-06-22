package org.tkit.onecx.ai.provider.common.services.agentic.a2a;

import java.util.function.Function;

/**
 * Executes A2A plans and merges unit results.
 */
public interface A2AGroupExecutor {

    String execute(A2AExecutionPlan plan, Function<A2AExecutionUnit, String> unitExecutor);
}
