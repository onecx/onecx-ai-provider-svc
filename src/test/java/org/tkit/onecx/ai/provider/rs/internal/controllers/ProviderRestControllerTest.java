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
@TestHTTPEndpoint(ProviderRestController.class)
@WithDBData(value = "data/testdata-internal.xml", deleteBeforeInsert = true, deleteAfterTest = true, rinseAndRepeat = true)
@GenerateKeycloakClient(clientName = "testClient", scopes = { "ocx-ai:all", "ocx-ai:read", "ocx-ai:write", "ocx-ai:delete" })
class ProviderRestControllerTest extends AbstractTest {

    @Test
    void createProviderTest() {
        // create provider
        var providerDto = new CreateProviderRequestDTO();
        providerDto.setName("Provider");
        providerDto.setModelName("ModelName");

        var id = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .when()
                .contentType(APPLICATION_JSON)
                .body(providerDto)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract()
                .body().as(ProviderDTO.class);

        assertThat(id).isNotNull();
        assertThat(id.getName()).isEqualTo(providerDto.getName());
        assertThat(id.getModelName()).isEqualTo(providerDto.getModelName());
    }

    @Test
    void findProviderBySearchCriteriaTest() {
        var criteria = new ProviderSearchCriteriaDTO();

        var data = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(criteria)
                .post("/search")
                .then()
                .statusCode(OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract()
                .as(ProviderPageResultDTO.class);

        assertThat(data).isNotNull();
        assertThat(data.getTotalElements()).isEqualTo(3);
        assertThat(data.getStream()).isNotNull().hasSize(3);

        criteria.setName("provider2");
        data = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(criteria)
                .post("/search")
                .then()
                .statusCode(OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract()
                .as(ProviderPageResultDTO.class);

        assertThat(data).isNotNull();
        assertThat(data.getTotalElements()).isEqualTo(1);
        assertThat(data.getStream()).isNotNull().hasSize(1);
    }

    @Test
    void getProviderByIdTest() {
        //provider none exists
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .when()
                .pathParam("id", "provider-none-exists-id")
                .get("/{id}")
                .then().statusCode(NOT_FOUND.getStatusCode());

        //provider exists
        var dto = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .when()
                .pathParam("id", "provider-11-111")
                .get("/{id}")
                .then().statusCode(OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract()
                .body().as(ProviderDTO.class);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo("provider-11-111");
        assertThat(dto.getName()).isEqualTo("provider1");
        assertThat(dto.getDescription()).isEqualTo("provider_description_1");
        assertThat(dto.getLlmUrl()).isEqualTo("http://some.url.org");
        assertThat(dto.getModelName()).isEqualTo("model1");

    }

    @Test
    void deleteProviderTest() {
        //get provider and check if exists
        var dto = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .when()
                .pathParam("id", "provider-DELETE_1")
                .get("/{id}")
                .then().statusCode(OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract()
                .body().as(ProviderDTO.class);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo("provider-DELETE_1");

        //delete provider
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .pathParam("id", "provider-DELETE_1")
                .delete("/{id}")
                .then().statusCode(NO_CONTENT.getStatusCode());

        //check if provider exists
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .pathParam("id", "provider-DELETE_1")
                .get("/{id}")
                .then().statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    void updateProviderTest() {

        var providerDto = new UpdateProviderRequestDTO();
        providerDto.setName("updated-Provider");
        providerDto.setModelName("updated-ModelName");

        //update with missing modificationCount => constraint exception
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(providerDto)
                .when()
                .pathParam("id", "does-not-exists")
                .put("/{id}")
                .then().statusCode(BAD_REQUEST.getStatusCode());

        providerDto.setModificationCount(0);

        //update none existing provider
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(providerDto)
                .when()
                .pathParam("id", "does-not-exists")
                .put("/{id}")
                .then().statusCode(NOT_FOUND.getStatusCode());

        //update provider
        var dto = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(providerDto)
                .when()
                .pathParam("id", "provider-11-111")
                .put("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(ProviderDTO.class);
        assertThat(dto.getModificationCount()).isNotEqualTo(providerDto.getModificationCount());
        assertThat(dto).isNotNull();
        assertThat(dto.getName()).isEqualTo(providerDto.getName());
        assertThat(dto.getModelName()).isEqualTo(providerDto.getModelName());

        //update with same modificationCount => optimistic lock exception
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(providerDto)
                .when()
                .pathParam("id", "provider-11-111")
                .put("/{id}")
                .then().statusCode(BAD_REQUEST.getStatusCode());
    }
}
