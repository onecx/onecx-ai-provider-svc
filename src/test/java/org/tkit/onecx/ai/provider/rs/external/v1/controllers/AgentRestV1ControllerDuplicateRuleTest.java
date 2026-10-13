package org.tkit.onecx.ai.provider.rs.external.v1.controllers;

import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.core.SecurityContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.domain.daos.AgentDAO;
import org.tkit.onecx.ai.provider.domain.daos.AgentMcpToolRuleDAO;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.AgentMcpToolRule;
import org.tkit.onecx.ai.provider.domain.models.GlobalTool;
import org.tkit.onecx.ai.provider.domain.models.Tool;
import org.tkit.onecx.ai.provider.domain.models.enums.ToolPermission;
import org.tkit.onecx.ai.provider.domain.models.enums.ToolType;
import org.tkit.onecx.ai.provider.rs.external.v1.mappers.AgentConfigurationMapper;
import org.tkit.onecx.ai.provider.rs.external.v1.mappers.AgentConfigurationMapperImpl;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ProblemDetailResponseDTOV1;

/**
 * Unit test (no {@code @QuarkusTest}/{@code AbstractTest}) for the duplicate tool-rule detection in
 * {@link AgentRestV1Controller}. A duplicate is two rules on the <em>same</em> MCP server with the
 * same tool name. The same tool name on <em>different</em> servers is legitimate and must not be
 * rejected.
 */
class AgentRestV1ControllerDuplicateRuleTest {

    private static final String AGENT_ID = "agent-config";

    private AgentDAO dao;

    private AgentMcpToolRuleDAO agentMcpToolRuleDAO;

    private AgentRestV1Controller controller;

    @BeforeEach
    void setUp() {
        dao = mock(AgentDAO.class);
        agentMcpToolRuleDAO = mock(AgentMcpToolRuleDAO.class);
        controller = new AgentRestV1Controller();
        controller.dao = dao;
        controller.agentMcpToolRuleDAO = agentMcpToolRuleDAO;
        controller.agentConfigurationMapper = new AgentConfigurationMapperImpl();
        controller.securityContext = mock(SecurityContext.class);
        when(controller.securityContext.getUserPrincipal()).thenReturn(() -> "tester");
    }

    @Test
    void sameToolNameAcrossDifferentServers_isNotADuplicate() {
        var toolA = mcpTool("tool-a");
        var toolB = mcpTool("tool-b");
        var agent = agentWithTools(Set.of(toolA, toolB));
        when(dao.findById(AGENT_ID)).thenReturn(agent);
        when(agentMcpToolRuleDAO.findByAgentAndToolIds(anyString(), anyList()))
                .thenReturn(List.of(
                        rule(toolA, "search"),
                        rule(toolB, "search")));

        var response = controller.getAgentConfiguration(AGENT_ID);

        assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
        // Both servers publish the same tool name legitimately, so the configuration is returned
        var servers = response.getEntity();
        assertThat(servers).isNotNull();
    }

    @Test
    void sameToolNameOnSameServer_isADuplicate() {
        var toolA = mcpTool("tool-a");
        var agent = agentWithTools(Set.of(toolA));
        when(dao.findById(AGENT_ID)).thenReturn(agent);
        when(agentMcpToolRuleDAO.findByAgentAndToolIds(anyString(), anyList()))
                .thenReturn(List.of(
                        rule(toolA, "search"),
                        rule(toolA, "search")));

        var response = controller.getAgentConfiguration(AGENT_ID);

        assertThat(response.getStatus()).isEqualTo(CONFLICT.getStatusCode());
        assertThat(response.getEntity()).isNotNull();
    }

    private Agent agentWithTools(Set<Tool> tools) {
        var agent = new Agent();
        agent.setId(AGENT_ID);
        agent.setName("agent-config");
        agent.setTenantId("default");
        agent.setTools(tools);
        agent.setGlobalTools(Set.of());
        return agent;
    }

    @Test
    void unknownAgent_returnsNotFoundWithoutLoadingRules() {
        try (var response = controller.getAgentConfiguration(AGENT_ID)) {
            assertThat(response.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
        }
        verifyNoInteractions(agentMcpToolRuleDAO);
    }

    @Test
    void nonMcpTools_doNotLoadRules() {
        var tool = mcpTool("tool-a");
        tool.setType(null);
        var globalTool = new GlobalTool();
        globalTool.setId("global-a");
        globalTool.setType(null);
        var agent = agentWithTools(Set.of(tool));
        agent.setGlobalTools(Set.of(globalTool));
        when(dao.findById(AGENT_ID)).thenReturn(agent);

        try (var response = controller.getAgentConfiguration(AGENT_ID)) {
            assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
        }
        verifyNoInteractions(agentMcpToolRuleDAO);
    }

    @Test
    void sameToolNameOnSameGlobalServer_returnsDetailedConflict() {
        var globalTool = new GlobalTool();
        globalTool.setId("global-a");
        globalTool.setType(ToolType.MCP);
        var agent = agentWithTools(Set.of());
        agent.setGlobalTools(Set.of(globalTool));
        var rule = rule(null, "search");
        rule.setGlobalTool(globalTool);
        when(dao.findById(AGENT_ID)).thenReturn(agent);
        when(agentMcpToolRuleDAO.findByAgentAndGlobalToolIds(AGENT_ID, List.of("global-a")))
                .thenReturn(List.of(rule, rule));

        try (var response = controller.getAgentConfiguration(AGENT_ID)) {
            assertThat(response.getStatus()).isEqualTo(CONFLICT.getStatusCode());
            var problem = (ProblemDetailResponseDTOV1) response.getEntity();
            assertThat(problem.getErrorCode()).isEqualTo("DUPLICATE_AGENT_TOOL_RULE");
            assertThat(problem.getDetail()).contains("'search'");
        }
    }

    @Test
    void authenticatedPrincipal_isPassedToConfigurationMapper() {
        var agent = agentWithTools(Set.of());
        when(dao.findById(AGENT_ID)).thenReturn(agent);
        Principal principal = () -> "alice";
        when(controller.securityContext.getUserPrincipal()).thenReturn(principal);
        controller.agentConfigurationMapper = mock(AgentConfigurationMapper.class);

        try (var response = controller.getAgentConfiguration(AGENT_ID)) {
            assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
        }
        verify(controller.agentConfigurationMapper).build(agent, List.of(), "default", "alice");
    }

    private Tool mcpTool(String id) {
        var tool = new Tool();
        tool.setId(id);
        tool.setName(id);
        tool.setType(ToolType.MCP);
        tool.setUrl("http://" + id + ".org");
        return tool;
    }

    private AgentMcpToolRule rule(Tool tool, String toolName) {
        var rule = new AgentMcpToolRule();
        rule.setTool(tool);
        rule.setToolName(toolName);
        rule.setToolDescription(toolName + " description");
        rule.setAllowed(ToolPermission.ALLOW);
        return rule;
    }
}
