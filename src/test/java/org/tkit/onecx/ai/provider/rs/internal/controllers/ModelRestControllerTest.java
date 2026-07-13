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
@TestHTTPEndpoint(ModelRestController.class)
@WithDBData(value = "data/testdata-internal.xml", deleteBeforeInsert = true, deleteAfterTest = true, rinseAndRepeat = true)
@GenerateKeycloakClient(clientName = "testClient", scopes = { "ocx-ai:all", "ocx-ai:read", "ocx-ai:write", "ocx-ai:delete" })
class ModelRestControllerTest extends AbstractTest {

    @Test
    void createModelTest() {
        var dto = new CreateModelRequestDTO();
        dto.setName("model-created");
        dto.setCommunicationMode(CommunicationModeDTO.SYNC);

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract()
                .as(ModelDTO.class);

        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("model-created");
        assertThat(created.getCommunicationMode()).isEqualTo(CommunicationModeDTO.SYNC);
    }

    @Test
    void findModelByCriteriaTest() {
        var criteria = new ModelSearchCriteriaDTO();
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
                .as(ModelPageResultDTO.class);

        assertThat(data).isNotNull();
        assertThat(data.getTotalElements()).isEqualTo(2);
        assertThat(data.getStream()).isNotNull().hasSize(2);

        criteria.setName("model1");
        criteria.setCommunicationMode(CommunicationModeDTO.SYNC);
        data = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(criteria)
                .post("/search")
                .then()
                .statusCode(OK.getStatusCode())
                .extract()
                .as(ModelPageResultDTO.class);

        assertThat(data.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getModelByIdTest() {
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .pathParam("id", "model-none-exists-id")
                .get("/{id}")
                .then().statusCode(NOT_FOUND.getStatusCode());

        var dto = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .pathParam("id", "model-11-111")
                .get("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(ModelDTO.class);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo("model-11-111");
        assertThat(dto.getName()).isEqualTo("model1");
    }

    @Test
    void deleteModelTest() {
        var create = new CreateModelRequestDTO();
        create.setName("model-delete");

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(create)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract().as(ModelDTO.class);

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
    void updateModelByIdTest() {
        var dto = new UpdateModelRequestDTO();
        dto.setName("model-updated");

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "does-not-exists")
                .put("/{id}")
                .then().statusCode(BAD_REQUEST.getStatusCode());

        dto.setModificationCount(0);
        dto.setCommunicationMode(CommunicationModeDTO.ASYNC);

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
                .pathParam("id", "model-11-111")
                .put("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(ModelDTO.class);

        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo("model-updated");
        assertThat(updated.getModificationCount()).isNotEqualTo(dto.getModificationCount());

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "model-11-111")
                .put("/{id}")
                .then().statusCode(BAD_REQUEST.getStatusCode());
    }
}
