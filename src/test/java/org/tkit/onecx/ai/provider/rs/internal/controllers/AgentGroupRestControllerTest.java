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
@TestHTTPEndpoint(AgentGroupRestController.class)
@WithDBData(value = "data/testdata-internal.xml", deleteBeforeInsert = true, deleteAfterTest = true, rinseAndRepeat = true)
@GenerateKeycloakClient(clientName = "testClient", scopes = { "ocx-ai:all", "ocx-ai:read", "ocx-ai:write", "ocx-ai:delete" })
class AgentGroupRestControllerTest extends AbstractTest {

    @Test
    void createAgentGroupTest() {
        var dto = new CreateAgentGroupRequestDTO();
        dto.setName("group-created");

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract()
                .as(AgentGroupDTO.class);

        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("group-created");
        assertThat(created.getOrchestrationMode()).isEqualTo(AgentGroupOrchestrationModeDTO.LEAD_DELEGATES);
        assertThat(created.getResponseStrategy()).isEqualTo(AgentGroupResponseStrategyDTO.LAST);
    }

    @Test
    void findAgentGroupByCriteriaTest() {
        var criteria = new AgentGroupSearchCriteriaDTO();

        var data = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(criteria)
                .post("/search")
                .then()
                .statusCode(OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract()
                .as(AgentGroupPageResultDTO.class);

        assertThat(data).isNotNull();
        assertThat(data.getTotalElements()).isEqualTo(1);
        assertThat(data.getStream()).isNotNull().hasSize(1);

        criteria.setName("group1");
        criteria.setOrchestrationMode(AgentGroupOrchestrationModeDTO.LEAD_DELEGATES);
        data = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(criteria)
                .post("/search")
                .then()
                .statusCode(OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract()
                .as(AgentGroupPageResultDTO.class);

        assertThat(data.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getAgentGroupByIdTest() {
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .pathParam("id", "group-none-exists-id")
                .get("/{id}")
                .then().statusCode(NOT_FOUND.getStatusCode());

        var dto = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .pathParam("id", "group-11-111")
                .get("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(AgentGroupDTO.class);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo("group-11-111");
        assertThat(dto.getName()).isEqualTo("group1");
    }

    @Test
    void deleteAgentGroupTest() {
        var create = new CreateAgentGroupRequestDTO();
        create.setName("group-delete");

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(create)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract().as(AgentGroupDTO.class);

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
    void updateAgentGroupTest() {
        var dto = new UpdateAgentGroupRequestDTO();
        dto.setName("updated-group");

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "does-not-exists")
                .put("/{id}")
                .then().statusCode(BAD_REQUEST.getStatusCode());

        dto.setModificationCount(0);

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
                .pathParam("id", "group-11-111")
                .put("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(AgentGroupDTO.class);

        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo(dto.getName());
        assertThat(updated.getModificationCount()).isNotEqualTo(dto.getModificationCount());

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "group-11-111")
                .put("/{id}")
                .then().statusCode(BAD_REQUEST.getStatusCode());
    }
}
