package org.tkit.onecx.ai.provider.rs.external.v1.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.AgentMcpToolRule;
import org.tkit.onecx.ai.provider.domain.models.GlobalScaffold;
import org.tkit.onecx.ai.provider.domain.models.GlobalSkill;
import org.tkit.onecx.ai.provider.domain.models.GlobalTool;
import org.tkit.onecx.ai.provider.domain.models.Model;
import org.tkit.onecx.ai.provider.domain.models.Provider;
import org.tkit.onecx.ai.provider.domain.models.Scaffold;
import org.tkit.onecx.ai.provider.domain.models.Skill;
import org.tkit.onecx.ai.provider.domain.models.Tool;
import org.tkit.onecx.ai.provider.domain.models.enums.AuthMode;
import org.tkit.onecx.ai.provider.domain.models.enums.CommunicationMode;
import org.tkit.onecx.ai.provider.domain.models.enums.ExecutionPolicy;
import org.tkit.onecx.ai.provider.domain.models.enums.ProviderType;
import org.tkit.onecx.ai.provider.domain.models.enums.ToolPermission;
import org.tkit.onecx.ai.provider.domain.models.enums.ToolType;

import com.fasterxml.jackson.databind.ObjectMapper;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.AgentConfigurationDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.McpServerConfigurationDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.SkillConfigurationDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ToolRuleConfigurationDTOV1;

/**
 * Plain unit test (no {@code @QuarkusTest}/{@code AbstractTest}) so it exercises the mapper without
 * a database or Docker.
 */
class AgentConfigurationMapperTest {

    private static final String TENANT_ID = "default";

    private static final String PRINCIPAL = "test-user";

    private static final String PROVIDER_API_KEY = "PROVIDER_SECRET_999";

    private static final String MCP_TOOL_API_KEY = "MCP_TOOL_SECRET_777";

    private final AgentConfigurationMapper mapper = new AgentConfigurationMapperImpl();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Provider provider;

    private Model model;

    private Scaffold scaffold;

    private GlobalScaffold globalScaffold;

    @BeforeEach
    void setUp() {
        provider = provider();
        model = modelWith(provider);
        scaffold = tenantScaffold();
        globalScaffold = globalScaffold();
    }

    @Test
    void build_nullAgent_returnsNull() {
        assertThat(mapper.build(null, List.of(), TENANT_ID, PRINCIPAL)).isNull();
    }

    @Test
    void build_mapsTopLevelFields() {
        var agent = baseAgent();

        var config = mapper.build(agent, List.of(), TENANT_ID, PRINCIPAL);

        assertThat(config.getSchemaVersion()).isEqualTo("1");
        assertThat(config.getContext().getTenantId()).isEqualTo(TENANT_ID);
        assertThat(config.getContext().getPrincipal()).isEqualTo(PRINCIPAL);
        assertThat(config.getAgent().getId()).isEqualTo("agent-1");
        assertThat(config.getAgent().getName()).isEqualTo("agent1");
        assertThat(config.getAgent().getDescription()).isEqualTo("agent_description");
        assertThat(config.getVoice().getVoiceEnabled()).isTrue();
        assertThat(config.getVoice().getLanguageCode()).isEqualTo("en");
        assertThat(config.getModel().getId()).isEqualTo("model-1");
        assertThat(config.getModel().getName()).isEqualTo("model1");
        assertThat(config.getModel().getModelIdentifier()).isEqualTo("gpt-4.1-mini");
        assertThat(config.getModel().getModelConfig()).isEqualTo("temperature=0.2");
        assertThat(config.getModel().getCommunicationMode()).isEqualTo(CommunicationMode.SYNC.name());
        assertThat(config.getProvider().getId()).isEqualTo("provider-1");
        assertThat(config.getProvider().getName()).isEqualTo("provider1");
        assertThat(config.getProvider().getType()).isEqualTo(ProviderType.OPENAI.name());
        assertThat(config.getProvider().getAuthMode()).isEqualTo(AuthMode.API_KEY.name());
        assertThat(config.getProvider().getLlmUrl()).isEqualTo("http://some.url.org");
        assertThat(config.getProvider().getCredentialRef()).isEqualTo("credential://provider/provider-1/api-key");
    }

