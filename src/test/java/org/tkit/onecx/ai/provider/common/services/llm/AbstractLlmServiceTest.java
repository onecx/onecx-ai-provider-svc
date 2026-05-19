package org.tkit.onecx.ai.provider.common.services.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.common.models.DispatchConfig;
import org.tkit.onecx.ai.provider.common.services.mcp.McpTool;
import org.tkit.onecx.ai.provider.common.services.mcp.McpToolRegistry;
import org.tkit.onecx.ai.provider.domain.models.Configuration;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.test.AbstractTest;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatMessageDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class AbstractLlmServiceTest extends AbstractTest {

    @Test
    void hasToolExecutionRequests_returnsFalse_whenAiMessageNull() {
        var service = testService();
        ChatResponse response = mock(ChatResponse.class);
        when(response.aiMessage()).thenReturn(null);

        assertThat(service.callHasToolExecutionRequests(response)).isFalse();
    }

    @Test
    void hasToolExecutionRequests_returnsFalse_whenFlagFalse() {
        var service = testService();
        ChatResponse response = mock(ChatResponse.class);
        AiMessage aiMessage = mock(AiMessage.class);
        when(response.aiMessage()).thenReturn(aiMessage);
        when(aiMessage.hasToolExecutionRequests()).thenReturn(false);

        assertThat(service.callHasToolExecutionRequests(response)).isFalse();
    }

    @Test
    void hasToolExecutionRequests_returnsFalse_whenRequestListNull() {
        var service = testService();
        ChatResponse response = mock(ChatResponse.class);
        AiMessage aiMessage = mock(AiMessage.class);
        when(response.aiMessage()).thenReturn(aiMessage);
        when(aiMessage.hasToolExecutionRequests()).thenReturn(true);
        when(aiMessage.toolExecutionRequests()).thenReturn(null);

        assertThat(service.callHasToolExecutionRequests(response)).isFalse();
    }

    @Test
    void hasToolExecutionRequests_returnsFalse_whenRequestListEmpty() {
        var service = testService();
        ChatResponse response = mock(ChatResponse.class);
        AiMessage aiMessage = mock(AiMessage.class);
        when(response.aiMessage()).thenReturn(aiMessage);
        when(aiMessage.hasToolExecutionRequests()).thenReturn(true);
        when(aiMessage.toolExecutionRequests()).thenReturn(List.of());

        assertThat(service.callHasToolExecutionRequests(response)).isFalse();
    }

    @Test
    void hasToolExecutionRequests_returnsTrue_whenRequestListPresent() {
        var service = testService();
        ChatResponse response = mock(ChatResponse.class);
        AiMessage aiMessage = mock(AiMessage.class);
        ToolExecutionRequest request = mock(ToolExecutionRequest.class);
        when(response.aiMessage()).thenReturn(aiMessage);
        when(aiMessage.hasToolExecutionRequests()).thenReturn(true);
        when(aiMessage.toolExecutionRequests()).thenReturn(List.of(request));

        assertThat(service.callHasToolExecutionRequests(response)).isTrue();
    }

    @Test
    void executeToolRequests_returnsAiMessageAndToolResult_whenToolExists() {
        var service = testService();
        ChatResponse response = mock(ChatResponse.class);
        AiMessage aiMessage = mock(AiMessage.class);
        ToolExecutionRequest request = mock(ToolExecutionRequest.class);
        McpToolRegistry registry = mock(McpToolRegistry.class);
        McpTool tool = mock(McpTool.class);

        when(request.name()).thenReturn("my-tool");
        when(request.arguments()).thenReturn("{}");
        when(response.aiMessage()).thenReturn(aiMessage);
        when(aiMessage.toolExecutionRequests()).thenReturn(List.of(request));
        when(registry.findByName("my-tool")).thenReturn(Optional.of(tool));
        when(tool.execute(request)).thenReturn("tool-ok");

        List<ChatMessage> result = service.callExecuteToolRequests(response, registry);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(aiMessage);
        assertThat(result.get(1)).isInstanceOf(ToolExecutionResultMessage.class);
        assertThat(((ToolExecutionResultMessage) result.get(1)).text()).isEqualTo("tool-ok");
    }

    @Test
    void executeToolRequests_returnsErrorResult_whenToolMissing() {
        var service = testService();
        ChatResponse response = mock(ChatResponse.class);
        AiMessage aiMessage = mock(AiMessage.class);
        ToolExecutionRequest request = mock(ToolExecutionRequest.class);
        McpToolRegistry registry = mock(McpToolRegistry.class);

        when(request.name()).thenReturn("missing-tool");
        when(request.arguments()).thenReturn("{}");
        when(response.aiMessage()).thenReturn(aiMessage);
        when(aiMessage.toolExecutionRequests()).thenReturn(List.of(request));
        when(registry.findByName("missing-tool")).thenReturn(Optional.empty());

        List<ChatMessage> result = service.callExecuteToolRequests(response, registry);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(aiMessage);
        assertThat(result.get(1)).isInstanceOf(ToolExecutionResultMessage.class);
        assertThat(((ToolExecutionResultMessage) result.get(1)).text()).contains("missing-tool");
    }

    @Test
    void executeToolRequestWithRetry_delegatesToToolExecute() {
        var service = testService();
        McpTool tool = mock(McpTool.class);
        ToolExecutionRequest request = mock(ToolExecutionRequest.class);
        when(tool.execute(request)).thenReturn("ok");

        String result = service.callExecuteToolRequestWithRetry(tool, request);

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void executeToolRequestWithRetry_propagatesException() {
        var service = testService();
        McpTool tool = mock(McpTool.class);
        ToolExecutionRequest request = mock(ToolExecutionRequest.class);
        when(tool.execute(request)).thenThrow(new RuntimeException("tool-failure"));

        assertThatThrownBy(() -> service.callExecuteToolRequestWithRetry(tool, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("tool-failure");
    }

    @Test
    void toolExecutionFallback_returnsErrorMessage() {
        var service = testService();
        ToolExecutionRequest request = mock(ToolExecutionRequest.class);
        when(request.name()).thenReturn("calc");

        String result = service.callToolExecutionFallback(null, request);

        assertThat(result).isEqualTo("Error: Tool execution failed for 'calc'");
    }

    @Test
    void mapToLangChainMessages_mapsUserAssistantSystem() {
        var service = testService();

        ChatMessageDTOV1 user = new ChatMessageDTOV1();
        user.setType(ChatMessageDTOV1.TypeEnum.USER);
        user.setMessage("u");

        ChatMessageDTOV1 assistant = new ChatMessageDTOV1();
        assistant.setType(ChatMessageDTOV1.TypeEnum.ASSISTANT);
        assistant.setMessage("a");

        ChatMessageDTOV1 system = new ChatMessageDTOV1();
        system.setType(ChatMessageDTOV1.TypeEnum.SYSTEM);
        system.setMessage("s");

        List<ChatMessage> mapped = service.callMapToLangChainMessages(List.of(user, assistant, system));

        assertThat(mapped).hasSize(3);
        assertThat(mapped.get(0)).isInstanceOf(UserMessage.class);
        assertThat(mapped.get(1)).isInstanceOf(AiMessage.class);
        assertThat(mapped.get(2)).isInstanceOf(SystemMessage.class);
    }

    @Test
    void mapToLangChainMessages_ignoresActionType() {
        var service = testService();

        ChatMessageDTOV1 action = new ChatMessageDTOV1();
        action.setType(ChatMessageDTOV1.TypeEnum.ACTION);
        action.setMessage("action");

        ChatMessageDTOV1 user = new ChatMessageDTOV1();
        user.setType(ChatMessageDTOV1.TypeEnum.USER);
        user.setMessage("u");

        List<ChatMessage> mapped = service.callMapToLangChainMessages(List.of(action, user));

        assertThat(mapped).hasSize(1);
        assertThat(mapped.get(0)).isInstanceOf(UserMessage.class);
    }

    private static TestableAbstractLlmService testService() {
        DispatchConfig dispatchConfig = mock(DispatchConfig.class);
        DispatchConfig.MCPConfig mcpConfig = mock(DispatchConfig.MCPConfig.class);
        when(dispatchConfig.mcpConfig()).thenReturn(mcpConfig);
        when(mcpConfig.maxToolExecutionRetries()).thenReturn(2L);

        return new TestableAbstractLlmService(dispatchConfig);
    }

    static class TestableAbstractLlmService extends AbstractLlmService {

        TestableAbstractLlmService(DispatchConfig dispatchConfig) {
            this.dispatchConfig = dispatchConfig;
        }

        boolean callHasToolExecutionRequests(ChatResponse response) {
            return hasToolExecutionRequests(response);
        }

        List<ChatMessage> callExecuteToolRequests(ChatResponse response, McpToolRegistry toolRegistry) {
            return executeToolRequests(response, toolRegistry);
        }

        String callExecuteToolRequestWithRetry(McpTool tool, ToolExecutionRequest toolRequest) {
            return executeToolRequestWithRetry(tool, toolRequest);
        }

        String callToolExecutionFallback(McpTool tool, ToolExecutionRequest toolRequest) {
            return toolExecutionFallback(tool, toolRequest);
        }

        List<ChatMessage> callMapToLangChainMessages(List<ChatMessageDTOV1> history) {
            return mapToLangChainMessages(history);
        }

        @Override
        public Response chat(Configuration configuration, ChatRequestDTOV1 chatRequestDTO) {
            return Response.ok().build();
        }

        @Override
        public String getHealthStatus(Provider provider) {
            return "HEALTHY";
        }
    }
}
