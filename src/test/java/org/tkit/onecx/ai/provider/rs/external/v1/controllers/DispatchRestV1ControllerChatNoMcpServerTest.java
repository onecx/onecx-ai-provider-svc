package org.tkit.onecx.ai.provider.rs.external.v1.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.tkit.quarkus.security.test.GenerateKeycloakClient;
import org.tkit.quarkus.test.WithDBData;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.*;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(DispatchRestV1Controller.class)
@WithDBData(value = "data/dispatch-no-mcp-v1.xml", deleteBeforeInsert = true, deleteAfterTest = true, rinseAndRepeat = true)
@GenerateKeycloakClient(clientName = DispatchRestV1ControllerChatNoMcpServerTest.CLIENT, scopes = { "ocx-ai-provider:read" })
class DispatchRestV1ControllerChatNoMcpServerTest extends AbstractConfigurationTest {

    static final String CLIENT = "testExtNoMcpServerClient";

    @Test
    void chatRequestProviderNoMCPServerTest() {

        var message = "Hallo";
        ollamaCreateChat("test-chat-no-mcp-server", "test-chat-no-mcp-server", "Hallo! Wie kann ich dir helfen?");

        var request = new ChatRequestDTOV1()
                .requestContext(new RequestContextDTOV1().filter(
                        new ConfigurationFilterDTOV1().key(ConfigurationFilterDTOV1.KeyEnum.APP_ID)
                                .value("test-chat-no-mcp-server")))
                .conversation(new ConversationDTOV1().conversationId("test-conversation-id"))
                .chatMessage(new ChatMessageDTOV1().message(message).type(ChatMessageDTOV1.TypeEnum.USER));

        given()
                .auth().oauth2(getKeycloakClientToken(CLIENT))
                .contentType(APPLICATION_JSON)
                .body(request)
                .when()
                .post()
                .then()
                .log().all()
                .statusCode(Response.Status.OK.getStatusCode());
    }
}
