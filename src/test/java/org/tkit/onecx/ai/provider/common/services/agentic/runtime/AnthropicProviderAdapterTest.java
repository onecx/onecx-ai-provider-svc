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
import org.mockserver.verify.VerificationTimes;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.Model;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;
import org.tkit.onecx.ai.provider.test.AbstractTest;

import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class AnthropicProviderAdapterTest extends AbstractTest {

    @Inject
    AnthropicProviderAdapter adapter;

    @InjectMockServerClient
    MockServerClient mockServerClient;

    @ConfigProperty(name = "quarkus.mockserver.endpoint")
    String mockServerEndpoint;

    private static final String ANTHROPIC_OK_BODY = """
            {
              "id": "msg_test",
              "type": "message",
              "role": "assistant",
              "model": "claude-3-haiku-20240307",
              "content": [
                { "type": "text", "text": "pong" }
              ],
              "stop_reason": "end_turn",
              "stop_sequence": null,
              "usage": { "input_tokens": 1, "output_tokens": 1 }
            }
            """;

    @AfterEach
    void resetMockserver() {
        try {
            mockServerClient.clear("ANTHROPIC");
        } catch (Exception _) {
            // mockId not existing
        }
    }

    @Test
    void supports_returnsTrueOnlyForAnthropic() {
        assertThat(adapter.supports(ProviderType.ANTHROPIC)).isTrue();
        assertThat(adapter.supports(ProviderType.OPENAI)).isFalse();
        assertThat(adapter.supports(ProviderType.OLLAMA)).isFalse();
    }

    @Test
    void createChatModel_withValidAgent_returnsModel() {
        assertThat(adapter.createChatModel(buildAgent(mockServerEndpoint, "claude-3-haiku-20240307", "sk-ant-test")))
                .isNotNull();
    }

    @Test
    void createChatModel_withoutApiKey_failsClearly() {
        assertThatThrownBy(() -> adapter.createChatModel(buildAgent(mockServerEndpoint, "claude-3-haiku-20240307", "")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Anthropic provider has no API key configured");
    }

    @Test
    void createChatModel_withoutModelIdentifier_failsClearly() {
        assertThatThrownBy(() -> adapter.createChatModel(buildAgent(mockServerEndpoint, "", "sk-ant-test")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Agent model has no model identifier configured");
    }

    @Test
    void health_anthropicResponds_returnsHealthy() {
        stubAnthropicMessages(200, ANTHROPIC_OK_BODY);

        assertThat(adapter.healthCheck(buildProvider(mockServerEndpoint, "sk-ant-test")))
                .isEqualTo(ProviderAdapter.HEALTHY);
    }

    @Test
    void health_anthropicReturns500_returnsUnhealthy() {
        stubAnthropicMessages(500, "");

        assertThat(adapter.healthCheck(buildProvider(mockServerEndpoint, "sk-ant-test")))
                .isEqualTo(ProviderAdapter.UNHEALTHY);
        mockServerClient.verify(request().withMethod("POST").withPath("/messages"),
                VerificationTimes.exactly(3));
    }

    @Test
    void health_missingApiKey_returnsUnhealthy() {
        assertThat(adapter.healthCheck(buildProvider(mockServerEndpoint, ""))).isEqualTo(ProviderAdapter.UNHEALTHY);
    }

    private void stubAnthropicMessages(int statusCode, String body) {
        var resp = response().withStatusCode(statusCode);
        if (!body.isBlank()) {
            resp = resp.withContentType(MediaType.APPLICATION_JSON).withBody(body);
        }
        mockServerClient.when(request().withMethod("POST").withPath("/messages")).withId("ANTHROPIC").respond(resp);
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
        provider.setType(ProviderType.ANTHROPIC);
        provider.setLlmUrl(baseUrl);
        provider.setApiKey(apiKey);
        return provider;
    }
}
