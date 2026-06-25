package org.tkit.onecx.ai.provider.common.services.agentic.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.common.services.mcp.McpService;
import org.tkit.onecx.ai.provider.common.services.mcp.McpToolRegistry;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.Model;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;

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
            Object result = runtimeAgent.agent()
                    .invokeWithAgenticScope(java.util.Map.of(RuntimeAgentFactory.INPUT_REQUEST, request))
                    .result();

            assertThat(result).isEqualTo("Tigers are large cats.");
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
}
