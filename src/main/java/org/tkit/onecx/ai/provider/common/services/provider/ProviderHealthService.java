package org.tkit.onecx.ai.provider.common.services.provider;

import jakarta.enterprise.context.ApplicationScoped;

import org.tkit.onecx.ai.provider.domain.models.Provider;

@ApplicationScoped
public class ProviderHealthService {

    public String getProviderHealthStatus(Provider provider) {
        if (provider == null || provider.getType() == null) {
            return "UNHEALTHY";
        }
        return "HEALTHY";
    }
}
