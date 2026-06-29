package org.tkit.onecx.ai.provider.common.services.agentic.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.MediaType;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.Model;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;
import org.tkit.onecx.ai.provider.test.AbstractTest;

import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class OpenAiProviderAdapterTest extends AbstractTest {

    @Inject
    OpenAiProviderAdapter adapter;

    @InjectMockServerClient
    MockServerClient mockServerClient;

    @ConfigProperty(name = "quarkus.mockserver.endpoint")
    String mockServerEndpoint;

    private static final String OPENAI_OK_BODY = """
            {
              "id": "chatcmpl-test",
              "object": "chat.completion",
              "created": 1780000000,
              "model": "gpt-4o-mini",
              "choices": [
                {
                  "index": 0,
                  "message": { "role": "assistant", "content": "pong" },
                  "finish_reason": "stop"
                }
              ],
              "usage": { "prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 2 }
            }
            """;

    @AfterEach
    void resetMockserver() {
        try {
            mockServerClient.clear("OPENAI");
        } catch (Exception _) {
            // mockId not existing
        }
    }

    @Test
    void supports_returnsTrueOnlyForOpenAi() {
        assertThat(adapter.supports(ProviderType.OPENAI)).isTrue();
        assertThat(adapter.supports(ProviderType.OLLAMA)).isFalse();
    }

    @Test
    void createChatModel_withValidAgent_returnsModel() {
        assertThat(adapter.createChatModel(buildAgent(mockServerEndpoint, "gpt-4o-mini", "sk-test"))).isNotNull();
    }

    @Test
    void createChatModel_withoutApiKey_failsClearly() {
        assertThatThrownBy(() -> adapter.createChatModel(buildAgent(mockServerEndpoint, "gpt-4o-mini", "")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OpenAI provider has no API key configured");
    }

    @Test
    void createChatModel_withoutModelIdentifier_failsClearly() {
        assertThatThrownBy(() -> adapter.createChatModel(buildAgent(mockServerEndpoint, "", "sk-test")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agent model has no model identifier configured");
    }

    @Test
    void health_openAiResponds_returnsHealthy() {
        stubOpenAiChat(200, OPENAI_OK_BODY);

        assertThat(adapter.healthCheck(buildProvider(mockServerEndpoint, "sk-test"))).isEqualTo(ProviderAdapter.HEALTHY);
    }

    @Test
    void health_openAiReturns500_returnsUnhealthy() {
        stubOpenAiChat(500, "");

        assertThat(adapter.healthCheck(buildProvider(mockServerEndpoint, "sk-test"))).isEqualTo(ProviderAdapter.UNHEALTHY);
    }

    @Test
    void health_missingApiKey_returnsUnhealthy() {
        assertThat(adapter.healthCheck(buildProvider(mockServerEndpoint, ""))).isEqualTo(ProviderAdapter.UNHEALTHY);
    }

    private void stubOpenAiChat(int statusCode, String body) {
        var resp = response().withStatusCode(statusCode);
        if (!body.isBlank()) {
            resp = resp.withContentType(MediaType.APPLICATION_JSON).withBody(body);
        }
        mockServerClient.when(request().withMethod("POST").withPath(".*chat/completions")).withId("OPENAI").respond(resp);
    }

    private Agent buildAgent(String baseUrl, String modelIdentifier, String apiKey) {
        var model = new Model();
        model.setProvider(buildProvider(baseUrl, apiKey));
        model.setModelIdentifier(modelIdentifier);

        var agent = new Agent();
        agent.setModel(model);
        return agent;
    }

    private Provider buildProvider(String baseUrl, String apiKey) {
        var provider = new Provider();
        provider.setType(ProviderType.OPENAI);
        provider.setLlmUrl(baseUrl);
        provider.setApiKey(apiKey);
        return provider;
    }
}
