package org.tkit.onecx.ai.provider.rs.external.v1.mappers;

import java.util.ArrayList;
import java.util.List;

import org.mapstruct.Mapper;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.AgentGroup;
import org.tkit.onecx.ai.provider.domain.models.ExternalAgent;
import org.tkit.onecx.ai.provider.domain.models.GlobalScaffold;
import org.tkit.onecx.ai.provider.domain.models.GlobalSkill;
import org.tkit.onecx.ai.provider.domain.models.GlobalTool;
import org.tkit.onecx.ai.provider.domain.models.Model;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.Scaffold;
import org.tkit.onecx.ai.provider.domain.models.Skill;
import org.tkit.onecx.ai.provider.domain.models.Tool;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatMessageDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.AgentFilter;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.AgentGroupSnapshot;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.AgentSnapshot;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.ChatMessage;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.ChatRequest;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.Conversation;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.ExternalAgentSnapshot;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.ModelSnapshot;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.ProviderSnapshot;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.RequestContext;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.RuntimeChatRequest;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.ScaffoldSnapshot;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.SkillSnapshot;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.ToolSnapshot;

@Mapper(componentModel = "cdi", uses = OffsetDateTimeMapper.class)
public abstract class RuntimeSnapshotMapper {

    public RuntimeChatRequest toRuntimeRequest(Agent agent, ChatRequestDTOV1 chatRequestDTO,
            List<AgentGroupSnapshot> groups) {
        var request = new RuntimeChatRequest();
        request.setChatRequest(mapChatRequest(chatRequestDTO));
        request.setRootAgent(mapAgent(agent, groups));
        return request;
    }

    public AgentSnapshot mapAgent(Agent agent) {
        return mapAgent(agent, List.of());
    }

    public AgentSnapshot mapAgent(Agent agent, List<AgentGroupSnapshot> groups) {
        if (agent == null) {
            return null;
        }
        var snapshot = new AgentSnapshot();
        snapshot.setId(id(agent.getId()));
        snapshot.setName(agent.getName());
        snapshot.setDescription(agent.getDescription());
        snapshot.setAdditionalPrompt(agent.getAdditionalPrompt());
        snapshot.setA2aEnabled(agent.getA2aEnabled());
        snapshot.setStatus(agent.getStatus() != null ? agent.getStatus().name() : null);
        snapshot.setModel(mapModel(agent.getModel()));
        snapshot.setScaffold(agent.getGlobalScaffold() != null ? mapGlobalScaffold(agent.getGlobalScaffold())
                : mapScaffold(agent.getScaffold()));
        snapshot.setTools(mapTools(agent));
        snapshot.setGroups(groups);
        return snapshot;
    }

    public AgentGroupSnapshot mapGroup(AgentGroup group, List<AgentSnapshot> agents,
            List<ExternalAgentSnapshot> externalAgents) {
        if (group == null) {
            return null;
        }
        var snapshot = new AgentGroupSnapshot();
        snapshot.setId(id(group.getId()));
        snapshot.setName(group.getName());
        snapshot.setDescription(group.getDescription());
        snapshot.setRoutingInstructions(group.getRoutingInstructions());
        snapshot.setOrchestrationMode(group.getOrchestrationMode() != null ? group.getOrchestrationMode().name() : null);
        snapshot.setResponseStrategy(group.getResponseStrategy() != null ? group.getResponseStrategy().name() : null);
        snapshot.setAgents(agents != null ? agents : List.of());
        snapshot.setExternalAgents(externalAgents != null ? externalAgents : List.of());
        return snapshot;
    }

    public ExternalAgentSnapshot mapExternalAgent(ExternalAgent agent) {
        if (agent == null) {
            return null;
        }
        var snapshot = new ExternalAgentSnapshot();
        snapshot.setId(id(agent.getId()));
        snapshot.setName(agent.getName());
        snapshot.setDescription(agent.getDescription());
        snapshot.setDiscoveryUrl(agent.getDiscoveryUrl());
        snapshot.setApiKey(agent.getApiKey());
        snapshot.setAuthMode(agent.getAuthMode() != null ? agent.getAuthMode().name() : null);
        snapshot.setEnabled(agent.getEnabled());
        return snapshot;
    }

