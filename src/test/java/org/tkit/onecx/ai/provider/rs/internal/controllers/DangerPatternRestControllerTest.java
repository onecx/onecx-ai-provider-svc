package org.tkit.onecx.ai.provider.rs.internal.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.test.AbstractTest;
import org.tkit.quarkus.security.test.GenerateKeycloakClient;
import org.tkit.quarkus.test.WithDBData;

import gen.org.tkit.onecx.ai.provider.rs.internal.model.*;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(DangerPatternRestController.class)
@WithDBData(value = "data/testdata-internal.xml", deleteBeforeInsert = true, deleteAfterTest = true, rinseAndRepeat = true)
@GenerateKeycloakClient(clientName = "testClient", scopes = { "ocx-ai:all", "ocx-ai:read", "ocx-ai:write",
        "ocx-ai:delete" })
class DangerPatternRestControllerTest extends AbstractTest {

    @Test
    void dangerPatternCrudTest() {
        var list = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .get()
                .then().statusCode(OK.getStatusCode())
                .extract().as(DangerPatternListDTO.class);

        // seeded patterns from Liquibase
        assertThat(list.getPatterns()).isNotEmpty();

        var create = new CreateDangerPatternRequestDTO();
        create.setPattern("purge");
        create.setDangerLevel(DangerLevelDTO.DANGEROUS);
        create.setDescription("Purges data");

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(create)
                .post()
                .then().statusCode(CREATED.getStatusCode())
                .extract().as(DangerPatternDTO.class);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getPattern()).isEqualTo("purge");

        var update = new UpdateDangerPatternRequestDTO();
        update.setModificationCount(created.getModificationCount());
        update.setPattern("purge");
        update.setDangerLevel(DangerLevelDTO.WARNING);

        var updated = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(update)
                .pathParam("id", created.getId())
                .put("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(DangerPatternDTO.class);

        assertThat(updated.getDangerLevel()).isEqualTo(DangerLevelDTO.WARNING);

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(update)
                .pathParam("id", "pattern-not-exists")
                .put("/{id}")
                .then().statusCode(NOT_FOUND.getStatusCode());

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .pathParam("id", created.getId())
                .delete("/{id}")
                .then().statusCode(NO_CONTENT.getStatusCode());

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .pathParam("id", created.getId())
                .delete("/{id}")
                .then().statusCode(NOT_FOUND.getStatusCode());
    }
}
