package org.tkit.onecx.ai.provider.rs.external.v1.mappers;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tkit.onecx.ai.provider.domain.models.AbstractScaffold;
import org.tkit.onecx.ai.provider.domain.models.AbstractTool;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.AgentMcpToolRule;
import org.tkit.onecx.ai.provider.domain.models.GlobalScaffold;
import org.tkit.onecx.ai.provider.domain.models.GlobalSkill;
import org.tkit.onecx.ai.provider.domain.models.Model;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.Scaffold;
import org.tkit.onecx.ai.provider.domain.models.Skill;
import org.tkit.onecx.ai.provider.domain.models.enums.ExecutionPolicy;
import org.tkit.onecx.ai.provider.domain.models.enums.ToolPermission;
import org.tkit.onecx.ai.provider.domain.models.enums.ToolType;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.AgentConfigurationAgentDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.AgentConfigurationDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ContextDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.McpServerConfigurationDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ModelConfigurationDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ProviderConfigurationDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ScaffoldConfigurationDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.SkillConfigurationDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ToolRuleConfigurationDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.VoiceSettingsDTOV1;

@Mapper(uses = OffsetDateTimeMapper.class)
public abstract class AgentConfigurationMapper {

    private static final String SCHEMA_VERSION = "1";

    private static final String CREDENTIAL_PREFIX_PROVIDER = "credential://provider/";

    private static final String CREDENTIAL_PREFIX_TOOL = "credential://tool/";

    private static final String CREDENTIAL_SUFFIX = "/api-key";

    @Mapping(target = "name", source = "agent.name")
    @Mapping(target = "description", source = "agent.description")
    abstract AgentConfigurationAgentDTOV1 agentObject(Agent agent);

    @Mapping(target = "name", source = "scaffold.name")
    @Mapping(target = "systemPrompt", source = "scaffold.systemPrompt")
    abstract ScaffoldConfigurationDTOV1 scaffoldObject(AbstractScaffold scaffold);

    @Mapping(target = "position", ignore = true)
    @Mapping(target = "source", constant = "TENANT")
    abstract SkillConfigurationDTOV1 tenantSkill(Skill skill);

    @Mapping(target = "position", ignore = true)
    @Mapping(target = "source", constant = "GLOBAL")
    abstract SkillConfigurationDTOV1 globalSkill(GlobalSkill skill);

    public AgentConfigurationDTOV1 build(Agent agent, List<AgentMcpToolRule> rules, String tenantId, String principal) {
        if (agent == null) {
            return null;
        }
        var config = new AgentConfigurationDTOV1();
        config.setSchemaVersion(SCHEMA_VERSION);
        config.setContext(buildContext(tenantId, principal));
        config.setAgent(agentObject(agent));
        config.setVoice(buildVoice(agent));
        var model = agent.getModel();
        config.setModel(modelConfiguration(model));
        config.setProvider(providerConfiguration(model != null ? model.getProvider() : null));
        var scaffold = agent.getGlobalScaffold() != null ? agent.getGlobalScaffold() : agent.getScaffold();
        config.setScaffold(scaffold != null ? scaffoldObject(scaffold) : null);
        config.setSkills(buildSkills(scaffold));
        config.setMcpServers(buildMcpServers(agent, rules));
        return config;
    }

    private ContextDTOV1 buildContext(String tenantId, String principal) {
        var context = new ContextDTOV1();
        context.setTenantId(tenantId);
        context.setPrincipal(principal);
        return context;
    }

    private VoiceSettingsDTOV1 buildVoice(Agent agent) {
        var voice = new VoiceSettingsDTOV1();
        voice.setVoiceEnabled(Boolean.TRUE.equals(agent.getVoiceEnabled()));
        voice.setLanguageCode(agent.getLanguageCode());
        return voice;
    }

    private ModelConfigurationDTOV1 modelConfiguration(Model model) {
        if (model == null) {
            return null;
        }
        var dto = new ModelConfigurationDTOV1();
        dto.setId(model.getId());
        dto.setName(model.getName());
        dto.setModelIdentifier(model.getModelIdentifier());
        dto.setModelConfig(model.getModelConfig());
        dto.setCommunicationMode(model.getCommunicationMode() != null ? model.getCommunicationMode().name() : null);
        return dto;
    }

    private ProviderConfigurationDTOV1 providerConfiguration(Provider provider) {
        if (provider == null) {
            return null;
        }
        var dto = new ProviderConfigurationDTOV1();
        dto.setId(provider.getId());
        dto.setName(provider.getName());
        dto.setType(provider.getType() != null ? provider.getType().name() : null);
        dto.setDescription(provider.getDescription());
        dto.setLlmUrl(provider.getLlmUrl());
        dto.setAuthMode(provider.getAuthMode() != null ? provider.getAuthMode().name() : null);
        dto.setCredentialRef(CREDENTIAL_PREFIX_PROVIDER + provider.getId() + CREDENTIAL_SUFFIX);
        return dto;
    }

    private List<SkillConfigurationDTOV1> buildSkills(AbstractScaffold scaffold) {
        if (scaffold == null) {
            return List.of();
        }
        var skills = new ArrayList<SkillConfigurationDTOV1>();
        var position = 0;
        if (scaffold instanceof Scaffold tenantScaffold) {
            for (var skill : orderedSkills(tenantScaffold.getSkills())) {
                skills.add(tenantSkill(skill));
                skills.get(skills.size() - 1).setPosition(position++);
            }
            for (var skill : orderedGlobalSkills(tenantScaffold.getGlobalSkills())) {
                skills.add(globalSkill(skill));
                skills.get(skills.size() - 1).setPosition(position++);
            }
        } else if (scaffold instanceof GlobalScaffold globalScaffold) {
            for (var skill : orderedGlobalSkills(globalScaffold.getSkills())) {
                skills.add(globalSkill(skill));
                skills.get(skills.size() - 1).setPosition(position++);
            }
        }
        return skills;
    }

