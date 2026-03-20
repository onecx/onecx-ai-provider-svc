package org.tkit.onecx.ai.provider.rs.external.v1.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.tkit.quarkus.security.test.GenerateKeycloakClient;
import org.tkit.quarkus.test.WithDBData;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.*;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(DispatchRestV1Controller.class)
@WithDBData(value = "data/dispatch-with-mcp-v1.xml", deleteBeforeInsert = true, deleteAfterTest = true, rinseAndRepeat = true)
@GenerateKeycloakClient(clientName = DispatchRestV1ControllerChatMcpServerTest.CLIENT, scopes = { "ocx-ai-provider:read" })
class DispatchRestV1ControllerChatMcpServerTest extends AbstractConfigurationTest {

    static final String CLIENT = "testChatMcpServerClient";

    @Test
    void chatRequestWithMcpServers1Test() {

        var message = "Hallo";

        mcpCreatePing("mcp-test-with-mcp-server");
        mcpCreateInit("mcp-test-with-mcp-server");
        mcpCreateNotify("mcp-test-with-mcp-server");
        mcpCreateToolsList("mcp-test-with-mcp-server");
        mcpCreateToolsCall("mcp-test-with-mcp-server");

        ollamaCreateChat("provider-test-with-mcp-server", "model1",
                createUserMessage(message),
                createAssistantMessage("Hallo!?",
                        createTooCall(0, "does-not-exists", Map.of("name", "Hallo")),
                        createTooCall(1, "searchTest", Map.of("name", "Hallo"))));

        var request = new ChatRequestDTOV1()
                .requestContext(new RequestContextDTOV1().filter(
                        new ConfigurationFilterDTOV1().key(ConfigurationFilterDTOV1.KeyEnum.APP_ID)
                                .value("test-with-mcp-server")))
                .conversation(new ConversationDTOV1().conversationId("test-conversation-id")
                        .addHistoryItem(new ChatMessageDTOV1().message(message).type(ChatMessageDTOV1.TypeEnum.SYSTEM))
                        .addHistoryItem(new ChatMessageDTOV1().message(message).type(ChatMessageDTOV1.TypeEnum.ASSISTANT))
                        .addHistoryItem(new ChatMessageDTOV1().message(message).type(ChatMessageDTOV1.TypeEnum.USER))
                        .addHistoryItem(new ChatMessageDTOV1().message(message).type(ChatMessageDTOV1.TypeEnum.ACTION)))
                .chatMessage(new ChatMessageDTOV1().message(message).type(ChatMessageDTOV1.TypeEnum.USER));

        given()
                .auth().oauth2(getKeycloakClientToken(CLIENT))
                .contentType(APPLICATION_JSON)
                .body(request)
                .when()
                .post()
                .then().statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    void chatRequestWithMcpServersNoFilterKeyTest() {
        var request = new ChatRequestDTOV1()
                .requestContext(new RequestContextDTOV1().filter(
                        new ConfigurationFilterDTOV1()
                                .value("test-with-mcp-server")))
                .chatMessage(new ChatMessageDTOV1().message("Hallo").type(ChatMessageDTOV1.TypeEnum.USER));

        given()
                .auth().oauth2(getKeycloakClientToken(CLIENT))
                .contentType(APPLICATION_JSON)
                .body(request)
                .when()
                .post()
                .then().statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void chatRequestWithMcpServersNoFilterValueTest() {
        var request = new ChatRequestDTOV1()
                .requestContext(new RequestContextDTOV1().filter(
                        new ConfigurationFilterDTOV1().key(ConfigurationFilterDTOV1.KeyEnum.APP_ID)))
                .chatMessage(new ChatMessageDTOV1().message("Hallo").type(ChatMessageDTOV1.TypeEnum.USER));

        given()
                .auth().oauth2(getKeycloakClientToken(CLIENT))
                .contentType(APPLICATION_JSON)
                .body(request)
                .when()
                .post()
                .then().statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void chatRequestWithMcpServersNoFilterTest() {
        var request = new ChatRequestDTOV1()
                //                .requestContext(new RequestContextDTOV1().filter(new ConfigurationFilterDTOV1()))
                .chatMessage(new ChatMessageDTOV1().message("Hallo").type(ChatMessageDTOV1.TypeEnum.USER));

        given()
                .auth().oauth2(getKeycloakClientToken(CLIENT))
                .contentType(APPLICATION_JSON)
                .body(request)
                .when()
                .post()
                .then().statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void chatRequestWithMcpServersFilterRegexTest() {
        var request = new ChatRequestDTOV1()
                .requestContext(new RequestContextDTOV1().filter(
                        new ConfigurationFilterDTOV1().key(ConfigurationFilterDTOV1.KeyEnum.APP_ID)
                                .value("onecx-test-")))
                .chatMessage(new ChatMessageDTOV1().message("Hallo").type(ChatMessageDTOV1.TypeEnum.USER));

        given()
                .auth().oauth2(getKeycloakClientToken(CLIENT))
                .contentType(APPLICATION_JSON)
                .body(request)
                .when()
                .post()
                .then().statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    //    @Test
    //    void chatRequestWithMcpServersNoFilterTest() {
    //        var request = new ChatRequestDTOV1()
    //                .conversation(new ConversationDTOV1().conversationId("test-conversation-id"))
    //                .chatMessage(new ChatMessageDTOV1().message("Hallo").type(ChatMessageDTOV1.TypeEnum.USER));
    //
    //        given()
    //                .auth().oauth2(getKeycloakClientToken("testExtClient"))
    //                .contentType(APPLICATION_JSON)
    //                .body(request)
    //                .when()
    //                .post()
    //                .then().statusCode(Response.Status.OK.getStatusCode());
    //    }
}
