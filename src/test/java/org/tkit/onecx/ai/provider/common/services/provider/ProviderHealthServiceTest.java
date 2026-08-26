package org.tkit.onecx.ai.provider.common.services.provider;

import static jakarta.ws.rs.core.Response.Status.OK;
import static jakarta.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;
import org.tkit.onecx.ai.provider.rs.external.v1.mappers.RuntimeSnapshotMapper;
import org.tkit.onecx.ai.provider.test.AbstractTest;

import gen.org.tkit.onecx.ai.provider.runtime.client.api.RuntimeInternalApi;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.ProviderHealthRequest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ProviderHealthServiceTest extends AbstractTest {

    @Inject
    ProviderHealthService providerHealthService;

    @InjectMock
    @RestClient
    RuntimeInternalApi runtimeProviderHealthClient;

    @Inject
    RuntimeSnapshotMapper runtimeSnapshotMapper;

    @Test
    void getProviderHealthStatus_returnsHealthy_whenRuntimeReturnsOk() {
        when(runtimeProviderHealthClient.getProviderHealthStatus(any(ProviderHealthRequest.class)))
                .thenReturn(Response.ok().status(OK.getStatusCode()).build());

        var provider = new Provider();
        provider.setId("p1");
        provider.setType(ProviderType.OLLAMA);
        provider.setLlmUrl("http://ollama:11434");

        assertThat(providerHealthService.getProviderHealthStatus(provider)).isEqualTo("HEALTHY");
    }

    @Test
    void getProviderHealthStatus_returnsUnhealthy_whenRuntimeReturnsNonOk() {
        when(runtimeProviderHealthClient.getProviderHealthStatus(any(ProviderHealthRequest.class)))
                .thenReturn(Response.status(SERVICE_UNAVAILABLE.getStatusCode()).build());

        var provider = new Provider();
        provider.setId("p1");
        provider.setType(ProviderType.OLLAMA);

        assertThat(providerHealthService.getProviderHealthStatus(provider)).isEqualTo("UNHEALTHY");
    }

    @Test
    void getProviderHealthStatus_returnsUnhealthy_whenRuntimeThrows() {
        when(runtimeProviderHealthClient.getProviderHealthStatus(any(ProviderHealthRequest.class)))
                .thenThrow(new RuntimeException("connection refused"));

        var provider = new Provider();
        provider.setId("p1");
        provider.setType(ProviderType.OPENAI);

        assertThat(providerHealthService.getProviderHealthStatus(provider)).isEqualTo("UNHEALTHY");
    }

    @Test
    void getProviderHealthStatus_returnsUnhealthy_whenProviderIsNull() {
        assertThat(providerHealthService.getProviderHealthStatus(null)).isEqualTo("UNHEALTHY");
    }

    @Test
    void getProviderHealthStatus_returnsUnhealthy_whenProviderTypeIsNull() {
        var provider = new Provider();
        provider.setId("p1");
        provider.setType(null);

        assertThat(providerHealthService.getProviderHealthStatus(provider)).isEqualTo("UNHEALTHY");
    }
}
