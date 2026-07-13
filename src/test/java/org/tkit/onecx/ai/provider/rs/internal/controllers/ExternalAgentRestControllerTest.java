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
@TestHTTPEndpoint(ExternalAgentRestController.class)
@WithDBData(value = "data/testdata-internal.xml", deleteBeforeInsert = true, deleteAfterTest = true, rinseAndRepeat = true)
@GenerateKeycloakClient(clientName = "testClient", scopes = { "ocx-ai:all", "ocx-ai:read", "ocx-ai:write", "ocx-ai:delete" })
class ExternalAgentRestControllerTest extends AbstractTest {

    @Test
    void createExternalAgentTest() {
        var dto = new CreateExternalAgentRequestDTO();
        dto.setName("external-agent-created");
        dto.setEnabled(true);
        dto.setGroupIds(List.of("group-11-111"));

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract()
                .as(ExternalAgentDTO.class);

        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("external-agent-created");
        assertThat(created.getGroups()).isNotNull().hasSize(1);
    }

    @Test
    void createExternalAgentWithoutExistingGroupTest() {
        var dto = new CreateExternalAgentRequestDTO();
        dto.setName("external-agent-created");
        dto.setEnabled(true);
        dto.setGroupIds(List.of("not-existing"));

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract()
                .as(ExternalAgentDTO.class);

        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("external-agent-created");
        assertThat(created.getGroups()).isNotNull().isEmpty();
    }

    @Test
    void createExternalAgentWithoutGroupTest() {
        var dto = new CreateExternalAgentRequestDTO();
        dto.setName("external-agent-created");
        dto.setEnabled(true);

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract()
                .as(ExternalAgentDTO.class);

        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("external-agent-created");
        assertThat(created.getGroups()).isNotNull().isEmpty();
    }

    @Test
    void findExternalAgentByCriteriaTest() {
        var criteria = new ExternalAgentSearchCriteriaDTO();
        criteria.setPageNumber(0);
        criteria.setPageSize(10);

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(criteria)
                .post("/search")
                .then()
                .statusCode(OK.getStatusCode())
                .extract().as(ExternalAgentPageResultDTO.class);
        criteria.setEnabled(true);

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(criteria)
                .post("/search")
                .then()
                .statusCode(OK.getStatusCode())
                .extract().as(ExternalAgentPageResultDTO.class);
    }

    @Test
    void getExternalAgentByIdTest() {
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .pathParam("id", "external-agent-none-exists-id")
                .get("/{id}")
                .then().statusCode(NOT_FOUND.getStatusCode());

        var dto = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .pathParam("id", "ext-agent-11-111")
                .get("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(ExternalAgentDTO.class);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo("ext-agent-11-111");
        assertThat(dto.getName()).isEqualTo("external-agent1");
    }

    @Test
    void deleteExternalAgentTest() {
        var create = new CreateExternalAgentRequestDTO();
        create.setName("external-agent-delete");

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(create)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract().as(ExternalAgentDTO.class);

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
    void updateExternalAgentByIdTest() {
        var dto = new UpdateExternalAgentRequestDTO();
        dto.setName("external-agent-updated");

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "does-not-exists")
                .put("/{id}")
                .then().statusCode(BAD_REQUEST.getStatusCode());

        dto.setModificationCount(0);
        dto.setEnabled(false);
        dto.setGroupIds(List.of("group-11-111", "group-does-not-exist"));

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
                .pathParam("id", "ext-agent-11-111")
                .put("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(ExternalAgentDTO.class);

        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo("external-agent-updated");
        assertThat(updated.getGroups()).isNotNull().hasSize(1);
        assertThat(updated.getModificationCount()).isNotEqualTo(dto.getModificationCount());

        dto.setModificationCount(0);
        //optimistic lock
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "ext-agent-11-111")
                .put("/{id}")
                .then().statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    void updateExternalAgentByIdWithoutGroupsTest() {
        var dto = new UpdateExternalAgentRequestDTO();
        dto.setName("external-agent-updated");
        dto.setModificationCount(0);
        dto.setEnabled(false);

        var updated = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "ext-agent-11-111")
                .put("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(ExternalAgentDTO.class);

        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo("external-agent-updated");
        assertThat(updated.getGroups()).isNotNull().isEmpty();
        assertThat(updated.getModificationCount()).isNotEqualTo(dto.getModificationCount());
    }
}
