package org.tkit.onecx.ai.provider.common.services.runtime;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.tkit.onecx.ai.provider.common.services.runtime.dto.RuntimeDtos.RuntimeChatRequest;
import org.tkit.onecx.ai.provider.common.services.runtime.dto.RuntimeDtos.RuntimeChatResponse;

@Path("/internal/runtime")
@RegisterRestClient(configKey = "onecx-ai-provider-runtime")
public interface ProviderRuntimeClient {

    @POST
    @Path("/chat")
    RuntimeChatResponse chat(RuntimeChatRequest request);
}
