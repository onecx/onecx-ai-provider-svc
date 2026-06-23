package org.tkit.onecx.ai.provider.common.services.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.common.services.agent.AgentService;
import org.tkit.onecx.ai.provider.common.services.agentic.a2a.A2AOrchestrationService;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.Model;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;
import org.tkit.onecx.ai.provider.test.AbstractTest;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class LlmServiceFactoryTest extends AbstractTest {

    @Inject
    LlmServiceFactory llmServiceFactory;

    @InjectMock
    OllamaLlmService ollamaLlmService;

    @InjectMock
    AgentService agentService;

    @InjectMock
    A2AOrchestrationService a2aOrchestrationService;

    // ── getProviderHealthStatus ───────────────────────────────────────────────

    @Test
    void healthCheck_routesToOllamaService_returnsHealthy() {
        when(ollamaLlmService.getHealthStatus(any())).thenReturn("HEALTHY");

        var provider = buildProvider();
        var result = llmServiceFactory.getProviderHealthStatus(provider);

        assertThat(result).isEqualTo("HEALTHY");
        verify(ollamaLlmService).getHealthStatus(provider);
    }

    @Test
    void healthCheck_routesToOllamaService_returnsUnhealthy() {
        when(ollamaLlmService.getHealthStatus(any())).thenReturn("UNHEALTHY");

        var result = llmServiceFactory.getProviderHealthStatus(buildProvider());

        assertThat(result).isEqualTo("UNHEALTHY");
    }

    // ── chat ─────────────────────────────────────────────────────────────────

    @Test
    void chat_noAgentFound_returnsNotFound() {
        when(agentService.findAgentByRequestContext(any())).thenReturn(null);

        var request = new ChatRequestDTOV1();
        request.setRequestContext(null);

        try (var response = llmServiceFactory.chat(request)) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    void chat_agentFound_routesToOllamaService() {
        var provider = buildProvider();
        var model = new Model();
        model.setProvider(provider);
        model.setModelIdentifier("mistral");
        var agent = new Agent();
        agent.setModel(model);

        when(agentService.findAgentByRequestContext(any())).thenReturn(agent);
        var request = new ChatRequestDTOV1();

        try (var mockedResponse = Response.ok("reply").build()) {
            when(a2aOrchestrationService.invokeRoot(any(), any())).thenReturn(mockedResponse);

            try (var response = llmServiceFactory.chat(request)) {
                assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
            }
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Provider buildProvider() {
        var provider = new Provider();
        provider.setType(ProviderType.OLLAMA);
        provider.setLlmUrl("http://ollama.local");
        return provider;
    }
}
