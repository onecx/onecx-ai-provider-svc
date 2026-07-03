package org.tkit.onecx.ai.provider.common.clients;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import gen.org.tkit.onecx.ai.provider.runtime.client.model.ProviderSnapshot;

@RegisterRestClient(configKey = "onecx_ai_runtime")
@RegisterClientHeaders
@Path("/internal/runtime/provider-health")
public interface RuntimeProviderHealthClient {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response getProviderHealthStatus(ProviderHealthRequest request);

    record ProviderHealthRequest(ProviderSnapshot provider) {
    }

    record ProviderHealthStatus(String status) {
    }
}
