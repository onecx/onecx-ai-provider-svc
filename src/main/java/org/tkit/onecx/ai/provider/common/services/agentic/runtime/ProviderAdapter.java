package org.tkit.onecx.ai.provider.common.services.agentic.runtime;

import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;

import dev.langchain4j.model.chat.ChatModel;

public interface ProviderAdapter {

    String HEALTHY = "HEALTHY";
    String UNHEALTHY = "UNHEALTHY";

    boolean supports(ProviderType type);

    ChatModel createChatModel(Agent agent);

    String healthCheck(Provider provider);
}