    private List<AbstractTool> mcpTools(Set<? extends AbstractTool> tools) {
        if (tools == null) {
            return List.of();
        }
        var result = new ArrayList<AbstractTool>();
        for (var tool : tools) {
            if (tool.getType() == ToolType.MCP) {
                result.add(tool);
            }
        }
        return result;
    }

    private List<Skill> orderedSkills(Set<Skill> skills) {
        if (skills == null) {
            return List.of();
        }
        return skills.stream().sorted(comparing(Skill::getId, nullsLast(naturalOrder()))).toList();
    }

    private List<GlobalSkill> orderedGlobalSkills(Set<GlobalSkill> skills) {
        if (skills == null) {
            return List.of();
        }
        return skills.stream().sorted(comparing(GlobalSkill::getId, nullsLast(naturalOrder()))).toList();
    }

    private List<McpServerConfigurationDTOV1> buildMcpServers(Agent agent, List<AgentMcpToolRule> rules) {
        var servers = new ArrayList<McpServerConfigurationDTOV1>();
        var rulesByTargetId = groupRulesByTargetId(rules);
        for (var tool : mcpTools(agent.getTools())) {
            servers.add(buildMcpServer(tool, rulesByTargetId.getOrDefault(tool.getId(), List.of())));
        }
        for (var tool : mcpTools(agent.getGlobalTools())) {
            servers.add(buildMcpServer(tool, rulesByTargetId.getOrDefault(tool.getId(), List.of())));
        }
        return servers;
    }

    private Map<String, List<AgentMcpToolRule>> groupRulesByTargetId(List<AgentMcpToolRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return Map.of();
        }
        var byTarget = new LinkedHashMap<String, List<AgentMcpToolRule>>();
        for (var rule : rules) {
            if (rule.getTool() != null || rule.getGlobalTool() != null) {
                byTarget.computeIfAbsent(ruleTargetId(rule), key -> new ArrayList<>()).add(rule);
            }
        }
        return byTarget;
    }

    private McpServerConfigurationDTOV1 buildMcpServer(AbstractTool tool, List<AgentMcpToolRule> rules) {
        var server = new McpServerConfigurationDTOV1();
        server.setId(tool.getId());
        server.setName(tool.getName());
        server.setDescription(tool.getDescription());
        server.setUrl(tool.getUrl());
        server.setCredentialRef(CREDENTIAL_PREFIX_TOOL + tool.getId() + CREDENTIAL_SUFFIX);
        server.setUnlistedToolPermission(mapUnlistedPermission(tool.getExecutionPolicy()));
        server.setUnlistedToolPermissionSource(mapUnlistedSource(tool.getExecutionPolicy()));
        server.setToolRules(rules.stream().map(this::mapRule).toList());
        return server;
    }

    private ToolRuleConfigurationDTOV1 mapRule(AgentMcpToolRule rule) {
        var dto = new ToolRuleConfigurationDTOV1();
        dto.setToolName(rule.getToolName());
        dto.setDescription(rule.getToolDescription());
        dto.setPermission(mapPermission(rule.getAllowed()));
        dto.setPolicySource(ToolRuleConfigurationDTOV1.PolicySourceEnum.AGENT_TOOL_RULE);
        return dto;
    }

    private McpServerConfigurationDTOV1.UnlistedToolPermissionEnum mapUnlistedPermission(ExecutionPolicy policy) {
        var canonical = (policy == null ? ExecutionPolicy.DEFAULT : policy).toCanonical();
        if (canonical == ExecutionPolicy.ALWAYS_ALLOW) {
            return McpServerConfigurationDTOV1.UnlistedToolPermissionEnum.ALWAYS_ALLOW;
        }
        return McpServerConfigurationDTOV1.UnlistedToolPermissionEnum.ALWAYS_ASK;
    }

    private McpServerConfigurationDTOV1.UnlistedToolPermissionSourceEnum mapUnlistedSource(ExecutionPolicy policy) {
        return policy != null
                ? McpServerConfigurationDTOV1.UnlistedToolPermissionSourceEnum.TOOL_EXECUTION_POLICY
                : McpServerConfigurationDTOV1.UnlistedToolPermissionSourceEnum.SYSTEM_DEFAULT;
    }

    private ToolRuleConfigurationDTOV1.PermissionEnum mapPermission(ToolPermission permission) {
        var canonical = (permission == null ? ToolPermission.DEFAULT : permission).toCanonical();
        if (canonical == ToolPermission.ALWAYS_ALLOW) {
            return ToolRuleConfigurationDTOV1.PermissionEnum.ALWAYS_ALLOW;
        }
        if (canonical == ToolPermission.DENY) {
            return ToolRuleConfigurationDTOV1.PermissionEnum.DENY;
        }
        return ToolRuleConfigurationDTOV1.PermissionEnum.ALWAYS_ASK;
    }

    private String ruleTargetId(AgentMcpToolRule rule) {
        return rule.getTool() != null ? rule.getTool().getId() : rule.getGlobalTool().getId();
    }
}
