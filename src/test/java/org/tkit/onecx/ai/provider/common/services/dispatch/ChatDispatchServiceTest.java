package org.tkit.onecx.ai.provider.common.services.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.common.services.agent.AgentService;
import org.tkit.onecx.ai.provider.common.services.agentic.runtime.AgenticRuntimeResult;
import org.tkit.onecx.ai.provider.common.services.agentic.runtime.AgenticRuntimeService;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.Model;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;
import org.tkit.onecx.ai.provider.test.AbstractTest;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ChatDispatchServiceTest extends AbstractTest {

    @Inject
    ChatDispatchService chatDispatchService;

    @InjectMock
    AgentService agentService;

    @InjectMock
    AgenticRuntimeService agenticRuntimeService;

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
    void chat_agentFound_routesToAgenticRuntime() {
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
        when(agenticRuntimeService.invokeRoot(any(), any()))
                .thenReturn(new AgenticRuntimeResult("exec-123", "reply", true));

        try (var response = chatDispatchService.chat(request)) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
            assertThat(response.getHeaderString("X-Execution-Id")).isEqualTo("exec-123");
        }
    }
}
