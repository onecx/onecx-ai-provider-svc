package org.tkit.onecx.ai.provider.common.services.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.common.clients.RuntimeProviderHealthClient;
import org.tkit.onecx.ai.provider.common.clients.RuntimeProviderHealthClient.ProviderHealthRequest;
import org.tkit.onecx.ai.provider.common.clients.RuntimeProviderHealthClient.ProviderHealthStatus;
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
    @RestClient
    RuntimeProviderHealthClient runtimeProviderHealthClient;

    @Test
    void healthCheck_returnsRuntimeStatus_whenProviderHasType() {
        var provider = buildProvider();
        when(runtimeProviderHealthClient.getProviderHealthStatus(any(ProviderHealthRequest.class)))
                .thenReturn(Response.ok(new ProviderHealthStatus("HEALTHY")).build());

        var result = providerHealthService.getProviderHealthStatus(provider);

        assertThat(result).isEqualTo("HEALTHY");
        verify(runtimeProviderHealthClient).getProviderHealthStatus(any(ProviderHealthRequest.class));
    }

    @Test
    void healthCheck_returnsUnhealthy_whenProviderMissingType() {
        var result = providerHealthService.getProviderHealthStatus(new Provider());

        assertThat(result).isEqualTo("UNHEALTHY");
        verifyNoInteractions(runtimeProviderHealthClient);
    }

    @Test
    void healthCheck_returnsUnhealthy_whenRuntimeCallFails() {
        var provider = buildProvider();
        when(runtimeProviderHealthClient.getProviderHealthStatus(any(ProviderHealthRequest.class)))
                .thenReturn(Response.serverError().build());

        var result = providerHealthService.getProviderHealthStatus(provider);

        assertThat(result).isEqualTo("UNHEALTHY");
    }

    private Provider buildProvider() {
        var provider = new Provider();
        provider.setType(ProviderType.OLLAMA);
        provider.setLlmUrl("http://ollama.local");
        return provider;
    }
}
