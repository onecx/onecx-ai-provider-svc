package org.tkit.onecx.ai.provider.common.services.agentic.runtime;

public record AgenticRuntimeResult(
        String executionId,
        String responseText,
        boolean successful) {
}