    @Test
    void build_globalScaffoldIsPreferredOverTenantScaffold() {
        var agent = baseAgent();
        agent.setGlobalScaffold(globalScaffold);

        var config = mapper.build(agent, List.of(), TENANT_ID, PRINCIPAL);

        assertThat(config.getScaffold().getId()).isEqualTo("gscaffold-1");
        assertThat(config.getScaffold().getName()).isEqualTo("global-scaffold1");
        assertThat(config.getScaffold().getSystemPrompt()).isEqualTo("global scaffold prompt");
        // Global scaffold exposes only global skills
        assertThat(config.getSkills()).extracting(SkillConfigurationDTOV1::getId).containsExactly("gskill-2");
    }

    @Test
    void build_tenantScaffoldListsTenantSkillsThenGlobalSkillsInOrder() {
        var agent = baseAgent();

        var config = mapper.build(agent, List.of(), TENANT_ID, PRINCIPAL);

        var skills = config.getSkills();
        assertThat(skills).hasSize(3);
        // Tenant skills first (ordered by id), then global skills
        assertThat(skills.get(0).getId()).isEqualTo("skill-1");
        assertThat(skills.get(0).getSource()).isEqualTo(SkillConfigurationDTOV1.SourceEnum.TENANT);
        assertThat(skills.get(1).getId()).isEqualTo("skill-2");
        assertThat(skills.get(1).getSource()).isEqualTo(SkillConfigurationDTOV1.SourceEnum.TENANT);
        assertThat(skills.get(2).getId()).isEqualTo("gskill-2");
        assertThat(skills.get(2).getSource()).isEqualTo(SkillConfigurationDTOV1.SourceEnum.GLOBAL);
        assertThat(skills).extracting(SkillConfigurationDTOV1::getPosition).containsExactly(0, 1, 2);
        assertThat(skills.get(0).getInstruction()).isEqualTo("extract facts");
        assertThat(skills.get(1).getInstruction()).isEqualTo("summarize response");
    }

    @Test
    void build_mcpServersIncludeOnlyMcpTools() {
        var agent = baseAgent();
        var mcpTool = mcpTool("mcp-tool-1", ExecutionPolicy.ALWAYS_ASK);
        var httpTool = tool("http-tool-1", ToolType.HTTP, null);
        var mcpGlobalTool = globalTool("mcp-global-tool-1", ToolType.MCP, ExecutionPolicy.ALWAYS_ALLOW);
        var httpGlobalTool = globalTool("http-global-tool-1", ToolType.HTTP, null);
        agent.setTools(Set.of(mcpTool, httpTool));
        agent.setGlobalTools(Set.of(mcpGlobalTool, httpGlobalTool));

        var config = mapper.build(agent, List.of(), TENANT_ID, PRINCIPAL);

        var servers = config.getMcpServers();
        assertThat(servers).extracting(McpServerConfigurationDTOV1::getId)
                .containsExactlyInAnyOrder("mcp-tool-1", "mcp-global-tool-1");
        // MCP-only filtering: HTTP tools are absent
        assertThat(servers).noneSatisfy(s -> assertThat(s.getId()).isIn("http-tool-1", "http-global-tool-1"));
        // Each server resolves an inline credential reference (never the raw key)
        assertThat(servers).allSatisfy(s -> assertThat(s.getCredentialRef())
                .isEqualTo("credential://tool/" + s.getId() + "/api-key"));
    }

    @Test
    void build_toolRulePermissions_resolveCanonicalAndLegacyValues() {
        var agent = baseAgent();
        var mcpTool = mcpTool("mcp-tool-1", ExecutionPolicy.ALWAYS_ASK);
        agent.setTools(Set.of(mcpTool));
        var rules = List.of(
                rule(mcpTool, "allowTool", ToolPermission.ALLOW), // legacy -> ALWAYS_ALLOW
                rule(mcpTool, "denyTool", ToolPermission.DENY),
                rule(mcpTool, "askTool", ToolPermission.ALWAYS_ASK),
                rule(mcpTool, "neverAskTool", ToolPermission.NEVER_ASK)); // legacy -> ALWAYS_ALLOW

        var config = mapper.build(agent, rules, TENANT_ID, PRINCIPAL);

        var server = singleServer(config);
        var byName = server.getToolRules().stream()
                .collect(java.util.stream.Collectors.toMap(ToolRuleConfigurationDTOV1::getToolName, r -> r));
        assertThat(byName.get("allowTool").getPermission()).isEqualTo(ToolRuleConfigurationDTOV1.PermissionEnum.ALWAYS_ALLOW);
        assertThat(byName.get("denyTool").getPermission()).isEqualTo(ToolRuleConfigurationDTOV1.PermissionEnum.DENY);
        assertThat(byName.get("askTool").getPermission()).isEqualTo(ToolRuleConfigurationDTOV1.PermissionEnum.ALWAYS_ASK);
        assertThat(byName.get("neverAskTool").getPermission())
                .isEqualTo(ToolRuleConfigurationDTOV1.PermissionEnum.ALWAYS_ALLOW);
        // Every explicit rule resolves to the AGENT_TOOL_RULE source
        assertThat(server.getToolRules()).allSatisfy(r -> assertThat(r.getPolicySource())
                .isEqualTo(ToolRuleConfigurationDTOV1.PolicySourceEnum.AGENT_TOOL_RULE));
    }

