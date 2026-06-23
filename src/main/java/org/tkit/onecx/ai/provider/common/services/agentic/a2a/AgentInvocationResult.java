package org.tkit.onecx.ai.provider.common.services.agentic.a2a;

/**
 * Result of invoking a single agent, including the local execution ID if one was created.
 */
public record AgentInvocationResult(
        String executionId,
        String responseText,
        boolean successful) {
}
