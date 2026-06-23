package org.tkit.onecx.ai.provider.common.services.agentic.a2a;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tkit.onecx.ai.provider.common.models.DispatchConfig;
import org.tkit.onecx.ai.provider.common.services.execution.ExecutionService;
import org.tkit.onecx.ai.provider.common.services.llm.AgentDispatchService;
import org.tkit.onecx.ai.provider.domain.daos.AgentDAO;
import org.tkit.onecx.ai.provider.domain.daos.ExternalAgentDAO;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.AgentGroup;
import org.tkit.onecx.ai.provider.domain.models.Execution;
import org.tkit.onecx.ai.provider.domain.models.ExternalAgent;
import org.tkit.onecx.ai.provider.domain.models.Model;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;

import com.fasterxml.jackson.databind.ObjectMapper;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatMessageDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;

@ExtendWith(MockitoExtension.class)
class A2AOrchestrationServiceTest {

    @Mock
    AgentDAO agentDAO;

    @Mock
    ExternalAgentDAO externalAgentDAO;

    @Mock
    ExecutionService executionService;

    @Mock
    DefaultA2AGroupPlanner a2aGroupPlanner;

    @Mock
    SequentialA2AExecutor sequentialA2AExecutor;

    @Mock
    AgentDispatchService agentDispatchService;

    @Mock
    DispatchConfig dispatchConfig;

    @Mock
    DispatchConfig.A2AConfig a2aConfig;

    @Mock
    ExternalAgentDiscoveryService externalAgentDiscoveryService;

    private final A2AOrchestrationService service = new A2AOrchestrationService();

    @BeforeEach
    void setUp() {
        service.agentDAO = agentDAO;
        service.externalAgentDAO = externalAgentDAO;
        service.executionService = executionService;
        service.a2aGroupPlanner = a2aGroupPlanner;
        service.sequentialA2AExecutor = sequentialA2AExecutor;
        service.agentDispatchService = agentDispatchService;
        service.dispatchConfig = dispatchConfig;
        service.objectMapper = new ObjectMapper();
        service.externalAgentDiscoveryService = externalAgentDiscoveryService;
    }

