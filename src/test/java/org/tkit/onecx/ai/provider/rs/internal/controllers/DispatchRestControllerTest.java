package org.tkit.onecx.ai.provider.rs.internal.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;
import org.tkit.onecx.ai.provider.test.AbstractTest;
import org.tkit.quarkus.security.test.GenerateKeycloakClient;
import org.tkit.quarkus.test.WithDBData;

import gen.org.tkit.onecx.ai.provider.rs.internal.model.ChatMessageDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ChatRequestDTO;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.RuntimeChatResponse;
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(DispatchRestController.class)
@WithDBData(value = "data/testdata-internal.xml", deleteBeforeInsert = true, deleteAfterTest = true, rinseAndRepeat = true)
@GenerateKeycloakClient(clientName = "testClient", scopes = { "ocx-ai:all", "ocx-ai:read", "ocx-ai:write", "ocx-ai:delete" })
class DispatchRestControllerTest extends AbstractTest {

    @InjectMockServerClient
    MockServerClient mockServerClient;

    static final String MOCK_ID = "MOCK";

    @BeforeEach
    void resetExpectation() {
        try {
            mockServerClient.clear(MOCK_ID);
        } catch (Exception _) {
            //  mockId not existing
        }
    }

    @Test
    void chatTest() {
        var request = new ChatRequestDTO();
        var message = new ChatMessageDTO();
        message.setConversationId("conversation-1");
        message.setType(ChatMessageDTO.TypeEnum.USER);
        message.setMessage("hello");
        request.setChatMessage(message);

        var data = new RuntimeChatResponse().message("ok");

        mockServerClient.when(request().withPath("/ai/internal/runtime/chat").withMethod(HttpMethod.POST))
                .withId(MOCK_ID)
                .respond(httpRequest -> response().withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(data)));

        var response = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(request)
                .post()
                .then()
                .statusCode(OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract()
                .as(ChatMessageDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("ok");
        assertThat(response.getType()).isEqualTo(ChatMessageDTO.TypeEnum.ASSISTANT);
        assertThat(response.getConversationId()).isEqualTo("conversation-1");
    }
}
