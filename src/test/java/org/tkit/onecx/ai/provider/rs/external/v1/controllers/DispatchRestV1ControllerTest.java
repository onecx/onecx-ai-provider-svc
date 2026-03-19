package org.tkit.onecx.ai.provider.rs.external.v1.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

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
                .chatMessage(new ChatMessageDTOV1().message("Test").type(ChatMessageDTOV1.TypeEnum.USER));

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
                .chatMessage(new ChatMessageDTOV1().message("Test").type(ChatMessageDTOV1.TypeEnum.USER));

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
                .chatMessage(new ChatMessageDTOV1().message("Test").type(ChatMessageDTOV1.TypeEnum.USER));

        given()
                .auth().oauth2(getKeycloakClientToken("testExtClient"))
                .contentType(APPLICATION_JSON)
                .body(request)
                .when()
                .post()
                .then().statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void chatRequestNoMessageTest() {
        var request = new ChatRequestDTOV1()
                .requestContext(new RequestContextDTOV1().filter(
                        new ConfigurationFilterDTOV1().key(ConfigurationFilterDTOV1.KeyEnum.APP_ID)
                                .value("test-wrong-provider")))
                .chatMessage(null);

        given()
                .auth().oauth2(getKeycloakClientToken("testExtClient"))
                .contentType(APPLICATION_JSON)
                .body(request)
                .when()
                .post()
                .then().statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void chatRequestTest() {
        var request = new ChatRequestDTOV1()
                .chatMessage(new ChatMessageDTOV1().message("Hallo").type(ChatMessageDTOV1.TypeEnum.USER));

        given()
                .auth().oauth2(getKeycloakClientToken("testExtClient"))
                .contentType(APPLICATION_JSON)
                .body(request)
                .when()
                .post()
                .then().statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    void chatRequestWithMcpServers0Test() {
        var request = new ChatRequestDTOV1()
                .requestContext(new RequestContextDTOV1().filter(
                        new ConfigurationFilterDTOV1().key(ConfigurationFilterDTOV1.KeyEnum.APP_ID)
                                .value("test-with-mcp-servers-0")))
                .chatMessage(new ChatMessageDTOV1().message("Hallo").type(ChatMessageDTOV1.TypeEnum.USER));

        given()
                .auth().oauth2(getKeycloakClientToken("testExtClient"))
                .contentType(APPLICATION_JSON)
                .body(request)
                .when()
                .post()
                .then().statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void chatRequestWithMcpServersNoFilterTest() {
        var request = new ChatRequestDTOV1()
                .conversation(new ConversationDTOV1().conversationId("test-conversation-id"))
                .chatMessage(new ChatMessageDTOV1().message("Hallo").type(ChatMessageDTOV1.TypeEnum.USER));

        given()
                .auth().oauth2(getKeycloakClientToken("testExtClient"))
                .contentType(APPLICATION_JSON)
                .body(request)
                .when()
                .post()
                .then().statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    void chatRequestWithMcpServers1Test() {
        var request = new ChatRequestDTOV1()
                .requestContext(new RequestContextDTOV1().filter(
                        new ConfigurationFilterDTOV1().key(ConfigurationFilterDTOV1.KeyEnum.APP_ID)
                                .value("test-with-mcp-servers-1")))
                .conversation(new ConversationDTOV1().conversationId("test-conversation-id")
                        .addHistoryItem(new ChatMessageDTOV1().message("Hallo").type(ChatMessageDTOV1.TypeEnum.SYSTEM))
                        .addHistoryItem(new ChatMessageDTOV1().message("Hallo").type(ChatMessageDTOV1.TypeEnum.ASSISTANT))
                        .addHistoryItem(new ChatMessageDTOV1().message("Hallo").type(ChatMessageDTOV1.TypeEnum.USER))
                        .addHistoryItem(new ChatMessageDTOV1().message("Hallo").type(ChatMessageDTOV1.TypeEnum.ACTION)))
                .chatMessage(new ChatMessageDTOV1().message("Hallo").type(ChatMessageDTOV1.TypeEnum.USER));

        given()
                .auth().oauth2(getKeycloakClientToken("testExtClient"))
                .contentType(APPLICATION_JSON)
                .body(request)
                .when()
                .post()
                .then().statusCode(Response.Status.OK.getStatusCode());
    }
}
