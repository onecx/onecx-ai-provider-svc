package org.tkit.onecx.ai.provider.common.services.agentic.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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
        service.runtimeTimeout = 1200L;
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
        when(runtimeAgentFactory.delegatesForGroup(root, group, request, "exec-root")).thenReturn(List.of());

        AgenticRuntimeResult result = service.invokeRoot(root, request);

        assertThat(result.successful()).isTrue();
        assertThat(result.responseText()).isEqualTo("root answer");
        verify(runtimeAgentFactory).delegatesForGroup(root, group, request, "exec-root");
        verify(runtimeAgentFactory, never()).agentsForGroup(any(), any(), any(), any());
        verify(executionService).succeedExecution("exec-root", "root answer");
    }

    @Test
    void invokeRoot_withDefaultA2AGroup_usesLeadAgentWithLazyDelegates() {
        AgentGroup group = new AgentGroup();
        group.setId("group-lead");
        group.setName("lead");

        Agent root = new Agent();
        root.setId("root");
        root.setName("root");
        root.setA2aEnabled(true);
        root.setGroups(Set.of(group));

        Execution rootExecution = new Execution();
        rootExecution.setExecutionId("exec-root");

        ChatRequestDTOV1 request = chatRequest("How are you?");
        RuntimeAgentDelegate delegate = new RuntimeAgentDelegate("peer", "Peer expert",
                () -> staticRuntimeAgent("peer answer"));
        when(executionService.createExecution(eq(root), eq(null), any())).thenReturn(rootExecution);
        when(runtimeAgentFactory.delegatesForGroup(root, group, request, "exec-root")).thenReturn(List.of(delegate));
        when(runtimeAgentFactory.leadAgent(root, request, "exec-root", List.of(delegate)))
                .thenReturn(staticRuntimeAgent("lead answer"));

        AgenticRuntimeResult result = service.invokeRoot(root, request);

        assertThat(result.successful()).isTrue();
        assertThat(result.responseText()).isEqualTo("lead answer");
        verify(runtimeAgentFactory).leadAgent(root, request, "exec-root", List.of(delegate));
        verify(runtimeAgentFactory, never()).agentsForGroup(any(), any(), any(), any());
        verify(executionService).succeedExecution("exec-root", "lead answer");
    }

    @Test
    void invokeRoot_withSequentialGroup_stillUsesWorkflowAgents() {
        AgentGroup group = new AgentGroup();
        group.setId("group-sequential");
        group.setName("sequential");
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
        when(runtimeAgentFactory.agentsForGroup(root, group, request, "exec-root")).thenReturn(List.of());

        AgenticRuntimeResult result = service.invokeRoot(root, request);

        assertThat(result.successful()).isTrue();
        assertThat(result.responseText()).isEqualTo("root answer");
        verify(runtimeAgentFactory).agentsForGroup(root, group, request, "exec-root");
        verify(runtimeAgentFactory, never()).delegatesForGroup(any(), any(), any(), any());
    }

    @Test
    void invokeRoot_withSupervisorGroup_usesLazySupervisorCandidates() {
        AgentGroup group = new AgentGroup();
        group.setId("group-supervisor");
        group.setName("supervisor");
        group.setOrchestrationMode(AgentGroupOrchestrationMode.SUPERVISOR_ROUTED);

        Agent root = new Agent();
        root.setId("root");
        root.setName("root");
        root.setA2aEnabled(true);
        root.setGroups(Set.of(group));

        Execution rootExecution = new Execution();
        rootExecution.setExecutionId("exec-root");

        ChatRequestDTOV1 request = chatRequest("Hello");
        when(executionService.createExecution(eq(root), eq(null), any())).thenReturn(rootExecution);
        when(runtimeAgentFactory.supervisorCandidatesForGroup(root, group, request, "exec-root")).thenReturn(List.of());
        when(runtimeAgentFactory.rootAgent(root, request, "exec-root")).thenReturn(staticRuntimeAgent("root answer"));

        AgenticRuntimeResult result = service.invokeRoot(root, request);

        assertThat(result.successful()).isTrue();
        assertThat(result.responseText()).isEqualTo("root answer");
        verify(runtimeAgentFactory).supervisorCandidatesForGroup(root, group, request, "exec-root");
        verify(runtimeAgentFactory, never()).agentsForGroup(any(), any(), any(), any());
    }

    @Test
    void invokeRoot_returnsTimeoutWhenRuntimeDeadlineExpires() {
        Agent root = new Agent();
        root.setId("root");
        root.setName("root");

        Execution rootExecution = new Execution();
        rootExecution.setExecutionId("exec-root");

        ChatRequestDTOV1 request = chatRequest("Hello");
        AtomicBoolean interrupted = new AtomicBoolean(false);
        service.runtimeTimeout = 1L;
        when(executionService.createExecution(eq(root), eq(null), any())).thenReturn(rootExecution);
        when(runtimeAgentFactory.rootAgent(root, request, "exec-root"))
                .thenReturn(new RuntimeAgent("root", "Root agent", new SleepingUntypedAgent(interrupted), null));

        AgenticRuntimeResult result = service.invokeRoot(root, request);

        assertThat(result.successful()).isFalse();
        assertThat(result.status()).isEqualTo(AgenticRuntimeStatus.TIMEOUT);
        assertThat(result.executionId()).isEqualTo("exec-root");
        verify(executionService).failExecution(eq("exec-root"), eq("TimeoutException"), any());
    }

    @Test
    void agentGroupDefaultsToLeadDelegates() {
        AgentGroup group = new AgentGroup();

        assertThat(group.getOrchestrationMode()).isEqualTo(AgentGroupOrchestrationMode.LEAD_DELEGATES);
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

    private static final class SleepingUntypedAgent implements UntypedAgent {

        private final AtomicBoolean interrupted;

        private SleepingUntypedAgent(AtomicBoolean interrupted) {
            this.interrupted = interrupted;
        }

        @Override
        public Object invoke(Map<String, Object> input) {
            return sleep();
        }

        @Override
        public ResultWithAgenticScope<String> invokeWithAgenticScope(Map<String, Object> input) {
            return new ResultWithAgenticScope<>(null, sleep());
        }

        @Override
        public AgenticScope getAgenticScope(Object memoryId) {
            return null;
        }

        @Override
        public boolean evictAgenticScope(Object memoryId) {
            return false;
        }

        private String sleep() {
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException ex) {
                interrupted.set(true);
                Thread.currentThread().interrupt();
            }
            return "late answer";
        }
    }
}
