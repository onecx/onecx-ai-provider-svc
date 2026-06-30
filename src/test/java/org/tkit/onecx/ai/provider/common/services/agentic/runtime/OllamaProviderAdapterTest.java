package org.tkit.onecx.ai.provider.common.services.agentic.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.MediaType;
import org.mockserver.verify.VerificationTimes;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.Model;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;
import org.tkit.onecx.ai.provider.test.AbstractTest;

import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class OllamaProviderAdapterTest extends AbstractTest {

    @Inject
    OllamaProviderAdapter adapter;

    @InjectMockServerClient
    MockServerClient mockServerClient;

    @ConfigProperty(name = "quarkus.mockserver.endpoint")
    String mockServerEndpoint;

    private static final String OLLAMA_OK_BODY = """
            {"model":"mistral","created_at":"2026-01-01T00:00:00Z",
             "message":{"role":"assistant","content":"pong"},
             "done":true,"done_reason":"stop"}
            """;

    @AfterEach
    void resetMockserver() {
        try {
            mockServerClient.clear("MOCK");
        } catch (Exception _) {
            // mockId not existing
        }
    }

    @Test
    void supports_returnsTrueOnlyForOllama() {
        assertThat(adapter.supports(ProviderType.OLLAMA)).isTrue();
        assertThat(adapter.supports(ProviderType.OPENAI)).isFalse();
        assertThat(adapter.supports(ProviderType.ANTHROPIC)).isFalse();
    }

    @Test
    void createChatModel_withValidAgent_returnsModel() {
        assertThat(adapter.createChatModel(buildAgent(mockServerEndpoint))).isNotNull();
    }

    @Test
    void health_ollamaResponds_returnsHealthy() {
        stubOllamaChat(200, OLLAMA_OK_BODY);

        assertThat(adapter.healthCheck(buildProvider(mockServerEndpoint))).isEqualTo(ProviderAdapter.HEALTHY);
    }

    @Test
    void health_ollamaReturns500_returnsUnhealthy() {
        stubOllamaChat(500, "");

        assertThat(adapter.healthCheck(buildProvider(mockServerEndpoint))).isEqualTo(ProviderAdapter.UNHEALTHY);
        mockServerClient.verify(request().withMethod("POST").withPath("/api/chat"), VerificationTimes.exactly(3));
    }

    @Test
    void health_ollamaUnreachable_returnsUnhealthy() {
        assertThat(adapter.healthCheck(buildProvider("http://127.0.0.1:19999"))).isEqualTo(ProviderAdapter.UNHEALTHY);
    }

    @Test
    void health_providerUrlBlank_returnsUnhealthy() {
        assertThat(adapter.healthCheck(buildProvider(""))).isEqualTo(ProviderAdapter.UNHEALTHY);
    }

    @Test
    void health_nullProvider_returnsUnhealthy() {
        assertThat(adapter.healthCheck(null)).isEqualTo(ProviderAdapter.UNHEALTHY);
    }

    @Test
    void health_providerWithApiKey_returnsHealthy() {
        stubOllamaChat(200, OLLAMA_OK_BODY);

        var provider = buildProvider(mockServerEndpoint);
        provider.setApiKey("Bearer secret-token");

        assertThat(adapter.healthCheck(provider)).isEqualTo(ProviderAdapter.HEALTHY);
    }

    private void stubOllamaChat(int statusCode, String body) {
        var resp = response().withStatusCode(statusCode);
        if (!body.isBlank()) {
            resp = resp.withContentType(MediaType.APPLICATION_JSON).withBody(body);
        }
        mockServerClient.when(request().withMethod("POST").withPath("/api/chat")).withId("MOCK").respond(resp);
    }

    private Agent buildAgent(String baseUrl) {
        var model = new Model();
        model.setProvider(buildProvider(baseUrl));
        model.setModelIdentifier("mistral");

        var agent = new Agent();
        agent.setModel(model);
        return agent;
    }

    private Provider buildProvider(String baseUrl) {
        var provider = new Provider();
        provider.setType(ProviderType.OLLAMA);
        provider.setLlmUrl(baseUrl);
        return provider;
    }
}
