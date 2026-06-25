package org.tkit.onecx.ai.provider.common.services.provider;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.tkit.onecx.ai.provider.common.services.agentic.runtime.ChatModelFactory;
import org.tkit.onecx.ai.provider.domain.models.Provider;

@ApplicationScoped
public class ProviderHealthService {

    @Inject
    ChatModelFactory chatModelFactory;

    public String getProviderHealthStatus(Provider provider) {
        return chatModelFactory.healthCheck(provider);
    }
}
