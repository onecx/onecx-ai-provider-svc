//package org.tkit.onecx.ai.provider.common.services.runtime;
//
//import java.util.List;
//
//import jakarta.enterprise.context.ApplicationScoped;
//import jakarta.inject.Inject;
//
//import org.tkit.onecx.ai.provider.common.services.runtime.dto.RuntimeDtos.AgentFilter;
//import org.tkit.onecx.ai.provider.common.services.runtime.dto.RuntimeDtos.AgentGroupSnapshot;
//import org.tkit.onecx.ai.provider.common.services.runtime.dto.RuntimeDtos.AgentSnapshot;
//import org.tkit.onecx.ai.provider.common.services.runtime.dto.RuntimeDtos.ChatMessage;
//import org.tkit.onecx.ai.provider.common.services.runtime.dto.RuntimeDtos.ChatRequest;
//import org.tkit.onecx.ai.provider.common.services.runtime.dto.RuntimeDtos.Conversation;
//import org.tkit.onecx.ai.provider.common.services.runtime.dto.RuntimeDtos.ExternalAgentSnapshot;
//import org.tkit.onecx.ai.provider.common.services.runtime.dto.RuntimeDtos.ModelSnapshot;
//import org.tkit.onecx.ai.provider.common.services.runtime.dto.RuntimeDtos.ProviderSnapshot;
//import org.tkit.onecx.ai.provider.common.services.runtime.dto.RuntimeDtos.RequestContext;
//import org.tkit.onecx.ai.provider.common.services.runtime.dto.RuntimeDtos.RuntimeChatRequest;
//import org.tkit.onecx.ai.provider.common.services.runtime.dto.RuntimeDtos.ScaffoldSnapshot;
//import org.tkit.onecx.ai.provider.common.services.runtime.dto.RuntimeDtos.SkillSnapshot;
//import org.tkit.onecx.ai.provider.common.services.runtime.dto.RuntimeDtos.ToolSnapshot;
//import org.tkit.onecx.ai.provider.domain.daos.AgentDAO;
//import org.tkit.onecx.ai.provider.domain.daos.ExternalAgentDAO;
//import org.tkit.onecx.ai.provider.domain.models.Agent;
//import org.tkit.onecx.ai.provider.domain.models.AgentGroup;
//import org.tkit.onecx.ai.provider.domain.models.ExternalAgent;
//import org.tkit.onecx.ai.provider.domain.models.Model;
//import org.tkit.onecx.ai.provider.domain.models.Provider;
//import org.tkit.onecx.ai.provider.domain.models.Scaffold;
//import org.tkit.onecx.ai.provider.domain.models.Skill;
//import org.tkit.onecx.ai.provider.domain.models.Tool;
//
//import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatMessageDTOV1;
//import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
//
//@ApplicationScoped
//public class RuntimeSnapshotMapper {
//
//    private static final String SOURCE_TENANT = "TENANT";
//
//    @Inject
//    AgentDAO agentDAO;
//
//    @Inject
//    ExternalAgentDAO externalAgentDAO;
//

//

