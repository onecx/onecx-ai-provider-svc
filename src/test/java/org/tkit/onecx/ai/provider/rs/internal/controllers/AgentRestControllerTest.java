package org.tkit.onecx.ai.provider.rs.internal.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.*;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.test.AbstractTest;
import org.tkit.quarkus.security.test.GenerateKeycloakClient;
import org.tkit.quarkus.test.WithDBData;

import gen.org.tkit.onecx.ai.provider.rs.internal.model.*;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(AgentRestController.class)
@WithDBData(value = "data/testdata-internal.xml", deleteBeforeInsert = true, deleteAfterTest = true, rinseAndRepeat = true)
@GenerateKeycloakClient(clientName = "testClient", scopes = { "ocx-ai:all", "ocx-ai:read", "ocx-ai:write", "ocx-ai:delete" })
class AgentRestControllerTest extends AbstractTest {

    @Test
    void createAgentTest() {
        var dto = new CreateAgentRequestDTO();
        dto.setName("agent-created");
        dto.setStatus(AgentStatusDTO.DRAFT);

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract()
                .as(AgentDTO.class);

        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("agent-created");
        assertThat(created.getStatus()).isEqualTo(AgentStatusDTO.DRAFT);
    }

    @Test
    void createAgentWithExistingDataTest() {
        var dto = new CreateAgentRequestDTO();
        dto.setName("agent-created");
        dto.setStatus(AgentStatusDTO.DRAFT);
        dto.setTools(List.of(new ToolDTO().id("tool-11-111")));
        dto.setModel(new ModelDTO().id("model-11-111"));
        dto.setScaffold(new ScaffoldDTO().id("scaffold-11-111"));
        dto.setGroups(List.of(new AgentGroupDTO().id("group-11-111")));

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract()
                .as(AgentDTO.class);

        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("agent-created");
        assertThat(created.getStatus()).isEqualTo(AgentStatusDTO.DRAFT);
    }

    @Test
    void createAgentWithoutExistingDataTest() {
        var dto = new CreateAgentRequestDTO();
        dto.setName("agent-created");
        dto.setStatus(AgentStatusDTO.DRAFT);
        dto.setTools(List.of(new ToolDTO().id("tool-x")));
        dto.setModel(new ModelDTO().id("model-x"));
        dto.setScaffold(new ScaffoldDTO().id("scaffold-x"));
        dto.setGroups(List.of(new AgentGroupDTO().id("group-x")));

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract()
                .as(AgentDTO.class);

        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("agent-created");
        assertThat(created.getStatus()).isEqualTo(AgentStatusDTO.DRAFT);
    }

    @Test
    void findAgentBySearchCriteriaTest() {
        var criteria = new AgentSearchCriteriaDTO();
        var data = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(criteria)
                .post("/search")
                .then()
                .statusCode(OK.getStatusCode())
                .extract()
                .as(AgentPageResultDTO.class);

        assertThat(data).isNotNull();
        assertThat(data.getTotalElements()).isEqualTo(4);
        assertThat(data.getStream()).isNotNull().hasSize(4);

        criteria.setName("agent2");
        criteria.setStatus(AgentStatusDTO.DRAFT);
        data = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(criteria)
                .post("/search")
                .then()
                .statusCode(OK.getStatusCode())
                .extract()
                .as(AgentPageResultDTO.class);

        assertThat(data.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getAgentByIdTest() {
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .pathParam("id", "agent-none-exists-id")
                .get("/{id}")
                .then().statusCode(NOT_FOUND.getStatusCode());

        var dto = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .pathParam("id", "agent-11-111")
                .get("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(AgentDTO.class);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo("agent-11-111");
        assertThat(dto.getName()).isEqualTo("agent1");
    }

    @Test
    void deleteAgentTest() {
        var create = new CreateAgentRequestDTO();
        create.setName("agent-delete");

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(create)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract().as(AgentDTO.class);

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
    void updateAgentTest() {
        var dto = new UpdateAgentRequestDTO();
        dto.setName("agent-updated");

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "does-not-exists")
                .put("/{id}")
                .then().statusCode(BAD_REQUEST.getStatusCode());

        dto.setModificationCount(0);
        dto.setStatus(AgentStatusDTO.LIVE);
        dto.setA2aEnabled(true);

        var tool = new ToolDTO();
        tool.setId("tool-11-111");

        var model = new ModelDTO();
        model.setId("model-11-111");

        var scaffold = new ScaffoldDTO();
        scaffold.setId("scaffold-11-111");

        var group = new AgentGroupDTO();
        group.setId("group-11-111");

        dto.setTools(List.of(tool));
        dto.setModel(model);
        dto.setScaffold(scaffold);
        dto.setGroups(List.of(group));

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
                .pathParam("id", "agent-11-111")
                .put("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(AgentDTO.class);

        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo("agent-updated");
        assertThat(updated.getTools()).isNotNull().isNotEmpty();
        assertThat(updated.getGroups()).isNotNull().isNotEmpty();
        assertThat(updated.getModificationCount()).isNotEqualTo(dto.getModificationCount());

        dto.setModificationCount(0);
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "agent-11-111")
                .put("/{id}")
                .then().statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    void updateAgentWithNotExistingDataTest() {
        var dto = new UpdateAgentRequestDTO();
        dto.setName("agent-updated");

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "does-not-exists")
                .put("/{id}")
                .then().statusCode(BAD_REQUEST.getStatusCode());

        dto.setModificationCount(0);
        dto.setStatus(AgentStatusDTO.LIVE);
        dto.setA2aEnabled(true);

        var tool = new ToolDTO();
        tool.setId("not-existing");

        var model = new ModelDTO();
        model.setId("not-existing");

        var scaffold = new ScaffoldDTO();
        scaffold.setId("not-existing");

        var group = new AgentGroupDTO();
        group.setId("not-existing");

        dto.setTools(List.of(tool));
        dto.setModel(model);
        dto.setScaffold(scaffold);
        dto.setGroups(List.of(group));

        var updated = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "agent-11-111")
                .put("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(AgentDTO.class);

        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo("agent-updated");
        assertThat(updated.getTools()).isNotNull().isNotEmpty();
        assertThat(updated.getGroups()).isNotNull().isNotEmpty();
        assertThat(updated.getModificationCount()).isNotEqualTo(dto.getModificationCount());

    }

    @Test
    void updateAgentWithoutDataTest() {
        var dto = new UpdateAgentRequestDTO();
        dto.setName("agent-updated");

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "does-not-exists")
                .put("/{id}")
                .then().statusCode(BAD_REQUEST.getStatusCode());

        dto.setModificationCount(0);
        dto.setStatus(AgentStatusDTO.LIVE);
        dto.setA2aEnabled(true);

        var updated = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "agent-11-111")
                .put("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(AgentDTO.class);

        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo("agent-updated");
        assertThat(updated.getTools()).isNotNull().isEmpty();
        assertThat(updated.getGroups()).isNotNull().isEmpty();
        assertThat(updated.getModificationCount()).isNotEqualTo(dto.getModificationCount());

    }
}
