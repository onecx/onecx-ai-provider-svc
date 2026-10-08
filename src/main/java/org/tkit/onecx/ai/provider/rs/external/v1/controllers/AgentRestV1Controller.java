package org.tkit.onecx.ai.provider.rs.external.v1.controllers;

import static jakarta.transaction.Transactional.TxType.NOT_SUPPORTED;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import org.tkit.onecx.ai.provider.domain.daos.AgentDAO;
import org.tkit.onecx.ai.provider.domain.daos.AgentMcpToolRuleDAO;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.AgentMcpToolRule;
import org.tkit.onecx.ai.provider.domain.models.GlobalTool;
import org.tkit.onecx.ai.provider.domain.models.Tool;
import org.tkit.onecx.ai.provider.domain.models.enums.ToolType;
import org.tkit.onecx.ai.provider.rs.external.v1.mappers.AgentConfigurationMapper;
import org.tkit.onecx.ai.provider.rs.external.v1.mappers.AgentMapper;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.AgentV1Api;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.AgentSearchCriteriaDTOV1;
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

    @Context
    SecurityContext securityContext;

    @Override
    public Response findAgentBySearchCriteria(AgentSearchCriteriaDTOV1 agentSearchCriteriaDTO) {
        var criteria = mapper.mapCriteria(agentSearchCriteriaDTO);
        var result = dao.findAgentsByCriteria(criteria);
        return Response.ok(mapper.mapPage(result)).build();
    }

    @Override
    public Response getAgentConfiguration(String id) {
        Agent agent = dao.findById(id);
        if (agent == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        var rules = loadRules(agent);
        if (hasDuplicateToolRule(rules)) {
            var problem = new ProblemDetailResponseDTOV1();
            problem.setErrorCode(ERROR_DUPLICATE_TOOL_RULE);
            problem.setDetail("Agent has duplicate MCP tool rules for tool '" + duplicateToolRuleName(rules) + "'");
            return Response.status(Response.Status.CONFLICT).entity(problem).build();
        }

        var payload = agentConfigurationMapper.build(agent, rules, agent.getTenantId(), principal());
        return Response.ok(payload).build();
    }

    private List<AgentMcpToolRule> loadRules(Agent agent) {
        var rules = new ArrayList<AgentMcpToolRule>();
        if (agent.getTools() != null) {
            var toolIds = agent.getTools().stream()
                    .filter(tool -> tool.getType() == ToolType.MCP)
                    .map(Tool::getId)
                    .toList();
            if (!toolIds.isEmpty()) {
                rules.addAll(agentMcpToolRuleDAO.findByAgentAndToolIds(agent.getId(), toolIds));
            }
        }
        if (agent.getGlobalTools() != null) {
            var globalToolIds = agent.getGlobalTools().stream()
                    .filter(tool -> tool.getType() == ToolType.MCP)
                    .map(GlobalTool::getId)
                    .toList();
            if (!globalToolIds.isEmpty()) {
                rules.addAll(agentMcpToolRuleDAO.findByAgentAndGlobalToolIds(agent.getId(), globalToolIds));
            }
        }
        return rules;
    }

    private boolean hasDuplicateToolRule(List<AgentMcpToolRule> rules) {
        var seen = new HashSet<String>();
        for (var rule : rules) {
            if (!seen.add(rule.getToolName())) {
                return true;
            }
        }
        return false;
    }

    private String duplicateToolRuleName(List<AgentMcpToolRule> rules) {
        var seen = new HashSet<String>();
        for (var rule : rules) {
            if (!seen.add(rule.getToolName())) {
                return rule.getToolName();
            }
        }
        return "";
    }

    private String principal() {
        var user = securityContext == null ? null : securityContext.getUserPrincipal();
        return user != null ? user.getName() : null;
    }
}
