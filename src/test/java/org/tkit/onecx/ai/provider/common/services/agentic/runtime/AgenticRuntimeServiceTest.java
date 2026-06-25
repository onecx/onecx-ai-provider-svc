package org.tkit.onecx.ai.provider.common.services.agentic.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tkit.onecx.ai.provider.common.services.execution.ExecutionService;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.AgentGroup;
import org.tkit.onecx.ai.provider.domain.models.Execution;
import org.tkit.onecx.ai.provider.domain.models.enums.AgentGroupOrchestrationMode;

import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatMessageDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;

@ExtendWith(MockitoExtension.class)
public class AgenticRuntimeServiceTest {

    @Mock
    ExecutionService executionService;

    @Mock
    RuntimeAgentFactory runtimeAgentFactory;

    AgenticRuntimeService service;

    @BeforeEach
    void setUp() {
        service = new AgenticRuntimeService();
        service.executionService = executionService;
        service.runtimeAgentFactory = runtimeAgentFactory;
    }

    @Test
    void invokeRoot_withoutGroups_invokesConfiguredLangChainAgent() {
        Agent root = new Agent();
        root.setId("root");
        root.setName("root");

        Execution rootExecution = new Execution();
        rootExecution.setExecutionId("exec-root");

        ChatRequestDTOV1 request = chatRequest("Hello");
        when(executionService.createExecution(eq(root), eq(null), any())).thenReturn(rootExecution);
        when(runtimeAgentFactory.rootAgent(root, request, "exec-root"))
                .thenReturn(staticRuntimeAgent("root answer"));

        AgenticRuntimeResult result = service.invokeRoot(root, request);

        assertThat(result.successful()).isTrue();
        assertThat(result.executionId()).isEqualTo("exec-root");
        assertThat(result.responseText()).isEqualTo("root answer");
        verify(executionService).succeedExecution("exec-root", "root answer");
        verify(runtimeAgentFactory, never()).agentsForGroup(any(), any(), any(), any());
    }

    @Test
    void invokeRoot_withEmptyA2AGroup_keepsRootResponse() {
        AgentGroup group = new AgentGroup();
        group.setId("group-empty");
        group.setName("empty");
        group.setOrchestrationMode(AgentGroupOrchestrationMode.SEQUENTIAL);

        Agent root = new Agent();
        root.setId("root");
        root.setName("root");
        root.setA2aEnabled(true);
        root.setGroups(Set.of(group));

        Execution rootExecution = new Execution();
        rootExecution.setExecutionId("exec-root");

        ChatRequestDTOV1 request = chatRequest("Hello");
        when(executionService.createExecution(eq(root), eq(null), any())).thenReturn(rootExecution);
        when(runtimeAgentFactory.rootAgent(root, request, "exec-root"))
                .thenReturn(staticRuntimeAgent("root answer"));
        when(runtimeAgentFactory.agentsForGroup(root, group, request, "exec-root")).thenReturn(java.util.List.of());

        AgenticRuntimeResult result = service.invokeRoot(root, request);

        assertThat(result.successful()).isTrue();
        assertThat(result.responseText()).isEqualTo("root answer");
        verify(runtimeAgentFactory).agentsForGroup(root, group, request, "exec-root");
        verify(executionService).succeedExecution("exec-root", "root answer");
    }

    private RuntimeAgent staticRuntimeAgent(String output) {
        return new RuntimeAgent("root", "Root agent", new StaticUntypedAgent(output), null);
    }

    private ChatRequestDTOV1 chatRequest(String text) {
        ChatRequestDTOV1 request = new ChatRequestDTOV1();
        ChatMessageDTOV1 message = new ChatMessageDTOV1();
        message.setType(ChatMessageDTOV1.TypeEnum.USER);
        message.setMessage(text);
        request.setChatMessage(message);
        return request;
    }

    private static final class StaticUntypedAgent implements UntypedAgent {

        private final String output;

        private StaticUntypedAgent(String output) {
            this.output = output;
        }

        @Override
        public Object invoke(Map<String, Object> input) {
            return output;
        }

        @Override
        public ResultWithAgenticScope<String> invokeWithAgenticScope(Map<String, Object> input) {
            return new ResultWithAgenticScope<>(null, output);
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
}