    @Test
    void build_unlistedToolPermission_resolvesFromExecutionPolicy() {
        var agent = baseAgent();
        var askTool = mcpTool("mcp-tool-ask", ExecutionPolicy.ALWAYS_ASK);
        var allowTool = mcpTool("mcp-tool-allow", ExecutionPolicy.ALWAYS_ALLOW);
        var legacyTool = mcpTool("mcp-tool-legacy", ExecutionPolicy.ALLOW);
        var defaultTool = mcpTool("mcp-tool-default", null);
        agent.setTools(Set.of(askTool, allowTool, legacyTool, defaultTool));

        var config = mapper.build(agent, List.of(), TENANT_ID, PRINCIPAL);

        var byId = config.getMcpServers().stream()
                .collect(java.util.stream.Collectors.toMap(McpServerConfigurationDTOV1::getId, s -> s));
        assertThat(byId.get("mcp-tool-ask").getUnlistedToolPermission()).isEqualTo(
                McpServerConfigurationDTOV1.UnlistedToolPermissionEnum.ALWAYS_ASK);
        assertThat(byId.get("mcp-tool-ask").getUnlistedToolPermissionSource()).isEqualTo(
                McpServerConfigurationDTOV1.UnlistedToolPermissionSourceEnum.TOOL_EXECUTION_POLICY);
        assertThat(byId.get("mcp-tool-allow").getUnlistedToolPermission()).isEqualTo(
                McpServerConfigurationDTOV1.UnlistedToolPermissionEnum.ALWAYS_ALLOW);
        assertThat(byId.get("mcp-tool-allow").getUnlistedToolPermissionSource()).isEqualTo(
                McpServerConfigurationDTOV1.UnlistedToolPermissionSourceEnum.TOOL_EXECUTION_POLICY);
        // Legacy ALLOW canonicalizes to ALWAYS_ALLOW
        assertThat(byId.get("mcp-tool-legacy").getUnlistedToolPermission()).isEqualTo(
                McpServerConfigurationDTOV1.UnlistedToolPermissionEnum.ALWAYS_ALLOW);
        // Missing execution policy resolves to ALWAYS_ASK from the SYSTEM_DEFAULT source
        assertThat(byId.get("mcp-tool-default").getUnlistedToolPermission()).isEqualTo(
                McpServerConfigurationDTOV1.UnlistedToolPermissionEnum.ALWAYS_ASK);
        assertThat(byId.get("mcp-tool-default").getUnlistedToolPermissionSource()).isEqualTo(
                McpServerConfigurationDTOV1.UnlistedToolPermissionSourceEnum.SYSTEM_DEFAULT);
    }

    @Test
    void build_neverLeaksRawCredentials() throws Exception {
        provider.setApiKey(PROVIDER_API_KEY);
        var agent = baseAgent();
        var mcpTool = mcpTool("mcp-tool-1", ExecutionPolicy.ALWAYS_ASK);
        mcpTool.setApiKey(MCP_TOOL_API_KEY);
        agent.setTools(Set.of(mcpTool));

        var config = mapper.build(agent, List.of(rule(mcpTool, "someTool", ToolPermission.ALLOW)), TENANT_ID, PRINCIPAL);

        var json = objectMapper.writeValueAsString(config);
        assertThat(json).doesNotContain(PROVIDER_API_KEY);
        assertThat(json).doesNotContain(MCP_TOOL_API_KEY);
        // Credential references are present instead of raw secrets
        assertThat(json).contains("credential://provider/provider-1/api-key");
        assertThat(json).contains("credential://tool/mcp-tool-1/api-key");
    }

