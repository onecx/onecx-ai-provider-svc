package org.tkit.onecx.ai.provider.common.services.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.MediaType;
import org.tkit.onecx.ai.provider.domain.models.Configuration;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;
import org.tkit.onecx.ai.provider.test.AbstractTest;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.*;
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class OllamaLlmServiceTest extends AbstractTest {

    @Inject
    OllamaLlmService ollamaLlmService;

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
    // ── getHealthStatus ───────────────────────────────────────────────────────

    @Test
    void health_ollamaResponds_returnsHealthy() {
        stubOllamaChat(200, OLLAMA_OK_BODY);

        assertThat(ollamaLlmService.getHealthStatus(buildProvider(mockServerEndpoint))).isEqualTo("HEALTHY");
    }

    @Test
    void health_ollamaReturns500_returnsUnhealthy() {
        stubOllamaChat(500, "");

        assertThat(ollamaLlmService.getHealthStatus(buildProvider(mockServerEndpoint))).isEqualTo("UNHEALTHY");
    }

    @Test
    void health_ollamaUnreachable_returnsUnhealthy() {
        assertThat(ollamaLlmService.getHealthStatus(buildProvider("http://127.0.0.1:19999"))).isEqualTo("UNHEALTHY");
    }

    @Test
    void health_providerUrlBlank_returnsUnhealthy() {
        assertThat(ollamaLlmService.getHealthStatus(buildProvider(""))).isEqualTo("UNHEALTHY");
    }

    @Test
    void health_providerUrlNull_returnsUnhealthy() {
        assertThat(ollamaLlmService.getHealthStatus(buildProvider(null))).isEqualTo("UNHEALTHY");
    }

    @Test
    void health_providerModelNameNull_returnsUnhealthy() {
        var provider = new Provider();
        provider.setType(ProviderType.OLLAMA);
        provider.setLlmUrl(mockServerEndpoint);
        assertThat(ollamaLlmService.getHealthStatus(provider)).isEqualTo("UNHEALTHY");
    }

    @Test
    void health_providerModelNameBlank_returnsUnhealthy() {
        var provider = buildProvider(mockServerEndpoint);
        provider.setModelName("   ");
        assertThat(ollamaLlmService.getHealthStatus(provider)).isEqualTo("UNHEALTHY");
    }

    @Test
    void health_nullProvider_returnsUnhealthy() {
        assertThat(ollamaLlmService.getHealthStatus(null)).isEqualTo("UNHEALTHY");
    }

    @Test
    void health_providerWithApiKey_setsAuthorizationHeader_returnsHealthy() {
        stubOllamaChat(200, OLLAMA_OK_BODY);

        var provider = buildProvider(mockServerEndpoint);
        provider.setApiKey("Bearer secret-token");

        assertThat(ollamaLlmService.getHealthStatus(provider)).isEqualTo("HEALTHY");
    }

    @Test
    void health_providerWithBlankApiKey_returnsHealthy() {
        stubOllamaChat(200, OLLAMA_OK_BODY);

        var provider = buildProvider(mockServerEndpoint);
        provider.setApiKey("   ");

        assertThat(ollamaLlmService.getHealthStatus(provider)).isEqualTo("HEALTHY");
    }

    // ── chat ─────────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("resource")
    void chat_simpleMessage_returnsOk() {
        stubOllamaChat(200, OLLAMA_OK_BODY);

        var response = ollamaLlmService.chat(buildConfiguration(mockServerEndpoint), buildChatRequest("hello", null));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        var dto = (ChatMessageDTOV1) response.getEntity();
        assertThat(dto.getMessage()).isEqualTo("pong");
        assertThat(dto.getType()).isEqualTo(ChatMessageDTOV1.TypeEnum.ASSISTANT);
    }

    @Test
    @SuppressWarnings("resource")
    void chat_withConversationHistory_returnsOk() {
        stubOllamaChat(200, OLLAMA_OK_BODY);

        var userMsg = new ChatMessageDTOV1();
        userMsg.setMessage("previous user msg");
        userMsg.setType(ChatMessageDTOV1.TypeEnum.USER);

        var assistantMsg = new ChatMessageDTOV1();
        assistantMsg.setMessage("previous assistant reply");
        assistantMsg.setType(ChatMessageDTOV1.TypeEnum.ASSISTANT);

        var systemMsg = new ChatMessageDTOV1();
        systemMsg.setMessage("you are a helpful bot");
        systemMsg.setType(ChatMessageDTOV1.TypeEnum.SYSTEM);

        var conversation = new ConversationDTOV1();
        conversation.setHistory(List.of(systemMsg, userMsg, assistantMsg));

        var response = ollamaLlmService.chat(buildConfiguration(mockServerEndpoint),
                buildChatRequest("follow-up", conversation));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    @SuppressWarnings("resource")
    void chat_withConversationHistoryNull_returnsOk() {
        stubOllamaChat(200, OLLAMA_OK_BODY);

        var conversation = new ConversationDTOV1();
        conversation.setHistory(null);

        var response = ollamaLlmService.chat(
                buildConfiguration(mockServerEndpoint),
                buildChatRequest("hello", conversation));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    @SuppressWarnings("resource")
    void chat_withConversationHistoryEmpty_returnsOk() {
        stubOllamaChat(200, OLLAMA_OK_BODY);

        var conversation = new ConversationDTOV1();
        conversation.setHistory(List.of());

        var response = ollamaLlmService.chat(
                buildConfiguration(mockServerEndpoint),
                buildChatRequest("hello", conversation));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    @SuppressWarnings("resource")
    void chat_modelReturnsError_returnsBadRequest() {
        stubOllamaChat(500, "");

        var response = ollamaLlmService.chat(buildConfiguration(mockServerEndpoint), buildChatRequest("hello", null));

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @SuppressWarnings("resource")
    void chat_modelUnreachable_returnsBadRequest() {
        var response = ollamaLlmService.chat(buildConfiguration("http://127.0.0.1:19999"), buildChatRequest("hello", null));

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void stubOllamaChat(int statusCode, String body) {
        var resp = response().withStatusCode(statusCode);
        if (!body.isBlank()) {
            resp = resp.withContentType(MediaType.APPLICATION_JSON).withBody(body);
        }
        mockServerClient.when(request().withMethod("POST").withPath("/api/chat")).withId("MOCK").respond(resp);
    }

    private Provider buildProvider(String baseUrl) {
        var provider = new Provider();
        provider.setType(ProviderType.OLLAMA);
        provider.setLlmUrl(baseUrl);
        provider.setModelName("mistral");
        return provider;
    }

    private Configuration buildConfiguration(String baseUrl) {
        var config = new Configuration();
        config.setProvider(buildProvider(baseUrl));
        return config;
    }

    private ChatRequestDTOV1 buildChatRequest(String message, ConversationDTOV1 conversation) {
        var chatMsg = new ChatMessageDTOV1();
        chatMsg.setMessage(message);
        chatMsg.setType(ChatMessageDTOV1.TypeEnum.USER);

        var request = new ChatRequestDTOV1();
        request.setChatMessage(chatMsg);
        request.setConversation(conversation);
        return request;
    }
}
