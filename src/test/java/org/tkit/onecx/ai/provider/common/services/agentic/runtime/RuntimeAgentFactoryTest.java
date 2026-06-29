package org.tkit.onecx.ai.provider.common.services.agentic.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.common.services.mcp.McpService;
import org.tkit.onecx.ai.provider.common.services.mcp.McpTool;
import org.tkit.onecx.ai.provider.common.services.mcp.McpToolRegistry;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.AgentGroup;
import org.tkit.onecx.ai.provider.domain.models.Model;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.internal.AgentExecutor;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.tool.ToolExecutionResult;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatMessageDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(RuntimeAgentFactoryTest.McpIterationProfile.class)
class RuntimeAgentFactoryTest {

    @Inject
    RuntimeAgentFactory factory;

    @InjectMock
    ChatModelFactory chatModelFactory;

    @InjectMock
    McpService mcpService;

    @Test
    void rootAgent_invokesPlainTextModelResponseWithoutJsonParsing() {
        Agent agent = agent();
        ChatRequestDTOV1 request = chatRequest("How big is a tiger?");
        when(chatModelFactory.createChatModel(agent)).thenReturn(new PlainTextChatModel("Tigers are large cats."));
        when(mcpService.createToolRegistry(agent, "exec-root")).thenReturn(McpToolRegistry.empty());

        try (RuntimeAgent runtimeAgent = factory.rootAgent(agent, request, "exec-root")) {
            assertThat(runtimeAgent.agent()).isInstanceOf(AgentExecutor.class);
            assertThat(runtimeAgent.invoker()).isInstanceOf(UntypedAgent.class);
            assertThat(runtimeAgent.invoker()).isNotSameAs(runtimeAgent.agent());

            Object result = runtimeAgent.invoker()
                    .invokeWithAgenticScope(java.util.Map.of("message", "How big is a tiger?"))
                    .result();

            assertThat(result).isEqualTo("Tigers are large cats.");
        }
    }

    @Test
    void leadAgent_exposesPeersAsOptionalDelegateTools() {
        Agent agent = agent();
        ChatRequestDTOV1 request = chatRequest("How big is a tiger?");
        CapturingChatModel chatModel = new CapturingChatModel("Tigers are large cats.");
        when(chatModelFactory.createChatModel(agent)).thenReturn(chatModel);
        when(mcpService.createToolRegistry(agent, "exec-root")).thenReturn(McpToolRegistry.empty());

        UntypedAgent delegateInvoker = mock(UntypedAgent.class);
        RuntimeAgentDelegate delegate = new RuntimeAgentDelegate("onecx-agent", "Expert for OneCX documentation",
                () -> new RuntimeAgent("onecx-agent", "Expert for OneCX documentation", delegateInvoker, null));

        try (RuntimeAgent runtimeAgent = factory.leadAgent(agent, request, "exec-root", List.of(delegate))) {
            Object result = runtimeAgent.invoker()
                    .invokeWithAgenticScope(Map.of("message", "How big is a tiger?"))
                    .result();

            assertThat(result).isEqualTo("Tigers are large cats.");
            assertThat(chatModel.lastRequest.toolSpecifications())
                    .extracting(spec -> spec.name())
                    .contains("delegate_onecx_agent");
            assertThat(chatModel.lastRequest.toString()).contains("Optional peer agents are available as tools");
            assertThat(chatModel.lastRequest.toString()).contains("You are the lead agent and own the final answer");
            assertThat(chatModel.lastRequest.toString())
                    .contains("Answer normal, general, basic, conversational, ambiguous, or unmatched requests yourself");
            assertThat(chatModel.lastRequest.toString()).contains("Use a peer agent only when");
            assertThat(chatModel.lastRequest.toString()).contains("A request mentioning OneCX matches peers");
            assertThat(chatModel.lastRequest.toString()).contains("Do not require the user to mention \"MCP server\"");
            assertThat(chatModel.lastRequest.toolSpecifications())
                    .extracting(spec -> spec.description())
                    .anySatisfy(description -> assertThat(description)
                            .contains("matches this agent's name, domain, data source, or specialty")
                            .contains("documentation"));
            assertThat(chatModel.lastRequest.toString()).contains("Current user message:");
            assertThat(chatModel.lastRequest.toString()).contains("How big is a tiger?");
            verify(delegateInvoker, never()).invokeWithAgenticScope(any());
        }
    }

