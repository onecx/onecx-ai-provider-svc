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
@TestHTTPEndpoint(ConfigurationRestController.class)
@WithDBData(value = "data/testdata-internal.xml", deleteBeforeInsert = true, deleteAfterTest = true, rinseAndRepeat = true)
@GenerateKeycloakClient(clientName = "testClient", scopes = { "ocx-ai:all", "ocx-ai:read", "ocx-ai:write", "ocx-ai:delete" })
class ConfigurationRestControllerTest extends AbstractTest {

    @Test
    void deleteConfigurationTest() {
        //delete configuration
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .pathParam("id", "configuration-DELETE_1")
                .delete("/{id}")
                .then().statusCode(NO_CONTENT.getStatusCode());

        //check if configuration exists
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .pathParam("id", "configuration-DELETE_1")
                .get("/{id}")
                .then().statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    void updateConfigurationTest() {
        var configurationDto = new UpdateConfigurationRequestDTO();
        configurationDto.setName("custom-name-app-id");
        configurationDto.setModificationCount(0);

        //update none existing ai configuration
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(configurationDto)
                .when()
                .pathParam("id", "does-not-exists")
                .put("/{id}")
                .then().statusCode(NOT_FOUND.getStatusCode());

        //update configuration
        var dto = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(configurationDto)
                .when()
                .pathParam("id", "configuration-11-111")
                .put("/{id}")
                .then().statusCode(OK.getStatusCode()).extract().as(ConfigurationDTO.class);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo("configuration-11-111");
        assertThat(dto.getName()).isEqualTo(configurationDto.getName());

        // update second time => optimistic lock exception
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(configurationDto)
                .when()
                .pathParam("id", "configuration-11-111")
                .put("/{id}")
                .then().statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    void getConfigurationTest() {
        //try to get configuration with non-existing id
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .when()
                .pathParam("id", "non-existing-configuration-id")
                .get("/{id}")
                .then().statusCode(NOT_FOUND.getStatusCode());

        //get configuration success
        var configurationDto = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .when()
                .pathParam("id", "configuration-11-111")
                .get("/{id}")
                .then().statusCode(OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract()
                .body().as(ConfigurationDTO.class);

        assertThat(configurationDto).isNotNull();
        assertThat(configurationDto.getId()).isEqualTo("configuration-11-111");
        assertThat(configurationDto.getName()).isEqualTo("configuration1");
    }

    @Test
    void findConfigurationBySearchCriteriaTest() {
        var criteria = new ConfigurationSearchCriteriaDTO();

        var data = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(criteria)
                .post("/search")
                .then()
                .statusCode(OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract()
                .as(ConfigurationPageResultDTO.class);

        assertThat(data).isNotNull();
        assertThat(data.getTotalElements()).isEqualTo(3);
        assertThat(data.getStream()).isNotNull().hasSize(3);

        criteria.setName("configuration2");
        data = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(criteria)
                .post("/search")
                .then()
                .statusCode(OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract()
                .as(ConfigurationPageResultDTO.class);

        assertThat(data).isNotNull();
        assertThat(data.getTotalElements()).isEqualTo(1);
        assertThat(data.getStream()).isNotNull().hasSize(1);

    }

    @Test
    void createConfigurationTest() {
        var configuration = new CreateConfigurationRequestDTO();
        configuration.setDescription("test-config1");

        //constraint exception - name is null
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(configuration)
                .post()
                .then()
                .statusCode(BAD_REQUEST.getStatusCode());

        configuration.setName("config1");

        var data = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(configuration)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract()
                .as(ConfigurationDTO.class);
        assertThat(data).isNotNull();
    }
}