//
//
//    private AgentGroupSnapshot mapGroup(AgentGroup group) {
//        if (group == null) {
//            return null;
//        }
//        String groupId = id(group.getId());
//        List<AgentSnapshot> agents = agentDAO.findAgentsByGroupId(groupId).stream()
//                .filter(agent -> agent.getId() != null)
//                .map(agent -> mapAgent(agent, false))
//                .toList();
//        List<ExternalAgentSnapshot> externalAgents = externalAgentDAO.findExternalAgentsByGroupId(groupId).stream()
//                .map(this::mapExternalAgent)
//                .toList();
//        return new AgentGroupSnapshot(groupId, group.getName(), group.getDescription(), group.getRoutingInstructions(),
//                group.getOrchestrationMode() != null ? group.getOrchestrationMode().name() : null,
//                group.getResponseStrategy() != null ? group.getResponseStrategy().name() : null, agents, externalAgents);
//    }
//
//    private ExternalAgentSnapshot mapExternalAgent(ExternalAgent agent) {
//        return new ExternalAgentSnapshot(id(agent.getId()), agent.getName(), agent.getDescription(), agent.getDiscoveryUrl(),
//                agent.getApiKey(), agent.getAuthMode() != null ? agent.getAuthMode().name() : null, agent.getEnabled());
//    }
//
//    private ModelSnapshot mapModel(Model model) {
//        if (model == null) {
//            return null;
//        }
//        return new ModelSnapshot(id(model.getId()), model.getName(), model.getModelIdentifier(), model.getModelConfig(),
//                model.getCommunicationMode() != null ? model.getCommunicationMode().name() : null,
//                mapProvider(model.getProvider()));
//    }
//
//    private ProviderSnapshot mapProvider(Provider provider) {
//        if (provider == null) {
//            return null;
//        }
//        return new ProviderSnapshot(id(provider.getId()), provider.getName(),
//                provider.getType() != null ? provider.getType().name() : null, provider.getDescription(),
//                provider.getLlmUrl(), provider.getApiKey(),
//                provider.getAuthMode() != null ? provider.getAuthMode().name() : null);
//    }
//
//    private ScaffoldSnapshot mapScaffold(Scaffold scaffold) {
//        if (scaffold == null) {
//            return null;
//        }
//        List<SkillSnapshot> skills = scaffold.getSkills() != null
//                ? scaffold.getSkills().stream().map(this::mapSkill).toList()
//                : List.of();
//        if (scaffold.getGlobalSkills() != null && !scaffold.getGlobalSkills().isEmpty()) {
//            var allSkills = new java.util.ArrayList<>(skills);
//            allSkills.addAll(scaffold.getGlobalSkills().stream().map(this::mapGlobalSkill).toList());
//            skills = allSkills;
//        }
//        return new ScaffoldSnapshot(id(scaffold.getId()), SOURCE_TENANT, scaffold.getName(), scaffold.getSystemPrompt(),
//                scaffold.getSourceProduct(), skills);
//    }
//
//    private ScaffoldSnapshot mapGlobalScaffold(org.tkit.onecx.ai.provider.domain.models.GlobalScaffold scaffold) {
//        if (scaffold == null) {
//            return null;
//        }
//        List<SkillSnapshot> skills = scaffold.getSkills() != null
//                ? scaffold.getSkills().stream().map(this::mapGlobalSkill).toList()
//                : List.of();
//        return new ScaffoldSnapshot(id(scaffold.getId()), "GLOBAL", scaffold.getName(), scaffold.getSystemPrompt(),
//                scaffold.getSourceProduct(), skills);
//    }
//
//    private SkillSnapshot mapSkill(Skill skill) {
//        return new SkillSnapshot(id(skill.getId()), SOURCE_TENANT, skill.getName(), skill.getDescription(),
//                skill.getInstruction());
//    }
//
//    private SkillSnapshot mapGlobalSkill(org.tkit.onecx.ai.provider.domain.models.GlobalSkill skill) {
//        return new SkillSnapshot(id(skill.getId()), "GLOBAL", skill.getName(), skill.getDescription(),
//                skill.getInstruction());
//    }
//
//    private ToolSnapshot mapTool(Tool tool) {
//        return new ToolSnapshot(id(tool.getId()), SOURCE_TENANT, tool.getName(), tool.getDescription(),
//                tool.getType() != null ? tool.getType().name() : null, tool.getUrl(), tool.getApiKey(),
//                tool.getAuthMode() != null ? tool.getAuthMode().name() : null);
//    }
//
//    private ToolSnapshot mapGlobalTool(org.tkit.onecx.ai.provider.domain.models.GlobalTool tool) {
//        return new ToolSnapshot(id(tool.getId()), "GLOBAL", tool.getName(), tool.getDescription(),
//                tool.getType() != null ? tool.getType().name() : null, tool.getUrl(), tool.getApiKey(),
//                tool.getAuthMode() != null ? tool.getAuthMode().name() : null);
//    }
//
//    private List<ToolSnapshot> tools(Agent agent) {
//        var tools = new java.util.ArrayList<ToolSnapshot>();
//        if (agent.getTools() != null) {
//            tools.addAll(agent.getTools().stream().map(this::mapTool).toList());
//        }
//        if (agent.getGlobalTools() != null) {
//            tools.addAll(agent.getGlobalTools().stream().map(this::mapGlobalTool).toList());
//        }
//        return tools;
//    }
//
//    private String id(Object id) {
//        return id != null ? id.toString() : null;
//    }
//}
