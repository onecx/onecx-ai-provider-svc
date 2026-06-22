package org.tkit.onecx.ai.provider.common.services.agent;

import java.util.HashSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.tkit.onecx.ai.provider.domain.daos.AgentDAO;
import org.tkit.onecx.ai.provider.domain.daos.AgentGroupDAO;
import org.tkit.onecx.ai.provider.domain.daos.ModelDAO;
import org.tkit.onecx.ai.provider.domain.daos.RuntimeConfigDAO;
import org.tkit.onecx.ai.provider.domain.daos.ScaffoldDAO;
import org.tkit.onecx.ai.provider.domain.daos.ToolDAO;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.AgentGroup;
import org.tkit.onecx.ai.provider.domain.models.Model;
import org.tkit.onecx.ai.provider.domain.models.RuntimeConfig;
import org.tkit.onecx.ai.provider.domain.models.Scaffold;
import org.tkit.onecx.ai.provider.domain.models.Tool;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.RequestContextDTOV1;

@ApplicationScoped
public class AgentService {

    @Inject
    AgentDAO agentDAO;

    @Inject
    ToolDAO toolDAO;

    @Inject
    ModelDAO modelDAO;

    @Inject
    ScaffoldDAO scaffoldDAO;

    @Inject
    RuntimeConfigDAO runtimeConfigDAO;

    @Inject
    AgentGroupDAO agentGroupDAO;

    @Transactional
    public Agent createAgent(Agent agent) {
        // Resolve tools
        if (agent.getTools() != null && !agent.getTools().isEmpty()) {
            var toolsToAdd = new HashSet<Tool>();
            agent.getTools().forEach(tool -> {
                var existing = toolDAO.findById(tool.getId());
                if (existing == null) {
                    existing = toolDAO.create(tool);
                }
                toolsToAdd.add(existing);
            });
            agent.setTools(toolsToAdd);
        }

        // Resolve model
        if (agent.getModel() != null) {
            var modelId = agent.getModel().getId();
            Model resolved = modelId != null ? modelDAO.findById(modelId) : null;
            if (resolved == null) {
                resolved = modelDAO.create(agent.getModel());
            }
            agent.setModel(resolved);
        }

        // Resolve scaffold
        if (agent.getScaffold() != null) {
            var scaffoldId = agent.getScaffold().getId();
            Scaffold resolved = scaffoldId != null ? scaffoldDAO.findById(scaffoldId) : null;
            if (resolved == null) {
                resolved = scaffoldDAO.create(agent.getScaffold());
            }
            agent.setScaffold(resolved);
        }

        // Resolve runtimeConfig
        if (agent.getRuntimeConfig() != null) {
            var rtId = agent.getRuntimeConfig().getId();
            RuntimeConfig resolved = rtId != null ? runtimeConfigDAO.findById(rtId) : null;
            if (resolved == null) {
                resolved = runtimeConfigDAO.create(agent.getRuntimeConfig());
            }
            agent.setRuntimeConfig(resolved);
        }

        // Resolve groups
        if (agent.getGroups() != null && !agent.getGroups().isEmpty()) {
            var groupsToAdd = new HashSet<AgentGroup>();
            agent.getGroups().forEach(group -> {
                var existing = agentGroupDAO.findById(group.getId());
                if (existing == null) {
                    existing = agentGroupDAO.create(group);
                }
                groupsToAdd.add(existing);
            });
            agent.setGroups(groupsToAdd);
        }

        return agentDAO.create(agent);
    }

    public Agent updateAgent(Agent agent) {
        if (agent == null || agent.getId() == null) {
            return null;
        }

        if (agent.getTools() != null && !agent.getTools().isEmpty()) {
            var toolsToAdd = new HashSet<Tool>();
            agent.getTools().forEach(tool -> {
                var existing = toolDAO.findById(tool.getId());
                if (existing == null) {
                    existing = toolDAO.create(tool);
                }
                toolsToAdd.add(existing);
            });
            agent.setTools(toolsToAdd);
        }

        return agentDAO.update(agent);
    }

    public Agent findAgentByRequestContext(RequestContextDTOV1 requestContext) {

        String filterKey = (requestContext != null && requestContext.getFilter() != null
                && requestContext.getFilter().getKey() != null)
                        ? requestContext.getFilter().getKey().value()
                        : null;
        String filterValue = (requestContext != null && requestContext.getFilter() != null
                && requestContext.getFilter().getValue() != null)
                        ? requestContext.getFilter().getValue()
                        : null;

        var agents = agentDAO.findAllAgentsByFilterKey(filterKey);

        if (filterValue == null) {
            return agents.stream()
                    .filter(a -> a.getFilter() == null || a.getFilter().getValue() == null)
                    .findFirst()
                    .orElse(null);
        }

        return agents.stream()
                .filter(a -> a.getFilter() != null
                        && a.getFilter().getValue() != null
                        && filterValue.matches(a.getFilter().getValue().replace("*", ".*")))
                .max((a1, a2) -> {
                    int len1 = a1.getFilter().getValue().replace("*", "").length();
                    int len2 = a2.getFilter().getValue().replace("*", "").length();
                    return Integer.compare(len1, len2);
                })
                .orElse(null);
    }
}
