package org.tkit.onecx.ai.provider.common.services.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.common.services.agentic.runtime.ChatModelFactory;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;
import org.tkit.onecx.ai.provider.test.AbstractTest;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ProviderHealthServiceTest extends AbstractTest {

    @Inject
    ProviderHealthService providerHealthService;

    @InjectMock
    ChatModelFactory chatModelFactory;

    @Test
    void healthCheck_routesToProviderRegistry_returnsHealthy() {
        when(chatModelFactory.healthCheck(any())).thenReturn("HEALTHY");

        var provider = buildProvider();
        var result = providerHealthService.getProviderHealthStatus(provider);

        assertThat(result).isEqualTo("HEALTHY");
        verify(chatModelFactory).healthCheck(provider);
    }

    @Test
    void healthCheck_routesToProviderRegistry_returnsUnhealthy() {
        when(chatModelFactory.healthCheck(any())).thenReturn("UNHEALTHY");

        var result = providerHealthService.getProviderHealthStatus(buildProvider());

        assertThat(result).isEqualTo("UNHEALTHY");
    }

    private Provider buildProvider() {
        var provider = new Provider();
        provider.setType(ProviderType.OLLAMA);
        provider.setLlmUrl("http://ollama.local");
        return provider;
    }
}
