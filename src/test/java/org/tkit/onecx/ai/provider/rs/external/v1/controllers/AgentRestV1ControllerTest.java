package org.tkit.onecx.ai.provider.rs.external.v1.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.test.AbstractTest;
import org.tkit.quarkus.security.test.GenerateKeycloakClient;
import org.tkit.quarkus.test.WithDBData;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.*;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(AgentRestV1Controller.class)
@WithDBData(value = "data/testdata-internal.xml", deleteBeforeInsert = true, deleteAfterTest = true, rinseAndRepeat = true)
@GenerateKeycloakClient(clientName = "testClient", scopes = { "ocx-ai:read" })
class AgentRestV1ControllerTest extends AbstractTest {

    @Test
    void findAgentBySearchCriteriaTest() {
        var criteria = new AgentSearchCriteriaDTOV1();
        var data = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(criteria)
                .post("/search")
                .then()
                .statusCode(OK.getStatusCode())
                .extract()
                .as(AgentPageResultDTOV1.class);

        assertThat(data).isNotNull();
        assertThat(data.getTotalElements()).isEqualTo(3);
        assertThat(data.getStream()).isNotNull().hasSize(3);

        var voiceAgent = data.getStream().stream().filter(a -> a.getId().equals("agent-44-444")).findFirst().orElseThrow();
        assertThat(voiceAgent.getVoiceEnabled()).isTrue();
        assertThat(voiceAgent.getLanguageCode()).isEqualTo("en");

        var plainAgent = data.getStream().stream().filter(a -> a.getId().equals("agent-11-111")).findFirst().orElseThrow();
        assertThat(plainAgent.getVoiceEnabled()).isFalse();
        assertThat(plainAgent.getLanguageCode()).isNull();

        criteria.setPageNumber(1);
        criteria.setPageSize(2);
        data = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(criteria)
                .post("/search")
                .then()
                .statusCode(OK.getStatusCode())
                .extract()
                .as(AgentPageResultDTOV1.class);

        assertThat(data.getTotalElements()).isEqualTo(3);
        assertThat(data.getStream()).isNotNull().hasSize(1);
    }

    @Test
    void getAgentConfiguration_resolvesSecretFreeConfiguration() {
        var response = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .basePath("/v1/agents")
                .pathParam("id", "agent-11-111")
                .get("/{id}/configuration");

        assertThat(response.statusCode()).isEqualTo(OK.getStatusCode());

        var config = response.as(AgentConfigurationDTOV1.class);
        assertThat(config.getSchemaVersion()).isEqualTo("1");
        assertThat(config.getContext().getTenantId()).isEqualTo("default");
        assertThat(config.getAgent().getId()).isEqualTo("agent-11-111");
        assertThat(config.getAgent().getName()).isEqualTo("agent1");
        assertThat(config.getModel().getModelIdentifier()).isEqualTo("gpt-4.1-mini");
        assertThat(config.getProvider().getType()).isEqualTo("OPENAI");
        assertThat(config.getProvider().getCredentialRef()).isEqualTo("credential://provider/provider-11-111/api-key");
        assertThat(config.getScaffold().getSystemPrompt()).isEqualTo("tenant scaffold prompt");

        // Tenant skills first (ordered), then global skills
        var skills = config.getSkills();
        assertThat(skills).hasSize(3);
        assertThat(skills.get(0).getId()).isEqualTo("skill-11-111");
        assertThat(skills.get(0).getSource()).isEqualTo(SkillConfigurationDTOV1.SourceEnum.TENANT);
        assertThat(skills.get(1).getId()).isEqualTo("skill-22-222");
        assertThat(skills.get(1).getSource()).isEqualTo(SkillConfigurationDTOV1.SourceEnum.TENANT);
        assertThat(skills.get(2).getId()).isEqualTo("gskill-11-111");
        assertThat(skills.get(2).getSource()).isEqualTo(SkillConfigurationDTOV1.SourceEnum.GLOBAL);
        assertThat(skills).extracting(SkillConfigurationDTOV1::getPosition).containsExactly(0, 1, 2);

        // One configured MCP server with resolved permissions
        var server = config.getMcpServers();
        assertThat(server).hasSize(1);
        var mcp = server.get(0);
        assertThat(mcp.getId()).isEqualTo("tool-11-111");
        assertThat(mcp.getUrl()).isEqualTo("http://mcp.server.org");
        assertThat(mcp.getCredentialRef()).isEqualTo("credential://tool/tool-11-111/api-key");
        assertThat(mcp.getUnlistedToolPermission())
                .isEqualTo(McpServerConfigurationDTOV1.UnlistedToolPermissionEnum.ALWAYS_ASK);
        assertThat(mcp.getUnlistedToolPermissionSource())
                .isEqualTo(McpServerConfigurationDTOV1.UnlistedToolPermissionSourceEnum.TOOL_EXECUTION_POLICY);
        var rules = mcp.getToolRules().stream()
                .collect(java.util.stream.Collectors.toMap(ToolRuleConfigurationDTOV1::getToolName, r -> r));
        assertThat(rules).hasSize(2);
        assertThat(rules.get("getProposal").getPermission()).isEqualTo(ToolRuleConfigurationDTOV1.PermissionEnum.ALWAYS_ALLOW);
        assertThat(rules.get("getProposal").getPolicySource())
                .isEqualTo(ToolRuleConfigurationDTOV1.PolicySourceEnum.AGENT_TOOL_RULE);
        assertThat(rules.get("deleteProposal").getPermission()).isEqualTo(ToolRuleConfigurationDTOV1.PermissionEnum.DENY);
        assertThat(rules.get("deleteProposal").getPolicySource())
                .isEqualTo(ToolRuleConfigurationDTOV1.PolicySourceEnum.AGENT_TOOL_RULE);

        // Raw credentials must never be exposed
        assertThat(response.getBody().asString()).doesNotContain("api_key_1");
    }

    @Test
    void getAgentConfiguration_unknownAgent_returnsNotFound() {
        var response = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .basePath("/v1/agents")
                .pathParam("id", "agent-not-exists")
                .get("/{id}/configuration");

        assertThat(response.statusCode()).isEqualTo(NOT_FOUND.getStatusCode());
    }

    @Test
    void getAgentConfiguration_duplicateToolRules_returnsConflict() {
        var response = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .basePath("/v1/agents")
                .pathParam("id", "agent-33-333")
                .get("/{id}/configuration");

        assertThat(response.statusCode()).isEqualTo(CONFLICT.getStatusCode());
        assertThat(response.jsonPath().getString("errorCode")).isEqualTo("DUPLICATE_AGENT_TOOL_RULE");
    }
}
