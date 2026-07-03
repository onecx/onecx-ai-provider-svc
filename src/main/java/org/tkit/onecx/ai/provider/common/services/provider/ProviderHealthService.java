package org.tkit.onecx.ai.provider.common.services.provider;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.tkit.onecx.ai.provider.common.clients.RuntimeProviderHealthClient;
import org.tkit.onecx.ai.provider.common.clients.RuntimeProviderHealthClient.ProviderHealthRequest;
import org.tkit.onecx.ai.provider.common.clients.RuntimeProviderHealthClient.ProviderHealthStatus;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.rs.external.v1.mappers.RuntimeSnapshotMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ProviderHealthService {

    private static final String UNHEALTHY = "UNHEALTHY";

    @Inject
    @RestClient
    RuntimeProviderHealthClient runtimeProviderHealthClient;

    @Inject
    RuntimeSnapshotMapper runtimeSnapshotMapper;

    public String getProviderHealthStatus(Provider provider) {
        if (provider == null || provider.getType() == null) {
            return UNHEALTHY;
        }
        try (Response response = runtimeProviderHealthClient
                .getProviderHealthStatus(new ProviderHealthRequest(runtimeSnapshotMapper.mapProvider(provider)))) {
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                log.warn("Runtime provider health check failed with HTTP status {}", response.getStatus());
                return UNHEALTHY;
            }
            var status = response.readEntity(ProviderHealthStatus.class);
            return status != null && status.status() != null ? status.status() : UNHEALTHY;
        } catch (Exception ex) {
            log.warn("Error invoking runtime provider health API: {}", ex.getMessage());
            log.debug("Runtime provider health API failure details", ex);
            return UNHEALTHY;
        }
    }
}
