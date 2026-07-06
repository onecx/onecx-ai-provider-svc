package org.tkit.onecx.ai.provider.common.services.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.tkit.onecx.ai.provider.common.services.agent.AgentService;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.Model;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;
import org.tkit.onecx.ai.provider.test.AbstractTest;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import gen.org.tkit.onecx.ai.provider.runtime.client.api.RuntimeInternalApi;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.RuntimeChatRequest;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.RuntimeChatResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ChatDispatchServiceTest extends AbstractTest {

    @Inject
    ChatDispatchService chatDispatchService;

    @InjectMock
    AgentService agentService;

    @InjectMock
    @RestClient
    RuntimeInternalApi providerRuntimeClient;

    @Test
    void chat_noAgentFound_returnsNotFound() {
        when(agentService.findAgentByRequestContext(any())).thenReturn(null);

        var request = new ChatRequestDTOV1();
        request.setRequestContext(null);

        try (var response = chatDispatchService.chat(request)) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    void chat_agentFound_routesToRuntime() {
        var provider = new Provider();
        provider.setType(ProviderType.OLLAMA);
        provider.setLlmUrl("http://ollama.local");
        var model = new Model();
        model.setProvider(provider);
        model.setModelIdentifier("mistral");
        var agent = new Agent();
        agent.setModel(model);

        when(agentService.findAgentByRequestContext(any())).thenReturn(agent);
        var request = new ChatRequestDTOV1();
        var runtimeResponse = new RuntimeChatResponse();
        runtimeResponse.setMessage("reply");
        when(providerRuntimeClient.chat(any()))
                .thenReturn(Response.ok(runtimeResponse).build());

        try (var response = chatDispatchService.chat(request)) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        }

        var requestCaptor = ArgumentCaptor.forClass(RuntimeChatRequest.class);
        verify(providerRuntimeClient).chat(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getRootAgent().getModel().getModelIdentifier()).isEqualTo("mistral");
        assertThat(requestCaptor.getValue().getRootAgent().getModel().getProvider().getType()).isEqualTo("OLLAMA");
    }

    @Test
    void chat_agentRuntimeTimeout_returnsGatewayTimeout() {
        var agent = new Agent();
        when(agentService.findAgentByRequestContext(any())).thenReturn(agent);
        var request = new ChatRequestDTOV1();
        var runtimeResponse = new RuntimeChatResponse();
        runtimeResponse.setMessage("timeout");
        when(providerRuntimeClient.chat(any()))
                .thenReturn(Response.status(Response.Status.GATEWAY_TIMEOUT).entity(runtimeResponse).build());

        try (var response = chatDispatchService.chat(request)) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.GATEWAY_TIMEOUT.getStatusCode());
            assertThat(response.getEntity()).isEqualTo("timeout");
        }
    }

    @Test
    void chat_agentRuntimeFailed_returnsBadRequest() {
        var agent = new Agent();
        when(agentService.findAgentByRequestContext(any())).thenReturn(agent);
        var request = new ChatRequestDTOV1();
        var runtimeResponse = new RuntimeChatResponse();
        runtimeResponse.setMessage("failed");
        when(providerRuntimeClient.chat(any()))
                .thenReturn(Response.status(Response.Status.BAD_REQUEST).entity(runtimeResponse).build());

        try (var response = chatDispatchService.chat(request)) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
            assertThat(response.getEntity()).isEqualTo("failed");
        }
    }
}
