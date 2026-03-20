package org.tkit.onecx.ai.provider.rs.external.v1.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.tkit.onecx.ai.provider.rs.external.v1.controllers.DispatchRestV1ControllerWithMcpServerErrorTest.CLIENT;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.tkit.quarkus.security.test.GenerateKeycloakClient;
import org.tkit.quarkus.test.WithDBData;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.*;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(DispatchRestV1Controller.class)
@WithDBData(value = "data/dispatch-with-mcp-error-v1.xml", deleteBeforeInsert = true, deleteAfterTest = true, rinseAndRepeat = true)
@GenerateKeycloakClient(clientName = CLIENT, scopes = { "ocx-ai-provider:read" })
class DispatchRestV1ControllerWithMcpServerErrorTest extends AbstractConfigurationTest {

    static final String CLIENT = "testExtWithMcpClient";

    @Test
    void chatRequestWithMcpServerTest() {

        mcpCreatePing("mcp-test-with-mcp");
        mcpCreateInit("mcp-test-with-mcp");
        mcpCreateNotify("mcp-test-with-mcp");
        mcpCreateToolsListResponse("mcp-test-with-mcp", Response.Status.BAD_GATEWAY);

        var request = new ChatRequestDTOV1()
                .requestContext(new RequestContextDTOV1().filter(
                        new ConfigurationFilterDTOV1().key(ConfigurationFilterDTOV1.KeyEnum.APP_ID)
                                .value("test-with-mcp-servers")))
                .chatMessage(new ChatMessageDTOV1().message("Hallo").type(ChatMessageDTOV1.TypeEnum.USER));

        given()
                .auth().oauth2(getKeycloakClientToken(CLIENT))
                .contentType(APPLICATION_JSON)
                .body(request)
                .when()
                .post()
                .then().statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

}
