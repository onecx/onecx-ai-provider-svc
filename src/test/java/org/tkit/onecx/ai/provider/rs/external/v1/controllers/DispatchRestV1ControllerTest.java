package org.tkit.onecx.ai.provider.rs.external.v1.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.OK;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.test.AbstractTest;
import org.tkit.quarkus.security.test.GenerateKeycloakClient;
import org.tkit.quarkus.test.WithDBData;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.*;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(DispatchRestV1Controller.class)
@WithDBData(value = "data/testdata-dispatch-v1.xml", deleteBeforeInsert = true, deleteAfterTest = true, rinseAndRepeat = true)
@GenerateKeycloakClient(clientName = "testExtClient", scopes = { "ocx-ai-provider:read" })
public class DispatchRestV1ControllerTest extends AbstractTest {

    @Test
    void chatRequestWrongConfigurationTest() {
        var request = new ChatRequestDTOV1()
                .requestContext(new RequestContextDTOV1().filter(
                        new ConfigurationFilterDTOV1().key(ConfigurationFilterDTOV1.KeyEnum.APP_ID).value("wrong-app-id")))
                .chatMessage(new ChatMessageDTOV1().message("Test"));

        given()
                .auth().oauth2(getKeycloakClientToken("testExtClient"))
                .contentType(APPLICATION_JSON)
                .body(request)
                .when()
                .post()
                .then().statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void chatRequestConfigurationNoProviderTest() {
        var request = new ChatRequestDTOV1()
                .requestContext(new RequestContextDTOV1().filter(
                        new ConfigurationFilterDTOV1().key(ConfigurationFilterDTOV1.KeyEnum.APP_ID).value("test-no-provider")))
                .chatMessage(new ChatMessageDTOV1().message("Test"));

        given()
                .auth().oauth2(getKeycloakClientToken("testExtClient"))
                .contentType(APPLICATION_JSON)
                .body(request)
                .when()
                .post()
                .then().statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void chatRequestConfigurationWrongProviderTest() {
        var request = new ChatRequestDTOV1()
                .requestContext(new RequestContextDTOV1().filter(
                        new ConfigurationFilterDTOV1().key(ConfigurationFilterDTOV1.KeyEnum.APP_ID)
                                .value("test-wrong-provider")))
                .chatMessage(new ChatMessageDTOV1().message("Test"));

        given()
                .auth().oauth2(getKeycloakClientToken("testExtClient"))
                .contentType(APPLICATION_JSON)
                .body(request)
                .when()
                .post()
                .then().statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void chatRequestTest() {
        var request = new ChatRequestDTOV1()
                .chatMessage(new ChatMessageDTOV1().message("Hallo"));

        given()
                .auth().oauth2(getKeycloakClientToken("testExtClient"))
                .contentType(APPLICATION_JSON)
                .body(request)
                .when()
                .post()
                .then().statusCode(Response.Status.OK.getStatusCode());
    }
}
