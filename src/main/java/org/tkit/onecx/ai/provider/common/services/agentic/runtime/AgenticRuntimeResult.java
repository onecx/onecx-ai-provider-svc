package org.tkit.onecx.ai.provider.common.services.agentic.runtime;

public record AgenticRuntimeResult(
        String executionId,
        String responseText,
        boolean successful,
        AgenticRuntimeStatus status) {

    public AgenticRuntimeResult(String executionId, String responseText, boolean successful) {
        this(executionId, responseText, successful,
                successful ? AgenticRuntimeStatus.SUCCESS : AgenticRuntimeStatus.FAILED);
    }
}
