package org.tkit.onecx.ai.provider.rs.external.v1.mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.domain.daos.AgentMcpToolRuleDAO;
import org.tkit.onecx.ai.provider.domain.daos.GlobalToolDAO;
import org.tkit.onecx.ai.provider.domain.daos.ProviderDAO;
import org.tkit.onecx.ai.provider.domain.daos.ToolDAO;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.AgentGroup;
import org.tkit.onecx.ai.provider.domain.models.AgentMcpToolRule;
import org.tkit.onecx.ai.provider.domain.models.ExternalAgent;
import org.tkit.onecx.ai.provider.domain.models.GlobalScaffold;
import org.tkit.onecx.ai.provider.domain.models.GlobalSkill;
import org.tkit.onecx.ai.provider.domain.models.GlobalTool;
import org.tkit.onecx.ai.provider.domain.models.Model;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.Scaffold;
import org.tkit.onecx.ai.provider.domain.models.Skill;
import org.tkit.onecx.ai.provider.domain.models.Tool;
import org.tkit.onecx.ai.provider.domain.models.enums.AgentGroupOrchestrationMode;
import org.tkit.onecx.ai.provider.domain.models.enums.AgentGroupResponseStrategy;
import org.tkit.onecx.ai.provider.domain.models.enums.AuthMode;
import org.tkit.onecx.ai.provider.domain.models.enums.ExecutionPolicy;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;
import org.tkit.onecx.ai.provider.domain.models.enums.ToolPermission;
import org.tkit.onecx.ai.provider.domain.models.enums.ToolType;
import org.tkit.onecx.ai.provider.test.AbstractTest;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.AgentConfigurationAgentDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.AgentConfigurationDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatMessageDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ConversationDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.McpServerConfigurationDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ModelConfigurationDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ProviderConfigurationDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ScaffoldConfigurationDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.SkillConfigurationDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ToolRuleConfigurationDTOV1;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.AgentGroupSnapshot;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.ToolRuleSnapshot;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.ToolRuleSnapshot.AllowedEnum;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.ToolSnapshot;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class RuntimeSnapshotMapperTest extends AbstractTest {

    @Inject
    RuntimeSnapshotMapper mapper;

    @InjectMock
    AgentMcpToolRuleDAO agentMcpToolRuleDAO;

    @InjectMock
    ProviderDAO providerDAO;

    @InjectMock
    ToolDAO toolDAO;

    @InjectMock
    GlobalToolDAO globalToolDAO;

    @BeforeEach
    void setUp() {
        when(agentMcpToolRuleDAO.findByAgentAndToolIds(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        when(agentMcpToolRuleDAO.findByAgentAndGlobalToolIds(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
    }

    @Test
    void mapAgent_null_returnsNull() {
        assertThat(mapper.mapAgent(null)).isNull();
        assertThat(mapper.mapAgent(null, List.of())).isNull();
    }

    @Test
    void mapAgent_withGlobalScaffold_preferredOverScaffold() {
        var agent = new Agent();
        agent.setName("agent-1");
        agent.setGlobalScaffold(globalScaffold("global-scaffold"));
        agent.setScaffold(scaffold("local-scaffold"));

        var snapshot = mapper.mapAgent(agent);
        assertThat(snapshot.getScaffold().getName()).isEqualTo("global-scaffold");
    }

    @Test
    void mapAgent_withLocalScaffold_whenNoGlobalScaffold() {
        var agent = new Agent();
        agent.setName("agent-1");
        agent.setScaffold(scaffold("local-scaffold"));

        var snapshot = mapper.mapAgent(agent);
        assertThat(snapshot.getScaffold().getName()).isEqualTo("local-scaffold");
    }

    @Test
    void mapAgent_withGroups_setsGroups() {
        var agent = new Agent();
        agent.setName("agent-1");
        var groupSnapshot = new AgentGroupSnapshot();
        groupSnapshot.setName("group-1");

        var snapshot = mapper.mapAgent(agent, List.of(groupSnapshot));
        assertThat(snapshot.getGroups()).hasSize(1);
        assertThat(snapshot.getGroups().get(0).getName()).isEqualTo("group-1");
    }

    @Test
    void mapGroup_null_returnsNull() {
        assertThat(mapper.mapGroup(null, List.of(), List.of())).isNull();
    }

    @Test
    void mapGroup_withNullAgentsAndExternalAgents_defaultsToEmptyLists() {
        var group = new AgentGroup();
        group.setName("group-1");
        group.setOrchestrationMode(AgentGroupOrchestrationMode.SUPERVISOR_ROUTED);
        group.setResponseStrategy(AgentGroupResponseStrategy.SCORED);

        var snapshot = mapper.mapGroup(group, null, null);
        assertThat(snapshot.getName()).isEqualTo("group-1");
        assertThat(snapshot.getOrchestrationMode()).isEqualTo("SUPERVISOR_ROUTED");
        assertThat(snapshot.getResponseStrategy()).isEqualTo("SCORED");
        assertThat(snapshot.getAgents()).isEmpty();
        assertThat(snapshot.getExternalAgents()).isEmpty();
    }

    @Test
    void mapGroup_withNullOrchestrationModeAndResponseStrategy_setsNull() {
        var group = new AgentGroup();
        group.setName("group-1");
        group.setOrchestrationMode(null);
        group.setResponseStrategy(null);

        var snapshot = mapper.mapGroup(group, List.of(), List.of());
        assertThat(snapshot.getOrchestrationMode()).isNull();
        assertThat(snapshot.getResponseStrategy()).isNull();
    }

    @Test
    void mapExternalAgent_null_returnsNull() {
        assertThat(mapper.mapExternalAgent(null)).isNull();
    }

    @Test
    void mapExternalAgent_withNullAuthMode_setsNull() {
        var agent = new ExternalAgent();
        agent.setName("ext-1");
        agent.setAuthMode(null);

        var snapshot = mapper.mapExternalAgent(agent);
        assertThat(snapshot.getAuthMode()).isNull();
    }

    @Test
    void mapExternalAgent_withAuthMode_setsName() {
        var agent = new ExternalAgent();
        agent.setName("ext-1");
        agent.setAuthMode(AuthMode.API_KEY);

        var snapshot = mapper.mapExternalAgent(agent);
        assertThat(snapshot.getAuthMode()).isEqualTo("API_KEY");
    }

    @Test
    void mapRuntimeChatMessage_nullMessage_returnsEmptyString() {
        var msg = mapper.mapRuntimeChatMessage(null, "conv-1");
        assertThat(msg.getMessage()).isEmpty();
        assertThat(msg.getType()).isEqualTo(ChatMessageDTOV1.TypeEnum.ASSISTANT);
        assertThat(msg.getConversationId()).isEqualTo("conv-1");
    }

    @Test
    void mapRuntimeChatMessage_preservesMessage() {
        var msg = mapper.mapRuntimeChatMessage("hello", "conv-1");
        assertThat(msg.getMessage()).isEqualTo("hello");
    }

    @Test
    void mapChatRequest_null_returnsNull() {
        assertThat(mapper.mapChatRequest(null)).isNull();
    }

    @Test
    void mapChatRequest_withNullChatMessage_setsNullChatMessage() {
        var request = new ChatRequestDTOV1();
        var result = mapper.mapChatRequest(request);
        assertThat(result.getChatMessage()).isNull();
    }

    @Test
    void mapChatRequest_withNullRequestContext_setsNullRequestContext() {
        var request = new ChatRequestDTOV1();
        var result = mapper.mapChatRequest(request);
        assertThat(result.getRequestContext()).isNull();
    }

    @Test
    void mapChatRequest_withRequestContext_mapsAiContext() {
        var request = new ChatRequestDTOV1();
        var rc = new gen.org.tkit.onecx.ai.provider.rs.external.v1.model.RequestContextDTOV1();
        rc.setAiContext(java.util.List.of("context-item"));
        request.setRequestContext(rc);

        var result = mapper.mapChatRequest(request);
        assertThat(result.getRequestContext()).isNotNull();
        assertThat(result.getRequestContext().getAiContext()).containsExactly("context-item");
    }

    @Test
    void mapChatRequest_withNullConversation_setsNullConversation() {
        var request = new ChatRequestDTOV1();
        var result = mapper.mapChatRequest(request);
        assertThat(result.getConversation()).isNull();
    }

    @Test
    void mapChatRequest_withConversation_nullConversationType_setsNull() {
        var request = new ChatRequestDTOV1();
        var conv = new ConversationDTOV1();
        conv.setConversationId("conv-1");
        conv.setConversationType(null);
        request.setConversation(conv);

        var result = mapper.mapChatRequest(request);
        assertThat(result.getConversation()).isNotNull();
        assertThat(result.getConversation().getConversationType()).isNull();
    }

    @Test
    void mapChatRequest_withConversation_nullHistory_defaultsToEmptyList() {
        var request = new ChatRequestDTOV1();
        var conv = new ConversationDTOV1();
        conv.setConversationId("conv-1");
        conv.setConversationType(ConversationDTOV1.ConversationTypeEnum.Q_AND_A);
        conv.setHistory(null);
        request.setConversation(conv);

        var result = mapper.mapChatRequest(request);
        assertThat(result.getConversation().getHistory()).isEmpty();
        assertThat(result.getConversation().getConversationType()).isEqualTo("Q_AND_A");
    }

    @Test
    void mapChatRequest_withConversation_andHistory_mapsMessages() {
        var request = new ChatRequestDTOV1();
        var conv = new ConversationDTOV1();
        conv.setConversationId("conv-1");
        var historyMsg = new ChatMessageDTOV1();
        historyMsg.setType(ChatMessageDTOV1.TypeEnum.USER);
        historyMsg.setMessage("hello");
        historyMsg.setConversationId("conv-1");
        conv.setHistory(List.of(historyMsg));
        request.setConversation(conv);

        var result = mapper.mapChatRequest(request);
        assertThat(result.getConversation().getHistory()).hasSize(1);
        assertThat(result.getConversation().getHistory().get(0).getMessage()).isEqualTo("hello");
    }

    @Test
    void mapChatMessage_null_returnsNull() {
        assertThat(mapper.mapChatMessage((ChatMessageDTOV1) null)).isNull();
    }

    @Test
    void mapChatMessage_withNullType_setsNull() {
        var msg = new ChatMessageDTOV1();
        msg.setMessage("hello");
        msg.setType(null);

        var result = mapper.mapChatMessage(msg);
        assertThat(result.getType()).isNull();
        assertThat(result.getMessage()).isEqualTo("hello");
    }

    @Test
    void mapModel_null_returnsNull() {
        assertThat(mapper.mapModel(null)).isNull();
    }

    @Test
    void mapModel_withNullCommunicationMode_setsNull() {
        var model = new Model();
        model.setName("model-1");
        model.setCommunicationMode(null);

        var snapshot = mapper.mapModel(model);
        assertThat(snapshot.getCommunicationMode()).isNull();
    }

    @Test
    void mapModel_preservesModelConfigVerbatim() {
        var modelConfig = "{\"api\":\"responses\", \"reasoningEffort\":\" medium \"}";
        var model = new Model();
        model.setModelConfig(modelConfig);

        assertThat(mapper.mapModel(model).getModelConfig()).isEqualTo(modelConfig);
    }

    @Test
    void mapModel_withNullProvider_setsNullProvider() {
        var model = new Model();
        model.setName("model-1");
        model.setProvider(null);

        var snapshot = mapper.mapModel(model);
        assertThat(snapshot.getProvider()).isNull();
    }

    @Test
    void mapProvider_null_returnsNull() {
        assertThat(mapper.mapProvider(null)).isNull();
    }

    @Test
    void mapProvider_withNullType_setsNull() {
        var provider = new Provider();
        provider.setName("p-1");
        provider.setType(null);

        var snapshot = mapper.mapProvider(provider);
        assertThat(snapshot.getType()).isNull();
    }

    @Test
    void mapProvider_withNullAuthMode_setsNull() {
        var provider = new Provider();
        provider.setName("p-1");
        provider.setAuthMode(null);

        var snapshot = mapper.mapProvider(provider);
        assertThat(snapshot.getAuthMode()).isNull();
    }

    @Test
    void mapProvider_withTypeAndAuthMode_mapsNames() {
        var provider = new Provider();
        provider.setType(ProviderType.OPENAI);
        provider.setAuthMode(AuthMode.API_KEY);

        var snapshot = mapper.mapProvider(provider);

        assertThat(snapshot.getType()).isEqualTo("OPENAI");
        assertThat(snapshot.getAuthMode()).isEqualTo("API_KEY");
    }

    @Test
    void mapScaffold_null_returnsNull() {
        assertThat(mapper.mapScaffold(null)).isNull();
    }

    @Test
    void mapScaffold_withNullSkills_returnsEmptySkills() {
        var scaffold = new Scaffold();
        scaffold.setName("s-1");
        scaffold.setSkills(null);
        scaffold.setGlobalSkills(null);

        var snapshot = mapper.mapScaffold(scaffold);
        assertThat(snapshot.getSkills()).isEmpty();
    }

    @Test
    void mapScaffold_withSkillsAndGlobalSkills_mergesBoth() {
        var scaffold = new Scaffold();
        scaffold.setName("s-1");
        scaffold.setSkills(Set.of(skill("local-skill")));
        scaffold.setGlobalSkills(Set.of(globalSkill("global-skill")));

        var snapshot = mapper.mapScaffold(scaffold);
        assertThat(snapshot.getSkills()).hasSize(2);
    }

    @Test
    void mapGlobalScaffold_null_returnsNull() {
        assertThat(mapper.mapGlobalScaffold(null)).isNull();
    }

    @Test
    void mapGlobalScaffold_withNullSkills_returnsEmptySkills() {
        var scaffold = new GlobalScaffold();
        scaffold.setName("gs-1");
        scaffold.setSkills(null);

        var snapshot = mapper.mapGlobalScaffold(scaffold);
        assertThat(snapshot.getSkills()).isEmpty();
    }

    @Test
    void mapGlobalScaffold_withSkills_mapsSkills() {
        var scaffold = new GlobalScaffold();
        scaffold.setName("gs-1");
        scaffold.setSkills(Set.of(globalSkill("g-skill")));

        var snapshot = mapper.mapGlobalScaffold(scaffold);
        assertThat(snapshot.getSkills()).hasSize(1);
        assertThat(snapshot.getSkills().get(0).getName()).isEqualTo("g-skill");
    }

    @Test
    void mapTools_agentWithNullToolsAndNullGlobalTools_returnsEmptyList() {
        var agent = new Agent();
        agent.setId("agent-1");

        var tools = mapper.mapTools(agent);
        assertThat(tools).isEmpty();
    }

    @Test
    void mapTools_agentWithTools_mapsToolRules() {
        var tool = new Tool();
        tool.setId("tool-1");
        tool.setName("searchTool");
        tool.setType(ToolType.MCP);
        tool.setUrl("http://mcp.local");
        tool.setAuthMode(AuthMode.API_KEY);
        tool.setExecutionPolicy(ExecutionPolicy.ALWAYS_ASK);

        var agent = new Agent();
        agent.setId("agent-1");
        agent.setTools(Set.of(tool));

        var rule = new AgentMcpToolRule();
        rule.setTool(tool);
        rule.setToolName("searchTool");
        rule.setAllowed(ToolPermission.ALLOW);

        when(agentMcpToolRuleDAO.findByAgentAndToolIds("agent-1", List.of("tool-1")))
                .thenReturn(List.of(rule));

        var tools = mapper.mapTools(agent);
        assertThat(tools).hasSize(1);
        assertThat(tools.get(0).getName()).isEqualTo("searchTool");
        assertThat(tools.get(0).getType()).isEqualTo("MCP");
        assertThat(tools.get(0).getAuthMode()).isEqualTo("API_KEY");
        assertThat(tools.get(0).getExecutionPolicy()).isEqualTo(ToolSnapshot.ExecutionPolicyEnum.ALWAYS_ASK);
        assertThat(tools.get(0).getToolRules()).hasSize(1);
        assertThat(tools.get(0).getToolRules().get(0).getAllowed()).isEqualTo(
                gen.org.tkit.onecx.ai.provider.runtime.client.model.ToolRuleSnapshot.AllowedEnum.ALLOW);
    }

    @Test
    void mapTools_agentWithLegacyAllowPolicies_mapsRuntimeCompatibilityValues() {
        var tool = new Tool();
        tool.setId("tool-1");
        tool.setName("searchTool");
        tool.setType(ToolType.MCP);
        tool.setUrl("http://mcp.local");
        tool.setAuthMode(AuthMode.API_KEY);
        tool.setExecutionPolicy(ExecutionPolicy.NEVER_ASK);

        var agent = new Agent();
        agent.setId("agent-1");
        agent.setTools(Set.of(tool));

        var rule = new AgentMcpToolRule();
        rule.setTool(tool);
        rule.setToolName("searchTool");
        rule.setAllowed(ToolPermission.NEVER_ASK);

        when(agentMcpToolRuleDAO.findByAgentAndToolIds("agent-1", List.of("tool-1")))
                .thenReturn(List.of(rule));

        var tools = mapper.mapTools(agent);
        assertThat(tools).hasSize(1);
        assertThat(tools.get(0).getExecutionPolicy()).isEqualTo(ToolSnapshot.ExecutionPolicyEnum.NEVER_ASK);
        assertThat(tools.get(0).getToolRules()).hasSize(1);
        assertThat(tools.get(0).getToolRules().get(0).getAllowed()).isEqualTo(
                AllowedEnum.ALLOW);
    }

    @Test
    void mapTools_agentWithGlobalTools_mapsGlobalToolRules() {
        var globalTool = new GlobalTool();
        globalTool.setId("gtool-1");
        globalTool.setName("globalRead");
        globalTool.setType(ToolType.MCP);
        globalTool.setUrl("http://mcp.global");
        globalTool.setAuthMode(null);
        globalTool.setExecutionPolicy(null);

        var agent = new Agent();
        agent.setId("agent-1");
        agent.setGlobalTools(Set.of(globalTool));

        var rule = new AgentMcpToolRule();
        rule.setGlobalTool(globalTool);
        rule.setToolName("globalRead");
        rule.setAllowed(null);

        when(agentMcpToolRuleDAO.findByAgentAndGlobalToolIds("agent-1", List.of("gtool-1")))
                .thenReturn(List.of(rule));

        var tools = mapper.mapTools(agent);
        assertThat(tools).hasSize(1);
        assertThat(tools.get(0).getName()).isEqualTo("globalRead");
        assertThat(tools.get(0).getAuthMode()).isNull();
        assertThat(tools.get(0).getExecutionPolicy()).isEqualTo(ToolSnapshot.ExecutionPolicyEnum.ALWAYS_ASK);
        assertThat(tools.get(0).getToolRules()).hasSize(1);
        assertThat(tools.get(0).getToolRules().get(0).getAllowed()).isEqualTo(
                AllowedEnum.ALWAYS_ASK);
    }

    @Test
    void mapTools_agentWithNullToolType_setsNullType() {
        var tool = new Tool();
        tool.setId("tool-1");
        tool.setName("toolNoType");
        tool.setType(null);

        var agent = new Agent();
        agent.setId("agent-1");
        agent.setTools(Set.of(tool));

        var tools = mapper.mapTools(agent);
        assertThat(tools.get(0).getType()).isNull();
    }

    @Test
    void mapTools_ignoresRulesWithoutMatchingToolReferences() {
        var tool = new Tool();
        tool.setId("tool-1");
        tool.setName("tenant-tool");
        var globalTool = new GlobalTool();
        globalTool.setId("global-tool-1");
        globalTool.setName("global-tool");
        var agent = new Agent();
        agent.setId("agent-1");
        agent.setTools(Set.of(tool));
        agent.setGlobalTools(Set.of(globalTool));

        when(agentMcpToolRuleDAO.findByAgentAndToolIds("agent-1", List.of("tool-1")))
                .thenReturn(List.of(new AgentMcpToolRule()));
        when(agentMcpToolRuleDAO.findByAgentAndGlobalToolIds("agent-1", List.of("global-tool-1")))
                .thenReturn(List.of(new AgentMcpToolRule()));

        var tools = mapper.mapTools(agent);

        assertThat(tools).hasSize(2).allSatisfy(snapshot -> assertThat(snapshot.getToolRules()).isEmpty());
    }

    @Test
    void toRuntimeRequest_mapsActiveVersionWithLiveSecrets() {
        var provider = new Provider();
        provider.setApiKey("provider-secret");
        when(providerDAO.findById("provider-1")).thenReturn(provider);
        var tool = liveTool("tool-1", ToolType.MCP, AuthMode.API_KEY, "tool-secret");
        when(toolDAO.findById("tool-1")).thenReturn(tool);

        var configuration = activeConfiguration();
        var server = new McpServerConfigurationDTOV1();
        server.setId("tool-1");
        server.setName("searchTool");
        server.setUrl("http://mcp.local");
        server.setAuthMode("API_KEY");
        server.setUnlistedToolPermission(McpServerConfigurationDTOV1.UnlistedToolPermissionEnum.ALWAYS_ALLOW);
        configuration.setMcpServers(List.of(server));

        configuration.getAgent().setAdditionalPrompt("extra");
        configuration.getAgent().setA2aEnabled(true);

        var request = new ChatRequestDTOV1();
        var msg = new ChatMessageDTOV1();
        msg.setType(ChatMessageDTOV1.TypeEnum.USER);
        msg.setMessage("hello");
        msg.setConversationId("conv-1");
        request.setChatMessage(msg);

        var result = mapper.toRuntimeRequest(configuration, request, List.of());
        var root = result.getRootAgent();
        assertThat(root.getName()).isEqualTo("agent-1");
        assertThat(root.getDescription()).isEqualTo("agent description");
        assertThat(root.getAdditionalPrompt()).isEqualTo("extra");
        assertThat(root.getA2aEnabled()).isTrue();
        assertThat(root.getModel().getModelIdentifier()).isEqualTo("mistral");
        assertThat(root.getModel().getProvider().getType()).isEqualTo("OLLAMA");
        assertThat(root.getModel().getProvider().getApiKey()).isEqualTo("provider-secret");
        assertThat(root.getScaffold().getName()).isEqualTo("scaffold-1");
        assertThat(root.getScaffold().getSkills()).hasSize(1);
        assertThat(root.getScaffold().getSkills().get(0).getName()).isEqualTo("skill-1");
        assertThat(root.getTools()).hasSize(1);
        assertThat(root.getTools().get(0).getApiKey()).isEqualTo("tool-secret");
        assertThat(root.getTools().get(0).getType()).isEqualTo("MCP");
        assertThat(root.getTools().get(0).getAuthMode()).isEqualTo("API_KEY");
        assertThat(root.getTools().get(0).getExecutionPolicy()).isEqualTo(ToolSnapshot.ExecutionPolicyEnum.NEVER_ASK);
        assertThat(result.getChatRequest().getChatMessage().getMessage()).isEqualTo("hello");
    }

    @Test
    void toRuntimeRequest_emptyConfiguration_mapsDefaults() {
        var groups = List.<AgentGroupSnapshot> of();

        var result = mapper.toRuntimeRequest(new AgentConfigurationDTOV1(), new ChatRequestDTOV1(), groups);
        assertThat(result.getRootAgent().getName()).isNull();
        assertThat(result.getRootAgent().getDescription()).isNull();
        assertThat(result.getRootAgent().getModel()).isNull();
        assertThat(result.getRootAgent().getScaffold()).isNull();
        assertThat(result.getRootAgent().getTools()).isEmpty();
        assertThat(result.getRootAgent().getGroups()).isSameAs(groups);
    }

    @Test
    void toRuntimeRequest_scaffoldWithoutSkills_mapsEmptySkills() {
        var configuration = new AgentConfigurationDTOV1();
        var scaffold = new ScaffoldConfigurationDTOV1();
        scaffold.setName("scaffold-1");
        configuration.setScaffold(scaffold);

        var result = mapper.toRuntimeRequest(configuration, new ChatRequestDTOV1(), List.of());
        assertThat(result.getRootAgent().getScaffold().getName()).isEqualTo("scaffold-1");
        assertThat(result.getRootAgent().getScaffold().getSkills()).isEmpty();
    }

    @Test
    void toRuntimeRequest_missingLiveProvider_failsWithConflict() {
        when(providerDAO.findById("provider-1")).thenReturn(null);

        var configuration = activeConfiguration();
        var request = new ChatRequestDTOV1();
        var error = assertThrows(WebApplicationException.class,
                () -> mapper.toRuntimeRequest(configuration, request, List.of()));
        assertThat(error.getResponse().getStatus()).isEqualTo(Response.Status.CONFLICT.getStatusCode());
        assertThat(error.getMessage()).contains("provider-1");
    }

    @Test
    void toRuntimeRequest_modelWithoutProvider_hasNoProvider() {
        var configuration = activeConfiguration();
        configuration.setProvider(null);

        var result = mapper.toRuntimeRequest(configuration, new ChatRequestDTOV1(), List.of());
        assertThat(result.getRootAgent().getModel().getProvider()).isNull();
        verify(providerDAO, never()).findById(any());
    }

    @Test
    void toRuntimeRequest_toolLookup_fallsBackToGlobalTool() {
        var globalTool = new GlobalTool();
        globalTool.setType(ToolType.HTTP);
        when(toolDAO.findById("global-1")).thenReturn(null);
        when(globalToolDAO.findById("global-1")).thenReturn(globalTool);

        var configuration = new AgentConfigurationDTOV1();
        configuration.setMcpServers(List.of(
                server("global-1", McpServerConfigurationDTOV1.UnlistedToolPermissionEnum.DENY, null)));

        var tools = mapper.toRuntimeRequest(configuration, new ChatRequestDTOV1(), List.of())
                .getRootAgent().getTools();
        assertThat(tools).hasSize(1);
        assertThat(tools.get(0).getName()).isEqualTo("global-1-name");
        assertThat(tools.get(0).getType()).isEqualTo("HTTP");
        assertThat(tools.get(0).getAuthMode()).isNull();
        assertThat(tools.get(0).getExecutionPolicy()).isEqualTo(ToolSnapshot.ExecutionPolicyEnum.ALWAYS_ASK);
        assertThat(tools.get(0).getToolRules()).isEmpty();
    }

    @Test
    void toRuntimeRequest_unknownTool_failsWithConflict() {
        when(toolDAO.findById("tool-1")).thenReturn(null);
        when(globalToolDAO.findById("tool-1")).thenReturn(null);

        var configuration = new AgentConfigurationDTOV1();
        configuration.setMcpServers(List.of(
                server("tool-1", McpServerConfigurationDTOV1.UnlistedToolPermissionEnum.ALWAYS_ASK, List.of())));

        var request = new ChatRequestDTOV1();
        var error = assertThrows(WebApplicationException.class,
                () -> mapper.toRuntimeRequest(configuration, request, List.of()));
        assertThat(error.getResponse().getStatus()).isEqualTo(Response.Status.CONFLICT.getStatusCode());
        assertThat(error.getMessage()).contains("tool-1");
    }

    @Test
    void toRuntimeRequest_toolWithoutLiveSecrets_mapsMcpTypeAndNullApiKey() {
        when(toolDAO.findById("tool-1")).thenReturn(liveTool("tool-1", ToolType.MCP, null, null));

        var configuration = new AgentConfigurationDTOV1();
        configuration.setMcpServers(List.of(server("tool-1", null, List.of())));

        var tool = mapper.toRuntimeRequest(configuration, new ChatRequestDTOV1(), List.of())
                .getRootAgent().getTools().get(0);
        assertThat(tool.getType()).isEqualTo("MCP");
        assertThat(tool.getAuthMode()).isNull();
        assertThat(tool.getApiKey()).isNull();
        assertThat(tool.getExecutionPolicy()).isEqualTo(ToolSnapshot.ExecutionPolicyEnum.ALWAYS_ASK);
    }

    @Test
    void toRuntimeRequest_toolRulePermissions_mapToRuntimeAllowed() {
        when(toolDAO.findById("tool-1")).thenReturn(liveTool("tool-1", ToolType.MCP, null, null));

        var configuration = new AgentConfigurationDTOV1();
        configuration.setMcpServers(List.of(server("tool-1", null, List.of(
                rule("allow", ToolRuleConfigurationDTOV1.PermissionEnum.ALWAYS_ALLOW),
                rule("deny", ToolRuleConfigurationDTOV1.PermissionEnum.DENY),
                rule("ask", ToolRuleConfigurationDTOV1.PermissionEnum.ALWAYS_ASK)))));

        var rules = mapper.toRuntimeRequest(configuration, new ChatRequestDTOV1(), List.of())
                .getRootAgent().getTools().get(0).getToolRules();
        assertThat(rules).extracting(ToolRuleSnapshot::getAllowed).containsExactly(
                ToolRuleSnapshot.AllowedEnum.ALLOW,
                ToolRuleSnapshot.AllowedEnum.DENY,
                ToolRuleSnapshot.AllowedEnum.ALWAYS_ASK);
        assertThat(rules).extracting(ToolRuleSnapshot::getToolName).containsExactly("allow", "deny", "ask");
    }

    @Test
    void toRuntimeRequest_unlistedToolPermissionDeny_mapsToAlwaysAsk() {
        when(toolDAO.findById("tool-1")).thenReturn(liveTool("tool-1", ToolType.MCP, null, null));

        var configuration = new AgentConfigurationDTOV1();
        configuration.setMcpServers(List.of(
                server("tool-1", McpServerConfigurationDTOV1.UnlistedToolPermissionEnum.DENY, List.of())));

        var tool = mapper.toRuntimeRequest(configuration, new ChatRequestDTOV1(), List.of())
                .getRootAgent().getTools().get(0);
        assertThat(tool.getExecutionPolicy()).isEqualTo(ToolSnapshot.ExecutionPolicyEnum.ALWAYS_ASK);
    }

    private AgentConfigurationDTOV1 activeConfiguration() {
        var agentDto = new AgentConfigurationAgentDTOV1();
        agentDto.setName("agent-1");
        agentDto.setDescription("agent description");

        var provider = new ProviderConfigurationDTOV1();
        provider.setId("provider-1");
        provider.setName("ollama");
        provider.setType("OLLAMA");
        provider.setLlmUrl("http://ollama.local");
        provider.setAuthMode("NONE");

        var model = new ModelConfigurationDTOV1();
        model.setId("model-1");
        model.setName("mistral-model");
        model.setModelIdentifier("mistral");
        model.setCommunicationMode("TEXT");

        var skill = new SkillConfigurationDTOV1();
        skill.setId("skill-1");
        skill.setName("skill-1");
        skill.setInstruction("do things");

        var scaffold = new ScaffoldConfigurationDTOV1();
        scaffold.setId("scaffold-1");
        scaffold.setName("scaffold-1");
        scaffold.setSystemPrompt("prompt");

        var configuration = new AgentConfigurationDTOV1();
        configuration.setAgent(agentDto);
        configuration.setProvider(provider);
        configuration.setModel(model);
        configuration.setScaffold(scaffold);
        configuration.setSkills(List.of(skill));
        return configuration;
    }

    private McpServerConfigurationDTOV1 server(String id, McpServerConfigurationDTOV1.UnlistedToolPermissionEnum permission,
            List<ToolRuleConfigurationDTOV1> rules) {
        var server = new McpServerConfigurationDTOV1();
        server.setId(id);
        server.setName(id + "-name");
        server.setUnlistedToolPermission(permission);
        server.setToolRules(rules);
        return server;
    }

    private ToolRuleConfigurationDTOV1 rule(String toolName, ToolRuleConfigurationDTOV1.PermissionEnum permission) {
        var rule = new ToolRuleConfigurationDTOV1();
        rule.setToolName(toolName);
        rule.setPermission(permission);
        return rule;
    }

    private Tool liveTool(String id, ToolType type, AuthMode authMode, String apiKey) {
        var tool = new Tool();
        tool.setId(id);
        tool.setType(type);
        tool.setAuthMode(authMode);
        tool.setApiKey(apiKey);
        return tool;
    }

    private Scaffold scaffold(String name) {
        var s = new Scaffold();
        s.setName(name);
        s.setSystemPrompt("prompt");
        return s;
    }

    @Test
    void mapTools_ignoresRulesWithoutMatchingToolRelationships() {
        var tool = new Tool();
        tool.setId("tool-1");
        var globalTool = new GlobalTool();
        globalTool.setId("global-1");
        var agent = new Agent();
        agent.setId("agent-1");
        agent.setTools(Set.of(tool));
        agent.setGlobalTools(Set.of(globalTool));
        var orphanRule = new AgentMcpToolRule();
        orphanRule.setToolName("orphan");
        when(agentMcpToolRuleDAO.findByAgentAndToolIds("agent-1", List.of("tool-1")))
                .thenReturn(List.of(orphanRule));
        when(agentMcpToolRuleDAO.findByAgentAndGlobalToolIds("agent-1", List.of("global-1")))
                .thenReturn(List.of(orphanRule));

        var tools = mapper.mapTools(agent);

        assertThat(tools).hasSize(2)
                .allSatisfy(snapshot -> assertThat(snapshot.getToolRules()).isEmpty());
    }

    @Test
    void mapTool_nullRules_returnsEmptyRules() {
        var tool = new Tool();
        tool.setName("search");

        var snapshot = mapper.mapTool(tool, null);

        assertThat(snapshot.getName()).isEqualTo("search");
        assertThat(snapshot.getToolRules()).isEmpty();
    }

    private GlobalScaffold globalScaffold(String name) {
        var s = new GlobalScaffold();
        s.setName(name);
        s.setSystemPrompt("prompt");
        return s;
    }

    private Skill skill(String name) {
        var s = new Skill();
        s.setName(name);
        s.setDescription("desc");
        s.setInstruction("instruction");
        return s;
    }

    private GlobalSkill globalSkill(String name) {
        var s = new GlobalSkill();
        s.setName(name);
        s.setDescription("desc");
        s.setInstruction("instruction");
        return s;
    }
}
