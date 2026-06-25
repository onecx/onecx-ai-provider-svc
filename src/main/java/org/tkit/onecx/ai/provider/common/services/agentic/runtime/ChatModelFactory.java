package org.tkit.onecx.ai.provider.common.services.agentic.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;

import dev.langchain4j.model.chat.ChatModel;

@ApplicationScoped
public class ChatModelFactory {

    @Inject
    Instance<ProviderAdapter> providerAdapters;

    public ChatModel createChatModel(Agent agent) {
        if (agent == null || agent.getModel() == null || agent.getModel().getProvider() == null) {
            throw new IllegalArgumentException("Agent has no associated model or provider");
        }
        return adapterFor(agent.getModel().getProvider()).createChatModel(agent);
    }

    public String healthCheck(Provider provider) {
        return adapterFor(provider).healthCheck(provider);
    }

    private ProviderAdapter adapterFor(Provider provider) {
        if (provider == null || provider.getType() == null) {
            throw new IllegalArgumentException("Provider has no type configured");
        }
        ProviderType type = provider.getType();
        for (ProviderAdapter adapter : providerAdapters) {
            if (adapter.supports(type)) {
                return adapter;
            }
        }
        throw new IllegalArgumentException("Provider type not supported by current runtime: " + type);
    }
}