    @Test
    void leadAgent_treatsDelegateFailureAsToolResult() {
        Agent agent = agent();
        ChatRequestDTOV1 request = chatRequest("Tell me what the OneCX Generator is for");
        DelegateToolCallingChatModel chatModel = new DelegateToolCallingChatModel();
        when(chatModelFactory.createChatModel(agent)).thenReturn(chatModel);
        when(mcpService.createToolRegistry(agent, "exec-root")).thenReturn(McpToolRegistry.empty());

        RuntimeAgentDelegate delegate = new RuntimeAgentDelegate("onecx-agent", "Answer every question related to OneCX",
                () -> new RuntimeAgent("onecx-agent", "Answer every question related to OneCX",
                        new ThrowingUntypedAgent(), null));

        try (RuntimeAgent runtimeAgent = factory.leadAgent(agent, request, "exec-root", List.of(delegate))) {
            Object result = runtimeAgent.invoker()
                    .invokeWithAgenticScope(Map.of("message", "Tell me what the OneCX Generator is for"))
                    .result();

            assertThat(result).isEqualTo("I could not reach the OneCX specialist, but I can still answer briefly.");
            assertThat(chatModel.calls.get()).isEqualTo(2);
            assertThat(chatModel.secondRequest.toString()).contains("could not complete the delegated request");
        }
    }

    @Test
    void leadAgent_waitsForDelegateMcpToolResultBeforeFinalAnswer() {
        Agent rootAgent = agent("root-agent");
        Agent onecxAgent = agent("onecx-agent");
        ChatRequestDTOV1 request = chatRequest("Tell me what the OneCX Generator is for");

        RootDelegatingChatModel rootModel = new RootDelegatingChatModel();
        McpCallingChatModel onecxModel = new McpCallingChatModel();
        McpClient mcpClient = mock(McpClient.class);
        ToolExecutionRequest expectedMcpRequest = ToolExecutionRequest.builder()
                .id("mcp-call-1")
                .name("search_docs")
                .arguments("{\"query\":\"OneCX Generator\"}")
                .build();
        when(mcpClient.executeTool(expectedMcpRequest)).thenReturn(ToolExecutionResult.builder()
                .resultText("OneCX Generator docs result")
                .build());

        when(chatModelFactory.createChatModel(rootAgent)).thenReturn(rootModel);
        when(chatModelFactory.createChatModel(onecxAgent)).thenReturn(onecxModel);
        when(mcpService.createToolRegistry(rootAgent, "exec-root")).thenReturn(McpToolRegistry.empty());
        when(mcpService.createToolRegistry(onecxAgent, "exec-child")).thenReturn(new McpToolRegistry(List.of(
                new McpTool("tool-search-docs", "http://mcp", toolSpec("search_docs"), mcpClient))));

        RuntimeAgentDelegate delegate = new RuntimeAgentDelegate("onecx-agent", "Answer every question related to OneCX",
                () -> factory.rootAgent(onecxAgent, request, "exec-child"));

        try (RuntimeAgent runtimeAgent = factory.leadAgent(rootAgent, request, "exec-root", List.of(delegate))) {
            Object result = runtimeAgent.invoker()
                    .invokeWithAgenticScope(Map.of("message", "Tell me what the OneCX Generator is for"))
                    .result();

            assertThat(result).isEqualTo("Final answer using OneCX Generator docs result");
            assertThat(rootModel.calls.get()).isEqualTo(2);
            assertThat(onecxModel.calls.get()).isEqualTo(2);
            assertThat(onecxModel.secondRequest.toString()).contains("OneCX Generator docs result");
            assertThat(rootModel.secondRequest.toString()).contains("OneCX peer answer from OneCX Generator docs result");
            verify(mcpClient).executeTool(expectedMcpRequest);
        }
    }

