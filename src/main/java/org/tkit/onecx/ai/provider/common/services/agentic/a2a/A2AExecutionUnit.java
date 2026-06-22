package org.tkit.onecx.ai.provider.common.services.agentic.a2a;

/**
 * Immutable execution unit representing one group invocation.
 */
public record A2AExecutionUnit(
        String groupId,
        String groupName) {
}
