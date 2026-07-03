package org.tkit.onecx.ai.provider.common.services.provider;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;
import org.tkit.onecx.ai.provider.test.AbstractTest;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ProviderHealthServiceTest extends AbstractTest {

    @Inject
    ProviderHealthService providerHealthService;

    @Test
    void healthCheck_returnsHealthy_whenProviderHasType() {
        var provider = buildProvider();
        var result = providerHealthService.getProviderHealthStatus(provider);

        assertThat(result).isEqualTo("HEALTHY");
    }

    @Test
    void healthCheck_returnsUnhealthy_whenProviderMissingType() {
        var result = providerHealthService.getProviderHealthStatus(new Provider());

        assertThat(result).isEqualTo("UNHEALTHY");
    }

    private Provider buildProvider() {
        var provider = new Provider();
        provider.setType(ProviderType.OLLAMA);
        provider.setLlmUrl("http://ollama.local");
        return provider;
    }
}