    @Test
    void supervisorCandidates_doNotOpenRuntimeUntilSelected() {
        Agent rootAgent = agent("root-agent");
        AgentGroup group = new AgentGroup();
        ChatRequestDTOV1 request = chatRequest("Hello");
        CapturingChatModel chatModel = new CapturingChatModel("root answer");
        when(chatModelFactory.createChatModel(rootAgent)).thenReturn(chatModel);
        when(mcpService.createToolRegistry(rootAgent, "exec-root")).thenReturn(McpToolRegistry.empty());

        List<RuntimeAgent> candidates = factory.supervisorCandidatesForGroup(rootAgent, group, request, "exec-root");

        assertThat(candidates).hasSize(1);
        verify(chatModelFactory, never()).createChatModel(rootAgent);
        verify(mcpService, never()).createToolRegistry(rootAgent, "exec-root");

        Object result = candidates.get(0).invoker()
                .invokeWithAgenticScope(Map.of("message", "Hello"))
                .result();

        assertThat(result).isEqualTo("root answer");
        verify(chatModelFactory).createChatModel(rootAgent);
        verify(mcpService).createToolRegistry(rootAgent, "exec-root");
    }

    @Test
    void rootAgent_usesConfiguredMcpMaxIterationsForSequentialToolCalls() {
        Agent agent = agent("docs-agent");
        ChatRequestDTOV1 request = chatRequest("Answer with docs");
        RepeatedToolCallingChatModel chatModel = new RepeatedToolCallingChatModel(4);
        McpClient mcpClient = mock(McpClient.class);
        ToolSpecification toolSpecification = toolSpec("search_docs");

        when(chatModelFactory.createChatModel(agent)).thenReturn(chatModel);
        when(mcpClient.executeTool(any())).thenAnswer(invocation -> ToolExecutionResult.builder()
                .resultText("tool result " + chatModel.toolExecutions.incrementAndGet())
                .build());
        when(mcpService.createToolRegistry(agent, "exec-root")).thenReturn(new McpToolRegistry(List.of(
                new McpTool("tool-search-docs", "http://mcp", toolSpecification, mcpClient))));

        try (RuntimeAgent runtimeAgent = factory.rootAgent(agent, request, "exec-root")) {
            Object result = runtimeAgent.invoker()
                    .invokeWithAgenticScope(Map.of("message", "Answer with docs"))
                    .result();

            assertThat(result).isEqualTo("final answer after tools");
            assertThat(chatModel.chatCalls.get()).isEqualTo(5);
            verify(mcpClient, times(4)).executeTool(any());
        }
    }

    private Agent agent() {
        return agent("yeah-agent");
    }

    private Agent agent(String name) {
        Provider provider = new Provider();
        provider.setType(ProviderType.OLLAMA);
        provider.setLlmUrl("http://ollama.local");

        Model model = new Model();
        model.setProvider(provider);
        model.setModelIdentifier("ministral-3:8b");

        Agent agent = new Agent();
        agent.setId(name);
        agent.setName(name);
        agent.setDescription("General assistant");
        agent.setModel(model);
        return agent;
    }

    private ChatRequestDTOV1 chatRequest(String text) {
        ChatRequestDTOV1 request = new ChatRequestDTOV1();
        ChatMessageDTOV1 message = new ChatMessageDTOV1();
        message.setType(ChatMessageDTOV1.TypeEnum.USER);
        message.setMessage(text);
        request.setChatMessage(message);
        return request;
    }

    private static final class PlainTextChatModel implements ChatModel {

        private final String response;

        private PlainTextChatModel(String response) {
            this.response = response;
        }

