package org.tkit.onecx.ai.provider.rs.external.v1.controllers;

import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.core.SecurityContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.domain.daos.AgentConfigurationVersionDAO;
import org.tkit.onecx.ai.provider.domain.daos.AgentDAO;
import org.tkit.onecx.ai.provider.domain.daos.AgentMcpToolRuleDAO;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.AgentConfigurationVersion;
import org.tkit.onecx.ai.provider.domain.models.AgentMcpToolRule;
import org.tkit.onecx.ai.provider.domain.models.GlobalTool;
import org.tkit.onecx.ai.provider.domain.models.Tool;
import org.tkit.onecx.ai.provider.domain.models.enums.ToolPermission;
import org.tkit.onecx.ai.provider.domain.models.enums.ToolType;
import org.tkit.onecx.ai.provider.rs.external.v1.mappers.AgentConfigurationMapper;
import org.tkit.onecx.ai.provider.rs.external.v1.mappers.AgentConfigurationMapperImpl;

import com.fasterxml.jackson.databind.ObjectMapper;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.AgentConfigurationDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.AgentConfigurationVersionDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.CreateAgentConfigurationVersionDTOV1;
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
        controller.objectMapper = new ObjectMapper().findAndRegisterModules();
        controller.versionDAO = mock(AgentConfigurationVersionDAO.class);
        var versions = new HashMap<String, AgentConfigurationVersion>();
        doAnswer(invocation -> {
            AgentConfigurationVersion version = invocation.getArgument(0);
            versions.put(version.getId(), version);
            return null;
        }).when(controller.versionDAO).create(any());
        when(controller.versionDAO.existsByAgentAndName(anyString(), anyString())).thenAnswer(invocation -> versions.values()
                .stream().anyMatch(version -> version.getAgentId().equals(invocation.getArgument(0))
                        && version.getName().equals(invocation.getArgument(1))));
        when(controller.versionDAO.findByAgentAndId(anyString(), anyString())).thenAnswer(invocation -> {
            var version = versions.get(invocation.getArgument(1));
            return version != null && version.getAgentId().equals(invocation.getArgument(0)) ? version : null;
        });
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

    @Test
    void versionsRemainStableAcrossLiveEditsAndActivation() {
        var agent = agentWithTools(Set.of());
        when(dao.findById(AGENT_ID)).thenReturn(agent);
        var first = saveVersion("First");
        assertThat(first.getActive()).isTrue();

        agent.setName("Changed draft");
        var second = saveVersion("Second");
        assertThat(second.getActive()).isFalse();
        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(activeConfiguration().getAgent().getName()).isEqualTo("agent-config");

        try (var response = controller.activateAgentConfigurationVersion(AGENT_ID, second.getId())) {
            assertThat(response.getStatus()).isEqualTo(200);
        }
        assertThat(activeConfiguration().getAgent().getName()).isEqualTo("Changed draft");
        try (var response = controller.getAgentConfigurationVersion(AGENT_ID, first.getId())) {
            assertThat(((AgentConfigurationDTOV1) response.getEntity()).getAgent().getName()).isEqualTo("agent-config");
            assertThat(response.getHeaderString("Content-Location")).endsWith("/" + first.getId());
        }
        controller.activateAgentConfigurationVersion(AGENT_ID, first.getId()).close();
        assertThat(agent.getActiveConfigurationVersionId()).isEqualTo(first.getId());
        assertThat(activeConfiguration().getAgent().getName()).isEqualTo("agent-config");
    }

    @Test
    void repeatedReadsInitializeOnlyOneStableDefaultVersion() {
        var agent = agentWithTools(Set.of());
        when(dao.findById(AGENT_ID)).thenReturn(agent);
        assertThat(activeConfiguration().getAgent().getName()).isEqualTo("agent-config");
        var initialId = agent.getActiveConfigurationVersionId();
        agent.setName("Unsaved draft");
        assertThat(activeConfiguration().getAgent().getName()).isEqualTo("agent-config");
        assertThat(agent.getActiveConfigurationVersionId()).isEqualTo(initialId);
        verify(controller.versionDAO).create(any());
    }

    @Test
    void identicalConfigurationsStillGetUniqueVersionIds() {
        when(dao.findById(AGENT_ID)).thenReturn(agentWithTools(Set.of()));
        assertThat(saveVersion("First").getId()).isNotEqualTo(saveVersion("Second").getId());
    }

    @Test
    void duplicateNamesAreRejectedAfterTrimmingWithoutChangingActiveVersion() {
        var agent = agentWithTools(Set.of());
        when(dao.findById(AGENT_ID)).thenReturn(agent);
        var first = saveVersion("Release 1");
        for (var name : List.of("Release 1", "  Release 1  ")) {
            var request = new CreateAgentConfigurationVersionDTOV1();
            request.setName(name);
            try (var response = controller.createAgentConfigurationVersion(AGENT_ID, request)) {
                assertThat(response.getStatus()).isEqualTo(CONFLICT.getStatusCode());
                assertThat(((ProblemDetailResponseDTOV1) response.getEntity()).getErrorCode())
                        .isEqualTo("DUPLICATE_AGENT_CONFIGURATION_VERSION_NAME");
            }
        }
        verify(controller.versionDAO).create(any());
        assertThat(agent.getActiveConfigurationVersionId()).isEqualTo(first.getId());
    }

    @Test
    void savedConfigurationUsesRetrievingPrincipal() {
        when(dao.findById(AGENT_ID)).thenReturn(agentWithTools(Set.of()));
        var version = saveVersion("First");
        when(controller.securityContext.getUserPrincipal()).thenReturn(() -> "runtime-user");
        try (var response = controller.getAgentConfigurationVersion(AGENT_ID, version.getId())) {
            assertThat(((AgentConfigurationDTOV1) response.getEntity()).getContext().getPrincipal())
                    .isEqualTo("runtime-user");
        }
    }

    @Test
    void foreignAndMissingVersionsCannotBeReadOrActivated() {
        var agent = agentWithTools(Set.of());
        when(dao.findById(AGENT_ID)).thenReturn(agent);
        var version = saveVersion("First");
        var other = agentWithTools(Set.of());
        other.setId("other-agent");
        when(dao.findById("other-agent")).thenReturn(other);
        try (var response = controller.getAgentConfigurationVersion("other-agent", version.getId())) {
            assertThat(response.getStatus()).isEqualTo(404);
        }
        try (var response = controller.activateAgentConfigurationVersion("other-agent", version.getId())) {
            assertThat(response.getStatus()).isEqualTo(404);
        }
        try (var response = controller.activateAgentConfigurationVersion(AGENT_ID, "missing")) {
            assertThat(response.getStatus()).isEqualTo(404);
        }
        try (var response = controller.getAgentConfigurationVersion(AGENT_ID, "missing")) {
            assertThat(response.getStatus()).isEqualTo(404);
        }
        assertThat(agent.getActiveConfigurationVersionId()).isEqualTo(version.getId());
        assertThat(other.getActiveConfigurationVersionId()).isNull();
    }

    @Test
    void invalidVersionNamesAreRejectedWithoutPersistence() {
        for (var name : List.of("", " ", "a".repeat(256))) {
            var request = new CreateAgentConfigurationVersionDTOV1();
            request.setName(name);
            try (var response = controller.createAgentConfigurationVersion(AGENT_ID, request)) {
                assertThat(response.getStatus()).isEqualTo(400);
            }
        }
        try (var response = controller.createAgentConfigurationVersion(AGENT_ID, null)) {
            assertThat(response.getStatus()).isEqualTo(400);
        }
        verifyNoInteractions(controller.versionDAO);
    }

    @Test
    void duplicateRulesPreventVersionCreation() {
        var tool = mcpTool("tool-a");
        var agent = agentWithTools(Set.of(tool));
        when(dao.findById(AGENT_ID)).thenReturn(agent);
        when(agentMcpToolRuleDAO.findByAgentAndToolIds(anyString(), anyList()))
                .thenReturn(List.of(rule(tool, "search"), rule(tool, "search")));
        var request = new CreateAgentConfigurationVersionDTOV1();
        request.setName("Invalid");
        try (var response = controller.createAgentConfigurationVersion(AGENT_ID, request)) {
            assertThat(response.getStatus()).isEqualTo(409);
        }
        verify(controller.versionDAO, never()).create(any());
        assertThat(agent.getActiveConfigurationVersionId()).isNull();
    }

    private AgentConfigurationVersionDTOV1 saveVersion(String name) {
        var request = new CreateAgentConfigurationVersionDTOV1();
        request.setName(name);
        try (var response = controller.createAgentConfigurationVersion(AGENT_ID, request)) {
            assertThat(response.getStatus()).isEqualTo(201);
            var version = (AgentConfigurationVersionDTOV1) response.getEntity();
            assertThat(version.getName()).isEqualTo(name);
            assertThat(response.getLocation().toString()).endsWith("/" + version.getId());
            return version;
        }
    }

    private AgentConfigurationDTOV1 activeConfiguration() {
        try (var response = controller.getAgentConfiguration(AGENT_ID)) {
            assertThat(response.getStatus()).isEqualTo(200);
            return (AgentConfigurationDTOV1) response.getEntity();
        }
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