    public ChatMessageDTOV1 mapRuntimeChatMessage(String message) {
        var chatMessage = new ChatMessageDTOV1();
        chatMessage.setMessage(message != null ? message : "");
        chatMessage.setType(ChatMessageDTOV1.TypeEnum.ASSISTANT);
        return chatMessage;
    }

    private ChatRequest mapChatRequest(ChatRequestDTOV1 request) {
        if (request == null) {
            return null;
        }
        var runtimeChatRequest = new ChatRequest();
        runtimeChatRequest.setChatMessage(mapChatMessage(request.getChatMessage()));
        runtimeChatRequest.setRequestContext(mapRequestContext(request));
        runtimeChatRequest.setConversation(mapConversation(request));
        return runtimeChatRequest;
    }

    private RequestContext mapRequestContext(ChatRequestDTOV1 request) {
        if (request.getRequestContext() == null) {
            return null;
        }
        var requestContext = new RequestContext();
        if (request.getRequestContext().getFilter() != null) {
            var filter = new AgentFilter();
            filter.setKey(request.getRequestContext().getFilter().getKey() != null
                    ? request.getRequestContext().getFilter().getKey().value()
                    : null);
            filter.setValue(request.getRequestContext().getFilter().getValue());
            requestContext.setFilter(filter);
        }
        requestContext.setAiContext(request.getRequestContext().getAiContext());
        return requestContext;
    }

    private Conversation mapConversation(ChatRequestDTOV1 request) {
        if (request.getConversation() == null) {
            return null;
        }
        var conversation = new Conversation();
        conversation.setConversationId(request.getConversation().getConversationId());
        conversation.setConversationType(request.getConversation().getConversationType() != null
                ? request.getConversation().getConversationType().value()
                : null);
        conversation.setHistory(request.getConversation().getHistory() != null
                ? request.getConversation().getHistory().stream().map(this::mapChatMessage).toList()
                : List.of());
        return conversation;
    }

    private ChatMessage mapChatMessage(ChatMessageDTOV1 message) {
        if (message == null) {
            return null;
        }
        var runtimeMessage = new ChatMessage();
        runtimeMessage.setConversationId(message.getConversationId());
        runtimeMessage.setMessage(message.getMessage());
        runtimeMessage.setType(message.getType() != null ? message.getType().value() : null);
        runtimeMessage.setCreationDate(message.getCreationDate());
        return runtimeMessage;
    }

    private ModelSnapshot mapModel(Model model) {
        if (model == null) {
            return null;
        }
        var snapshot = new ModelSnapshot();
        snapshot.setId(id(model.getId()));
        snapshot.setName(model.getName());
        snapshot.setModelIdentifier(model.getModelIdentifier());
        snapshot.setModelConfig(model.getModelConfig());
        snapshot.setCommunicationMode(model.getCommunicationMode() != null ? model.getCommunicationMode().name() : null);
        snapshot.setProvider(mapProvider(model.getProvider()));
        return snapshot;
    }

    private ProviderSnapshot mapProvider(Provider provider) {
        if (provider == null) {
            return null;
        }
        var snapshot = new ProviderSnapshot();
        snapshot.setId(id(provider.getId()));
        snapshot.setName(provider.getName());
        snapshot.setType(provider.getType() != null ? provider.getType().name() : null);
        snapshot.setDescription(provider.getDescription());
        snapshot.setLlmUrl(provider.getLlmUrl());
        snapshot.setApiKey(provider.getApiKey());
        snapshot.setAuthMode(provider.getAuthMode() != null ? provider.getAuthMode().name() : null);
        return snapshot;
    }

