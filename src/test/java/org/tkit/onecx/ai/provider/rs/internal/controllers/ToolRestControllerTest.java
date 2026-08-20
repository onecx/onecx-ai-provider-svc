package org.tkit.onecx.ai.provider.rs.internal.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.*;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.test.AbstractTest;
import org.tkit.quarkus.security.test.GenerateKeycloakClient;
import org.tkit.quarkus.test.WithDBData;

import gen.org.tkit.onecx.ai.provider.rs.internal.model.*;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(ToolRestController.class)
@WithDBData(value = "data/testdata-internal.xml", deleteBeforeInsert = true, deleteAfterTest = true, rinseAndRepeat = true)
@GenerateKeycloakClient(clientName = "testClient", scopes = { "ocx-ai:all", "ocx-ai:read", "ocx-ai:write", "ocx-ai:delete" })
class ToolRestControllerTest extends AbstractTest {

    @Test
    void createToolTest() {
        var dto = new CreateToolRequestDTO();
        dto.setName("tool-created");
        dto.setType(ToolTypeDTO.HTTP);

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract()
                .as(ToolDTO.class);

        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("tool-created");
        assertThat(created.getType()).isEqualTo(ToolTypeDTO.HTTP);
    }

    @Test
    void findToolByCriteriaTest() {
        var criteria = new ToolSearchCriteriaDTO();
        criteria.setPageNumber(0);
        criteria.setPageSize(10);

        var data = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(criteria)
                .post("/search")
                .then()
                .statusCode(OK.getStatusCode())
                .extract()
                .as(ToolPageResultDTO.class);

        assertThat(data).isNotNull();
        assertThat(data.getTotalElements()).isEqualTo(3);
        assertThat(data.getStream()).isNotNull().hasSize(3);

        criteria.setType(ToolTypeDTO.MCP);
        data = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(criteria)
                .post("/search")
                .then()
                .statusCode(OK.getStatusCode())
                .extract()
                .as(ToolPageResultDTO.class);

        assertThat(data.getTotalElements()).isEqualTo(1);
        assertThat(data.getStream()).isNotNull().hasSize(1);
    }

    @Test
    void getToolByIdTest() {
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .pathParam("id", "tool-none-exists-id")
                .get("/{id}")
                .then().statusCode(NOT_FOUND.getStatusCode());

        var dto = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .pathParam("id", "tool-11-111")
                .get("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(ToolDTO.class);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo("tool-11-111");
        assertThat(dto.getType()).isEqualTo(ToolTypeDTO.MCP);
    }

    @Test
    void deleteToolTest() {
        var create = new CreateToolRequestDTO();
        create.setName("tool-delete");
        create.setType(ToolTypeDTO.CUSTOM);

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(create)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract().as(ToolDTO.class);

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .pathParam("id", created.getId())
                .delete("/{id}")
                .then().statusCode(NO_CONTENT.getStatusCode());

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .pathParam("id", created.getId())
                .get("/{id}")
                .then().statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    void deleteToolCascadesRulesTest() {
        var create = new CreateToolRequestDTO();
        create.setName("tool-cascade");
        create.setType(ToolTypeDTO.MCP);

        var createdTool = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(create)
                .post()
                .then().statusCode(CREATED.getStatusCode())
                .extract().as(ToolDTO.class);

        var rule = new CreateAgentMcpToolRuleRequestDTO();
        rule.setToolName("someTool");
        rule.setAllowed(ToolPermissionDTO.ALLOW);

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(rule)
                .basePath("/internal/agents")
                .pathParam("agentId", "agent-11-111")
                .pathParam("toolId", createdTool.getId())
                .post("/{agentId}/tools/{toolId}/mcp-tool-rules")
                .then().statusCode(CREATED.getStatusCode());

        // verify rule exists
        var rules = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .basePath("/internal/agents")
                .pathParam("agentId", "agent-11-111")
                .pathParam("toolId", createdTool.getId())
                .get("/{agentId}/tools/{toolId}/mcp-tool-rules")
                .then().statusCode(OK.getStatusCode())
                .extract().as(AgentMcpToolRuleListDTO.class);
        assertThat(rules.getRules()).hasSize(1);

        // delete tool cascades to rules
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .pathParam("id", createdTool.getId())
                .delete("/{id}")
                .then().statusCode(NO_CONTENT.getStatusCode());

        // rules are gone
        var afterDelete = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .basePath("/internal/agents")
                .pathParam("agentId", "agent-11-111")
                .pathParam("toolId", createdTool.getId())
                .get("/{agentId}/tools/{toolId}/mcp-tool-rules")
                .then().statusCode(OK.getStatusCode())
                .extract().as(AgentMcpToolRuleListDTO.class);
        assertThat(afterDelete.getRules()).isEmpty();
    }

    @Test
    void updateToolByIdTest() {
        var dto = new UpdateToolRequestDTO();
        dto.setName("tool-updated");

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "does-not-exists")
                .put("/{id}")
                .then().statusCode(BAD_REQUEST.getStatusCode());

        dto.setModificationCount(0);
        dto.setType(ToolTypeDTO.HTTP);
        dto.setExecutionPolicy(ExecutionPolicyDTO.ALWAYS_ASK);

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "does-not-exists")
                .put("/{id}")
                .then().statusCode(NOT_FOUND.getStatusCode());

        var updated = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "tool-11-111")
                .put("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(ToolDTO.class);

        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo("tool-updated");
        assertThat(updated.getModificationCount()).isNotEqualTo(dto.getModificationCount());

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "tool-11-111")
                .put("/{id}")
                .then().statusCode(BAD_REQUEST.getStatusCode());
    }
}
