package org.tkit.onecx.ai.provider.common.services.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.IntStream;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.common.models.DispatchConfig;
import org.tkit.onecx.ai.provider.common.services.mcp.McpTool;
import org.tkit.onecx.ai.provider.common.services.mcp.McpToolRegistry;
import org.tkit.onecx.ai.provider.domain.models.Configuration;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;
import org.tkit.onecx.ai.provider.test.AbstractTest;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatMessageDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class OllamaLlmServiceBranchTest extends AbstractTest {

    @Test
    void chat_withToolSpecifications_andRetryAfterToolExecutionError_returnsOk() {
        var service = new TestableOllamaLlmService(dispatchConfig(3), registryWithTools(1));
        service.modelResponses.add(chatResponse("initial"));
        service.modelResponses.add(chatResponse("final"));
        service.toolExecutionFlags.add(true);
        service.toolExecutionFlags.add(true);
        service.toolExecutionFlags.add(false);
        service.executeToolResults.add(new RuntimeException("tool failed"));
        service.executeToolResults.add(List.of(new AiMessage("tool-result")));

        try (Response response = service.chat(configuration(), chatRequest("hello"))) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
            assertThat(((ChatMessageDTOV1) response.getEntity()).getMessage()).isEqualTo("final");
            assertThat(service.capturedRequests).hasSize(2);
        }
    }

    @Test
    void chat_followUpResponseNull_returnsBadRequest() {
        var service = new TestableOllamaLlmService(dispatchConfig(3), registryWithTools(1));
        service.modelResponses.add(chatResponse("initial"));
        service.modelResponses.add(null);
        service.toolExecutionFlags.add(true);
        service.executeToolResults.add(List.of(new AiMessage("tool-result")));

        try (Response response = service.chat(configuration(), chatRequest("hello"))) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
            assertThat(response.getEntity()).isEqualTo("Failed to get follow-up response from model during tool execution");
        }
    }

    @Test
    void chat_reachesMaxIterations_returnsOk() {
        var service = new TestableOllamaLlmService(dispatchConfig(1), registryWithTools(1));
        service.modelResponses.add(chatResponse("initial"));
        service.modelResponses.add(chatResponse("after-tool"));
        service.toolExecutionFlags.add(true);
        service.toolExecutionFlags.add(true);
        service.executeToolResults.add(List.of(new AiMessage("tool-result")));

        try (Response response = service.chat(configuration(), chatRequest("hello"))) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
            assertThat(((ChatMessageDTOV1) response.getEntity()).getMessage()).isEqualTo("after-tool");
        }
    }

    @Test
    void chat_unexpectedException_returnsBadRequest() {
        var service = new TestableOllamaLlmService(dispatchConfig(3), McpToolRegistry.empty());
        service.modelResponses.add(new RuntimeException("boom"));

        try (Response response = service.chat(configuration(), chatRequest("hello"))) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
            assertThat(response.getEntity()).isEqualTo("Unexpected error: boom");
        }
    }

    @Test
    void getHealthStatus_whenModelChatThrows_returnsUnhealthy() {
        var service = new TestableOllamaLlmService(dispatchConfig(3), McpToolRegistry.empty());
        service.modelResponses.add(new RuntimeException("boom"));

        assertThat(service.getHealthStatus(provider())).isEqualTo("UNHEALTHY");
    }

    @Test
    void getHealthStatus_whenModelResponseNull_returnsUnhealthy() {
        var service = new TestableOllamaLlmService(dispatchConfig(3), McpToolRegistry.empty());
        service.modelResponses.add(null);

        assertThat(service.getHealthStatus(provider())).isEqualTo("UNHEALTHY");
    }

    @Test
    void getHealthStatus_whenAiMessageNull_returnsUnhealthy() {
        var service = new TestableOllamaLlmService(dispatchConfig(3), McpToolRegistry.empty());
        ChatResponse response = mock(ChatResponse.class);
        when(response.aiMessage()).thenReturn(null);
        service.modelResponses.add(response);

        assertThat(service.getHealthStatus(provider())).isEqualTo("UNHEALTHY");
    }

    private static ChatResponse chatResponse(String text) {
        ChatResponse response = mock(ChatResponse.class);
        when(response.aiMessage()).thenReturn(new AiMessage(text));
        return response;
    }

    private static Configuration configuration() {
        Configuration configuration = new Configuration();
        configuration.setProvider(provider());
        return configuration;
    }

    private static Provider provider() {
        Provider provider = new Provider();
        provider.setType(ProviderType.OLLAMA);
        provider.setLlmUrl("http://localhost:11434");
        provider.setModelName("mistral");
        return provider;
    }

    private static ChatRequestDTOV1 chatRequest(String message) {
        ChatMessageDTOV1 chatMessage = new ChatMessageDTOV1();
        chatMessage.setType(ChatMessageDTOV1.TypeEnum.USER);
        chatMessage.setMessage(message);

        ChatRequestDTOV1 request = new ChatRequestDTOV1();
        request.setChatMessage(chatMessage);
        return request;
    }

    private static DispatchConfig dispatchConfig(long maxIterations) {
        DispatchConfig dispatchConfig = mock(DispatchConfig.class);
        DispatchConfig.ProviderConfig providerConfig = mock(DispatchConfig.ProviderConfig.class);
        DispatchConfig.MCPConfig mcpConfig = mock(DispatchConfig.MCPConfig.class);

        when(providerConfig.timeout()).thenReturn(1L);
        when(providerConfig.logRequests()).thenReturn(false);
        when(providerConfig.logResponse()).thenReturn(false);

        when(mcpConfig.maxIterations()).thenReturn(maxIterations);
        when(mcpConfig.maxTimeout()).thenReturn(1L);
        when(mcpConfig.logRequests()).thenReturn(false);
        when(mcpConfig.logResponse()).thenReturn(false);
        when(mcpConfig.maxToolExecutionRetries()).thenReturn(1L);
        when(mcpConfig.toolExecutionRetryDelay()).thenReturn(0L);

        when(dispatchConfig.providerConfig()).thenReturn(providerConfig);
        when(dispatchConfig.mcpConfig()).thenReturn(mcpConfig);
        return dispatchConfig;
    }

    private static McpToolRegistry registryWithTools(int count) {
        List<McpTool> tools = IntStream.range(0, count)
                .mapToObj(i -> new McpTool(
                        "http://mcp-" + i,
                        ToolSpecification.builder()
                                .name("tool-" + i)
                                .description("test-tool-" + i)
                                .parameters(JsonObjectSchema.builder().build())
                                .build(),
                        mock(McpClient.class)))
                .toList();
        return new McpToolRegistry(tools);
    }

    static class TestableOllamaLlmService extends OllamaLlmService {
        final Queue<Object> modelResponses = new LinkedList<>();
        final Queue<Boolean> toolExecutionFlags = new LinkedList<>();
        final Queue<Object> executeToolResults = new LinkedList<>();
        final List<ChatRequest> capturedRequests = new ArrayList<>();
        private final McpToolRegistry toolRegistry;

        TestableOllamaLlmService(DispatchConfig dispatchConfig, McpToolRegistry toolRegistry) {
            this.dispatchConfig = dispatchConfig;
            this.toolRegistry = toolRegistry;
        }

        @Override
        protected McpToolRegistry createToolRegistry(Configuration aiConfiguration) {
            return toolRegistry;
        }

        @Override
        protected boolean hasToolExecutionRequests(ChatResponse response) {
            return toolExecutionFlags.isEmpty() ? false : toolExecutionFlags.remove();
        }

        @Override
        protected List<ChatMessage> executeToolRequests(ChatResponse response, McpToolRegistry toolRegistry) {
            Object next = executeToolResults.remove();
            if (next instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            return (List<ChatMessage>) next;
        }

        @Override
        protected ChatResponse modelChatRequestWithRetries(ChatModel chatModel, ChatRequest chatRequest) {
            capturedRequests.add(chatRequest);
            Object next = modelResponses.remove();
            if (next instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            return (ChatResponse) next;
        }
    }
}