        @Override
        public ChatResponse doChat(ChatRequest chatRequest) {
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from(response))
                    .build();
        }
    }

    private static final class CapturingChatModel implements ChatModel {

        private final String response;
        private ChatRequest lastRequest;

        private CapturingChatModel(String response) {
            this.response = response;
        }

        @Override
        public ChatResponse doChat(ChatRequest chatRequest) {
            lastRequest = chatRequest;
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from(response))
                    .build();
        }
    }

    private static final class DelegateToolCallingChatModel implements ChatModel {

        private final AtomicInteger calls = new AtomicInteger();
        private ChatRequest secondRequest;

        @Override
        public ChatResponse doChat(ChatRequest chatRequest) {
            if (calls.incrementAndGet() == 1) {
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from(ToolExecutionRequest.builder()
                                .id("call-1")
                                .name("delegate_onecx_agent")
                                .arguments("{\"message\":\"Tell me what the OneCX Generator is for\"}")
                                .build()))
                        .build();
            }
            secondRequest = chatRequest;
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from(
                            "I could not reach the OneCX specialist, but I can still answer briefly."))
                    .build();
        }
    }

    private static final class ThrowingUntypedAgent implements UntypedAgent {

        @Override
        public Object invoke(Map<String, Object> input) {
            throw new RuntimeException("delegate timeout");
        }

        @Override
        public ResultWithAgenticScope<String> invokeWithAgenticScope(Map<String, Object> input) {
            throw new RuntimeException("delegate timeout");
        }

        @Override
        public AgenticScope getAgenticScope(Object memoryId) {
            return null;
        }

        @Override
        public boolean evictAgenticScope(Object memoryId) {
            return false;
        }
    }

    private static final class RootDelegatingChatModel implements ChatModel {

        private final AtomicInteger calls = new AtomicInteger();
        private ChatRequest secondRequest;

        @Override
        public ChatResponse doChat(ChatRequest chatRequest) {
            if (calls.incrementAndGet() == 1) {
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from(ToolExecutionRequest.builder()
                                .id("delegate-call-1")
                                .name("delegate_onecx_agent")
                                .arguments("{\"message\":\"Tell me what the OneCX Generator is for\"}")
                                .build()))
                        .build();
            }
            secondRequest = chatRequest;
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from("Final answer using OneCX Generator docs result"))
                    .build();
        }
    }

    private static final class McpCallingChatModel implements ChatModel {

        private final AtomicInteger calls = new AtomicInteger();
        private ChatRequest secondRequest;

        @Override
        public ChatResponse doChat(ChatRequest chatRequest) {
            if (calls.incrementAndGet() == 1) {
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from(ToolExecutionRequest.builder()
                                .id("mcp-call-1")
                                .name("search_docs")
                                .arguments("{\"query\":\"OneCX Generator\"}")
                                .build()))
                        .build();
            }
            secondRequest = chatRequest;
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from("OneCX peer answer from OneCX Generator docs result"))
                    .build();
        }
    }

    private static final class RepeatedToolCallingChatModel implements ChatModel {

        private final int toolCallsBeforeFinal;
        private final AtomicInteger chatCalls = new AtomicInteger();
        private final AtomicInteger toolExecutions = new AtomicInteger();

        private RepeatedToolCallingChatModel(int toolCallsBeforeFinal) {
            this.toolCallsBeforeFinal = toolCallsBeforeFinal;
        }

        @Override
        public ChatResponse doChat(ChatRequest chatRequest) {
            int call = chatCalls.incrementAndGet();
            if (call <= toolCallsBeforeFinal) {
                return ChatResponse.builder()
                        .aiMessage(AiMessage.from(ToolExecutionRequest.builder()
                                .id("mcp-call-" + call)
                                .name("search_docs")
                                .arguments("{\"query\":\"OneCX Generator " + call + "\"}")
                                .build()))
                        .build();
            }
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from("final answer after tools"))
                    .build();
        }
    }

    public static class McpIterationProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("onecx.ai.dispatch.mcp.max-iterations", "5");
        }
    }

    private static ToolSpecification toolSpec(String name) {
        return ToolSpecification.builder()
                .name(name)
                .description("Search docs")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("query")
                        .required("query")
                        .build())
                .build();
    }
}
