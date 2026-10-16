package org.tkit.onecx.ai.provider.rs.external.v1.controllers;

import static jakarta.transaction.Transactional.TxType.NOT_SUPPORTED;
import static jakarta.transaction.Transactional.TxType.REQUIRED;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriBuilder;

import org.tkit.onecx.ai.provider.domain.daos.AgentConfigurationVersionDAO;
import org.tkit.onecx.ai.provider.domain.daos.AgentDAO;
import org.tkit.onecx.ai.provider.domain.daos.AgentMcpToolRuleDAO;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.AgentConfigurationVersion;
import org.tkit.onecx.ai.provider.domain.models.AgentMcpToolRule;
import org.tkit.onecx.ai.provider.domain.models.GlobalTool;
import org.tkit.onecx.ai.provider.domain.models.Tool;
import org.tkit.onecx.ai.provider.domain.models.enums.ToolType;
import org.tkit.onecx.ai.provider.rs.external.v1.mappers.AgentConfigurationMapper;
import org.tkit.onecx.ai.provider.rs.external.v1.mappers.AgentMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.AgentV1Api;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.AgentConfigurationDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.AgentConfigurationVersionDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.AgentSearchCriteriaDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.CreateAgentConfigurationVersionDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ProblemDetailResponseDTOV1;

@ApplicationScoped
@Transactional(value = NOT_SUPPORTED)
public class AgentRestV1Controller implements AgentV1Api {

    private static final String ERROR_DUPLICATE_TOOL_RULE = "DUPLICATE_AGENT_TOOL_RULE";

    @Inject
    AgentDAO dao;

    @Inject
    AgentMcpToolRuleDAO agentMcpToolRuleDAO;

    @Inject
    AgentMapper mapper;

    @Inject
    AgentConfigurationMapper agentConfigurationMapper;

    @Inject
    AgentConfigurationVersionDAO versionDAO;

    @Inject
    ObjectMapper objectMapper;

    @Context
    SecurityContext securityContext;

    @Override
    public Response findAgentBySearchCriteria(AgentSearchCriteriaDTOV1 agentSearchCriteriaDTO) {
        var criteria = mapper.mapCriteria(agentSearchCriteriaDTO);
        var result = dao.findAgentsByCriteria(criteria);
        return Response.ok(mapper.mapPage(result)).build();
    }