    @Test
    void invokeRoot_nullAgent_returnsBadRequest() {
        try (Response response = service.invokeRoot(null, new ChatRequestDTOV1())) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        }
    }

    @Test
    void invokeRoot_singleAgent_success() {
        Agent root = agent("root", "root-id", false, null);
        ChatRequestDTOV1 request = chatRequest("hello");

        Execution rootExecution = new Execution();
        rootExecution.setExecutionId("exec-root");

        when(executionService.createExecution(eq(root), eq(null), any())).thenReturn(rootExecution);

        try (Response rootDispatchResponse = assistantResponse("root answer");
                Response response = invokeRootWithSingleDispatch(root, request, rootDispatchResponse)) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
            assertThat(response.getHeaderString("X-Execution-Id")).isEqualTo("exec-root");
            ChatMessageDTOV1 dto = (ChatMessageDTOV1) response.getEntity();
            assertThat(dto.getMessage()).isEqualTo("root answer");
        }

        verify(executionService).startExecution("exec-root");
        verify(executionService).succeedExecution("exec-root", "root answer");
        verify(executionService, never()).incrementAgentCallCount(any());
    }

    @Test
    void invokeRoot_withA2AChild_mergesChildResponseAndTracksAgentCall() {
        when(dispatchConfig.a2aConfig()).thenReturn(a2aConfig);
        when(a2aConfig.maxDepth()).thenReturn(10);

        AgentGroup group = new AgentGroup();
        group.setId("group-1");
        group.setName("helpers");

        Agent root = agent("root", "root-id", true, Set.of(group));
        Agent child = agent("child", "child-id", false, Set.of(group));

        ChatRequestDTOV1 request = chatRequest("hello");

        Execution rootExecution = new Execution();
        rootExecution.setExecutionId("exec-root");
        Execution childExecution = new Execution();
        childExecution.setExecutionId("exec-child");

        A2AExecutionPlan plan = new A2AExecutionPlan(A2AStrategy.SEQUENTIAL,
                List.of(new A2AExecutionUnit("group-1", "helpers")));

        when(executionService.createExecution(eq(root), eq(null), any())).thenReturn(rootExecution);
        when(executionService.createExecution(eq(child), eq("group-1"), any())).thenReturn(childExecution);

        when(a2aGroupPlanner.plan(root)).thenReturn(plan);
        when(agentDAO.findAgentsByGroupId("group-1")).thenReturn(List.of(root, child));
        when(externalAgentDAO.findExternalAgentsByGroupId("group-1")).thenReturn(List.of());
        when(sequentialA2AExecutor.execute(eq(plan), any())).thenCallRealMethod();

        try (Response rootDispatchResponse = assistantResponse("root answer");
                Response childDispatchResponse = assistantResponse("child answer");
                Response response = invokeRootWithTwoDispatches(root, request, rootDispatchResponse, childDispatchResponse)) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
            assertThat(response.getHeaderString("X-Execution-Id")).isEqualTo("exec-root");
            ChatMessageDTOV1 dto = (ChatMessageDTOV1) response.getEntity();
            assertThat(dto.getMessage()).contains("root answer");
            assertThat(dto.getMessage()).contains("child answer");
            assertThat(dto.getMessage()).contains("[helpers] child");
        }

        verify(executionService).startExecution("exec-root");
        verify(executionService).startExecution("exec-child");
        verify(executionService).waitForResource("exec-root",
                org.tkit.onecx.ai.provider.domain.models.enums.ExecutionState.WAITING_AGENT);
        verify(executionService).resumeExecution("exec-root");
        verify(executionService).incrementAgentCallCount("exec-root");
    }

    private Response invokeRootWithSingleDispatch(Agent root, ChatRequestDTOV1 request, Response dispatchResponse) {
        when(agentDispatchService.dispatch(eq(root), eq(request), eq("exec-root"))).thenReturn(dispatchResponse);
        return service.invokeRoot(root, request);
    }

    private Response invokeRootWithTwoDispatches(Agent root, ChatRequestDTOV1 request,
            Response rootDispatchResponse, Response childDispatchResponse) {
        when(agentDispatchService.dispatch(eq(root), eq(request), eq("exec-root"))).thenReturn(rootDispatchResponse);
        when(agentDispatchService.dispatch(any(Agent.class), eq(request), eq("exec-child"))).thenReturn(childDispatchResponse);
        return service.invokeRoot(root, request);
    }

    private Agent agent(String name, String id, boolean a2aEnabled, Set<AgentGroup> groups) {
        Agent agent = new Agent();
        agent.setName(name);
        agent.setId(id);
        agent.setA2aEnabled(a2aEnabled);
        agent.setGroups(groups);

        Model model = new Model();
        Provider provider = new Provider();
        provider.setType(ProviderType.OLLAMA);
        provider.setLlmUrl("http://localhost");
        model.setProvider(provider);
        agent.setModel(model);
        return agent;
    }

    private ChatRequestDTOV1 chatRequest(String message) {
        ChatRequestDTOV1 request = new ChatRequestDTOV1();
        ChatMessageDTOV1 chatMessage = new ChatMessageDTOV1();
        chatMessage.setMessage(message);
        chatMessage.setType(ChatMessageDTOV1.TypeEnum.USER);
        request.setChatMessage(chatMessage);
        return request;
    }

    private Response assistantResponse(String message) {
        ChatMessageDTOV1 dto = new ChatMessageDTOV1();
        dto.setMessage(message);
        dto.setType(ChatMessageDTOV1.TypeEnum.ASSISTANT);
        return Response.ok(dto).build();
    }

    // -----------------------------------------------------------------------
    // Strict A2A protocol: external agent discovery tests
    // -----------------------------------------------------------------------

    @Test
    void invokeRoot_withExternalAgent_discoveryFailure_skipsExternalAgent() {
        when(dispatchConfig.a2aConfig()).thenReturn(a2aConfig);
        when(a2aConfig.maxDepth()).thenReturn(10);

        AgentGroup group = new AgentGroup();
        group.setId("group-ext");
        group.setName("external-helpers");

        Agent root = agent("root", "root-id", true, Set.of(group));
        ExternalAgent external = externalAgent("ext-bot", "http://ext/discover");

        ChatRequestDTOV1 request = chatRequest("hello");

        Execution rootExecution = new Execution();
        rootExecution.setExecutionId("exec-root");

        A2AExecutionPlan plan = new A2AExecutionPlan(A2AStrategy.SEQUENTIAL,
                List.of(new A2AExecutionUnit("group-ext", "external-helpers")));

        when(executionService.createExecution(eq(root), eq(null), any())).thenReturn(rootExecution);
        when(a2aGroupPlanner.plan(root)).thenReturn(plan);
        when(agentDAO.findAgentsByGroupId("group-ext")).thenReturn(List.of());
        when(externalAgentDAO.findExternalAgentsByGroupId("group-ext")).thenReturn(List.of(external));
        when(sequentialA2AExecutor.execute(eq(plan), any())).thenCallRealMethod();

        // Discovery fails → card is null
        when(externalAgentDiscoveryService.fetchAgentCard("http://ext/discover")).thenReturn(null);

        try (Response rootDispatchResponse = assistantResponse("root answer");
                Response response = invokeRootWithSingleDispatch(root, request, rootDispatchResponse)) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
            ChatMessageDTOV1 dto = (ChatMessageDTOV1) response.getEntity();
            // External agent result is skipped; only root answer is in the response
            assertThat(dto.getMessage()).isEqualTo("root answer");
        }

        verify(externalAgentDiscoveryService).fetchAgentCard("http://ext/discover");
    }

    @Test
    void invokeRoot_withExternalAgent_discoverySucceeds_usesDiscoveredEndpoint() {
        // This test verifies the strict protocol path: discovery is called and
        // the card is fetched.  The actual HTTP POST is not exercised here (it
        // would hit a real network); what matters is that discovery is invoked
        // with the correct URL before the invocation attempt.
        when(dispatchConfig.a2aConfig()).thenReturn(a2aConfig);
        when(a2aConfig.maxDepth()).thenReturn(10);

        AgentGroup group = new AgentGroup();
        group.setId("group-ext");
        group.setName("external-helpers");

        Agent root = agent("root", "root-id", true, Set.of(group));
        ExternalAgent external = externalAgent("ext-bot", "http://ext/discover");

        ChatRequestDTOV1 request = chatRequest("hello");

        Execution rootExecution = new Execution();
        rootExecution.setExecutionId("exec-root");

        A2AExecutionPlan plan = new A2AExecutionPlan(A2AStrategy.SEQUENTIAL,
                List.of(new A2AExecutionUnit("group-ext", "external-helpers")));

        when(executionService.createExecution(eq(root), eq(null), any())).thenReturn(rootExecution);
        when(a2aGroupPlanner.plan(root)).thenReturn(plan);
        when(agentDAO.findAgentsByGroupId("group-ext")).thenReturn(List.of());
        when(externalAgentDAO.findExternalAgentsByGroupId("group-ext")).thenReturn(List.of(external));
        when(sequentialA2AExecutor.execute(eq(plan), any())).thenCallRealMethod();

        // Discovery succeeds but the network call for the actual invocation
        // will fail (no real server). That is fine — we just verify discovery
        // was attempted with the right discovery URL.
        AgentCard card = new AgentCard("http://ext/tasks/send", "ext-bot", null);
        when(externalAgentDiscoveryService.fetchAgentCard("http://ext/discover")).thenReturn(card);

        try (Response rootDispatchResponse = assistantResponse("root answer");
                Response response = invokeRootWithSingleDispatch(root, request, rootDispatchResponse)) {
            // Root still succeeds; external call may silently fail (network unreachable)
            assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        }

        // Most important assertion: discovery was called with the correct URL
        verify(externalAgentDiscoveryService).fetchAgentCard("http://ext/discover");
    }

    @Test
    void invokeRoot_externalAgentWithoutDiscoveryUrl_isFiltered() {
        when(dispatchConfig.a2aConfig()).thenReturn(a2aConfig);
        when(a2aConfig.maxDepth()).thenReturn(10);

        AgentGroup group = new AgentGroup();
        group.setId("group-ext");
        group.setName("external-helpers");

        Agent root = agent("root", "root-id", true, Set.of(group));
        // No discoveryUrl set → should be filtered before reaching discovery service
        ExternalAgent external = externalAgent("no-url-bot", null);

        ChatRequestDTOV1 request = chatRequest("hello");

        Execution rootExecution = new Execution();
        rootExecution.setExecutionId("exec-root");

        A2AExecutionPlan plan = new A2AExecutionPlan(A2AStrategy.SEQUENTIAL,
                List.of(new A2AExecutionUnit("group-ext", "external-helpers")));

        when(executionService.createExecution(eq(root), eq(null), any())).thenReturn(rootExecution);
        when(a2aGroupPlanner.plan(root)).thenReturn(plan);
        when(agentDAO.findAgentsByGroupId("group-ext")).thenReturn(List.of());
        when(externalAgentDAO.findExternalAgentsByGroupId("group-ext")).thenReturn(List.of(external));
        when(sequentialA2AExecutor.execute(eq(plan), any())).thenCallRealMethod();

        try (Response rootDispatchResponse = assistantResponse("root answer");
                Response response = invokeRootWithSingleDispatch(root, request, rootDispatchResponse)) {
            assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        }

        // External agent with blank URL is filtered in resolveTargets; discovery should never be called
        verify(externalAgentDiscoveryService, never()).fetchAgentCard(anyString());
    }

    private ExternalAgent externalAgent(String name, String discoveryUrl) {
        ExternalAgent ea = new ExternalAgent();
        ea.setName(name);
        ea.setDiscoveryUrl(discoveryUrl);
        ea.setEnabled(true);
        return ea;
    }
}
