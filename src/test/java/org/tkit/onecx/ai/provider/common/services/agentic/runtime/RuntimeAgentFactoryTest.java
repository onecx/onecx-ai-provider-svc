package org.tkit.onecx.ai.provider.common.services.agentic.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.common.services.mcp.McpService;
import org.tkit.onecx.ai.provider.common.services.mcp.McpToolRegistry;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.Model;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;

import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.internal.AgentExecutor;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatMessageDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
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

    private Agent agent() {
        Provider provider = new Provider();
        provider.setType(ProviderType.OLLAMA);
        provider.setLlmUrl("http://ollama.local");

        Model model = new Model();
        model.setProvider(provider);
        model.setModelIdentifier("ministral-3:8b");

        Agent agent = new Agent();
        agent.setId("agent-1");
        agent.setName("yeah-agent");
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
}