    @Test
    void build_noToolsAndRules_hasNoMcpServers() {
        var agent = baseAgent();

        var config = mapper.build(agent, List.of(), TENANT_ID, PRINCIPAL);

        assertThat(config.getMcpServers()).isEmpty();
    }

    private McpServerConfigurationDTOV1 singleServer(AgentConfigurationDTOV1 config) {
        assertThat(config.getMcpServers()).hasSize(1);
        return config.getMcpServers().get(0);
    }

    private Agent baseAgent() {
        var agent = new Agent();
        agent.setId("agent-1");
        agent.setName("agent1");
        agent.setDescription("agent_description");
        agent.setTenantId(TENANT_ID);
        agent.setVoiceEnabled(true);
        agent.setLanguageCode("en");
        agent.setModel(model);
        agent.setScaffold(scaffold);
        return agent;
    }

    private Provider provider() {
        var provider = new Provider();
        provider.setId("provider-1");
        provider.setName("provider1");
        provider.setType(ProviderType.OPENAI);
        provider.setDescription("provider_description");
        provider.setLlmUrl("http://some.url.org");
        provider.setAuthMode(AuthMode.API_KEY);
        return provider;
    }

    private Model modelWith(Provider provider) {
        var model = new Model();
        model.setId("model-1");
        model.setName("model1");
        model.setModelIdentifier("gpt-4.1-mini");
        model.setModelConfig("temperature=0.2");
        model.setCommunicationMode(CommunicationMode.SYNC);
        model.setProvider(provider);
        return model;
    }

    private Scaffold tenantScaffold() {
        var scaffold = new Scaffold();
        scaffold.setId("scaffold-1");
        scaffold.setName("scaffold1");
        scaffold.setSystemPrompt("tenant scaffold prompt");
        var skill1 = skill("skill-1", "extract facts");
        var skill2 = skill("skill-2", "summarize response");
        scaffold.setSkills(Set.of(skill1, skill2));
        scaffold.setGlobalSkills(Set.of(globalSkill("gskill-2")));
        return scaffold;
    }

    private GlobalScaffold globalScaffold() {
        var scaffold = new GlobalScaffold();
        scaffold.setId("gscaffold-1");
        scaffold.setName("global-scaffold1");
        scaffold.setSystemPrompt("global scaffold prompt");
        scaffold.setSkills(Set.of(globalSkill("gskill-2")));
        return scaffold;
    }

    private Skill skill(String id, String instruction) {
        var skill = new Skill();
        skill.setId(id);
        skill.setName(id);
        skill.setDescription(id + " description");
        skill.setInstruction(instruction);
        return skill;
    }

    private GlobalSkill globalSkill(String id) {
        var skill = new GlobalSkill();
        skill.setId(id);
        skill.setName(id);
        skill.setDescription(id + " description");
        skill.setInstruction(id + " instruction");
        return skill;
    }

    private Tool mcpTool(String id, ExecutionPolicy executionPolicy) {
        return tool(id, ToolType.MCP, executionPolicy);
    }

    private Tool tool(String id, ToolType type, ExecutionPolicy executionPolicy) {
        var tool = new Tool();
        tool.setId(id);
        tool.setName(id);
        tool.setDescription(id + " description");
        tool.setType(type);
        tool.setUrl("http://" + id + ".org");
        tool.setExecutionPolicy(executionPolicy);
        return tool;
    }

    private GlobalTool globalTool(String id, ToolType type, ExecutionPolicy executionPolicy) {
        var tool = new GlobalTool();
        tool.setId(id);
        tool.setName(id);
        tool.setDescription(id + " description");
        tool.setType(type);
        tool.setUrl("http://" + id + ".org");
        tool.setExecutionPolicy(executionPolicy);
        return tool;
    }

    private AgentMcpToolRule rule(Tool tool, String toolName, ToolPermission allowed) {
        var rule = new AgentMcpToolRule();
        rule.setTool(tool);
        rule.setToolName(toolName);
        rule.setToolDescription(toolName + " description");
        rule.setAllowed(allowed);
        return rule;
    }
}
