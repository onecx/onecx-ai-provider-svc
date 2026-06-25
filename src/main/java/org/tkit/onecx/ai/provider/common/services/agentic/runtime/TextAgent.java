package org.tkit.onecx.ai.provider.common.services.agentic.runtime;

public interface TextAgent {

    @dev.langchain4j.agentic.Agent
    String invoke();
}