    @Override
    @Transactional(REQUIRED)
    public Response getAgentConfiguration(String id) {
        Agent agent = dao.findById(id);
        if (agent == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        dao.lockConfiguration(agent);
        if (agent.getActiveConfigurationVersionId() == null) {
            try (var resolved = resolveConfiguration(agent)) {
                if (resolved.getStatus() != Response.Status.OK.getStatusCode()) {
                    return Response.fromResponse(resolved).build();
                }
                saveVersion(agent, "Initial configuration", (AgentConfigurationDTOV1) resolved.getEntity());
            }
        }
        return configuration(versionDAO.findByAgentAndId(id, agent.getActiveConfigurationVersionId()));
    }

    @Override
    @Transactional(REQUIRED)
    public Response createAgentConfigurationVersion(String id, CreateAgentConfigurationVersionDTOV1 request) {
        if (request == null || request.getName() == null || request.getName().isBlank()
                || request.getName().length() > 255) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        var agent = dao.findById(id);
        if (agent == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        dao.lockConfiguration(agent);
        var name = request.getName().strip();
        if (versionDAO.existsByAgentAndName(id, name)) {
            var problem = new ProblemDetailResponseDTOV1();
            problem.setErrorCode("DUPLICATE_AGENT_CONFIGURATION_VERSION_NAME");
            problem.setDetail("Agent already has a configuration version named '" + name + "'");
            return Response.status(Response.Status.CONFLICT).entity(problem).build();
        }
        try (var resolved = resolveConfiguration(agent)) {
            if (resolved.getStatus() != Response.Status.OK.getStatusCode()) {
                return Response.fromResponse(resolved).build();
            }
            var version = saveVersion(agent, name, (AgentConfigurationDTOV1) resolved.getEntity());
            return Response.created(versionLocation(version))
                    .entity(versionMetadata(agent, version)).build();
        }
    }

    @Override
    public Response getAgentConfigurationVersion(String id, String versionId) {
        if (dao.findById(id) == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return configuration(versionDAO.findByAgentAndId(id, versionId));
    }

    @Override
    @Transactional(REQUIRED)
    public Response activateAgentConfigurationVersion(String id, String versionId) {
        var agent = dao.findById(id);
        if (agent == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        dao.lockConfiguration(agent);
        var version = versionDAO.findByAgentAndId(id, versionId);
        if (version == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        agent.setActiveConfigurationVersionId(version.getId());
        return Response.ok(versionMetadata(agent, version)).build();
    }

    private AgentConfigurationVersion saveVersion(Agent agent, String name, AgentConfigurationDTOV1 payload) {
        try {
            if (payload != null && payload.getContext() != null) {
                payload.getContext().setPrincipal(null);
            }
            var version = new AgentConfigurationVersion(agent, name, objectMapper.writeValueAsString(payload));
            versionDAO.create(version);
            if (agent.getActiveConfigurationVersionId() == null) {
                agent.setActiveConfigurationVersionId(version.getId());
            }
            return version;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize agent configuration", ex);
        }
    }

    private Response configuration(AgentConfigurationVersion version) {
        if (version == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        try {
            var payload = objectMapper.readValue(version.getConfiguration(), AgentConfigurationDTOV1.class);
            if (payload != null && payload.getContext() != null) {
                payload.getContext().setPrincipal(principal());
            }
            return Response.ok(payload).contentLocation(versionLocation(version)).build();
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot deserialize agent configuration version " + version.getId(), ex);
        }
    }

    private java.net.URI versionLocation(AgentConfigurationVersion version) {
        return UriBuilder.fromPath("/v1/agents/{id}/configuration/versions/{versionId}")
                .build(version.getAgentId(), version.getId());
    }

    private AgentConfigurationVersionDTOV1 versionMetadata(Agent agent, AgentConfigurationVersion version) {
        var result = new AgentConfigurationVersionDTOV1();
        result.setId(version.getId());
        result.setName(version.getName());
        result.setActive(version.getId().equals(agent.getActiveConfigurationVersionId()));
        return result;
    }

    private Response resolveConfiguration(Agent agent) {
        var rules = loadRules(agent);
        var duplicateToolRuleName = duplicateToolRuleName(rules);
        if (duplicateToolRuleName != null) {
            var problem = new ProblemDetailResponseDTOV1();
            problem.setErrorCode(ERROR_DUPLICATE_TOOL_RULE);
            problem.setDetail("Agent has duplicate MCP tool rules for tool '" + duplicateToolRuleName + "'");
            return Response.status(Response.Status.CONFLICT).entity(problem).build();
        }

        var payload = agentConfigurationMapper.build(agent, rules, agent.getTenantId(), principal());
        return Response.ok(payload).build();
    }

    private List<AgentMcpToolRule> loadRules(Agent agent) {
        var rules = new ArrayList<AgentMcpToolRule>();
        var toolIds = agent.getTools().stream()
                .filter(tool -> tool.getType() == ToolType.MCP)
                .map(Tool::getId)
                .toList();
        if (!toolIds.isEmpty()) {
            rules.addAll(agentMcpToolRuleDAO.findByAgentAndToolIds(agent.getId(), toolIds));
        }

        var globalToolIds = agent.getGlobalTools().stream()
                .filter(tool -> tool.getType() == ToolType.MCP)
                .map(GlobalTool::getId)
                .toList();
        if (!globalToolIds.isEmpty()) {
            rules.addAll(agentMcpToolRuleDAO.findByAgentAndGlobalToolIds(agent.getId(), globalToolIds));
        }
        return rules;
    }

    private String duplicateToolRuleName(List<AgentMcpToolRule> rules) {
        var seen = new HashSet<String>();
        for (var rule : rules) {
            var key = ruleServerKey(rule) + ' ' + rule.getToolName();
            if (!seen.add(key)) {
                return rule.getToolName();
            }
        }
        return null;
    }

    private String ruleServerKey(AgentMcpToolRule rule) {
        if (rule.getTool() != null) {
            return "tool:" + rule.getTool().getId();
        }
        return "globalTool:" + rule.getGlobalTool().getId();
    }

    private String principal() {
        return securityContext.getUserPrincipal().getName();
    }
}