    private ScaffoldSnapshot mapScaffold(Scaffold scaffold) {
        if (scaffold == null) {
            return null;
        }
        var skills = new ArrayList<SkillSnapshot>();
        if (scaffold.getSkills() != null) {
            skills.addAll(scaffold.getSkills().stream().map(this::mapSkill).toList());
        }
        if (scaffold.getGlobalSkills() != null) {
            skills.addAll(scaffold.getGlobalSkills().stream().map(this::mapGlobalSkill).toList());
        }
        var snapshot = new ScaffoldSnapshot();
        snapshot.setId(id(scaffold.getId()));
        snapshot.setSource(ScaffoldSnapshot.SourceEnum.TENANT);
        snapshot.setName(scaffold.getName());
        snapshot.setSystemPrompt(scaffold.getSystemPrompt());
        snapshot.setSourceProduct(scaffold.getSourceProduct());
        snapshot.setSkills(skills);
        return snapshot;
    }

    private ScaffoldSnapshot mapGlobalScaffold(GlobalScaffold scaffold) {
        if (scaffold == null) {
            return null;
        }
        var snapshot = new ScaffoldSnapshot();
        snapshot.setId(id(scaffold.getId()));
        snapshot.setSource(ScaffoldSnapshot.SourceEnum.GLOBAL);
        snapshot.setName(scaffold.getName());
        snapshot.setSystemPrompt(scaffold.getSystemPrompt());
        snapshot.setSourceProduct(scaffold.getSourceProduct());
        snapshot.setSkills(scaffold.getSkills() != null
                ? scaffold.getSkills().stream().map(this::mapGlobalSkill).toList()
                : List.of());
        return snapshot;
    }

    private SkillSnapshot mapSkill(Skill skill) {
        var snapshot = new SkillSnapshot();
        snapshot.setId(id(skill.getId()));
        snapshot.setSource(SkillSnapshot.SourceEnum.TENANT);
        snapshot.setName(skill.getName());
        snapshot.setDescription(skill.getDescription());
        snapshot.setInstruction(skill.getInstruction());
        return snapshot;
    }

    private SkillSnapshot mapGlobalSkill(GlobalSkill skill) {
        var snapshot = new SkillSnapshot();
        snapshot.setId(id(skill.getId()));
        snapshot.setSource(SkillSnapshot.SourceEnum.GLOBAL);
        snapshot.setName(skill.getName());
        snapshot.setDescription(skill.getDescription());
        snapshot.setInstruction(skill.getInstruction());
        return snapshot;
    }

    private List<ToolSnapshot> mapTools(Agent agent) {
        var tools = new ArrayList<ToolSnapshot>();
        if (agent.getTools() != null) {
            tools.addAll(agent.getTools().stream().map(this::mapTool).toList());
        }
        if (agent.getGlobalTools() != null) {
            tools.addAll(agent.getGlobalTools().stream().map(this::mapGlobalTool).toList());
        }
        return tools;
    }

    private ToolSnapshot mapTool(Tool tool) {
        var snapshot = new ToolSnapshot();
        snapshot.setId(id(tool.getId()));
        snapshot.setSource(ToolSnapshot.SourceEnum.TENANT);
        snapshot.setName(tool.getName());
        snapshot.setDescription(tool.getDescription());
        snapshot.setType(tool.getType() != null ? tool.getType().name() : null);
        snapshot.setUrl(tool.getUrl());
        snapshot.setApiKey(tool.getApiKey());
        snapshot.setAuthMode(tool.getAuthMode() != null ? tool.getAuthMode().name() : null);
        return snapshot;
    }

    private ToolSnapshot mapGlobalTool(GlobalTool tool) {
        var snapshot = new ToolSnapshot();
        snapshot.setId(id(tool.getId()));
        snapshot.setSource(ToolSnapshot.SourceEnum.GLOBAL);
        snapshot.setName(tool.getName());
        snapshot.setDescription(tool.getDescription());
        snapshot.setType(tool.getType() != null ? tool.getType().name() : null);
        snapshot.setUrl(tool.getUrl());
        snapshot.setApiKey(tool.getApiKey());
        snapshot.setAuthMode(tool.getAuthMode() != null ? tool.getAuthMode().name() : null);
        return snapshot;
    }

    private String id(Object id) {
        return id != null ? id.toString() : null;
    }
}
