package org.tkit.onecx.ai.provider.rs.internal.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.common.services.llm.LlmServiceFactory;
import org.tkit.onecx.ai.provider.test.AbstractTest;
import org.tkit.quarkus.security.test.GenerateKeycloakClient;
import org.tkit.quarkus.test.WithDBData;

import gen.org.tkit.onecx.ai.provider.rs.internal.model.ProviderHealthStatusDTO;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(ProviderRestController.class)
@WithDBData(value = "data/testdata-internal.xml", deleteBeforeInsert = true, deleteAfterTest = true, rinseAndRepeat = true)
@GenerateKeycloakClient(clientName = "testClient", scopes = { "ocx-ai:all", "ocx-ai:read", "ocx-ai:write", "ocx-ai:delete" })
public class ProviderHealthCheckTest extends AbstractTest {
    @InjectMock
    LlmServiceFactory llmServiceFactory;

    @Test
    void getProviderHealthStatusTest() {
        when(llmServiceFactory.getProviderHealthStatus(org.mockito.ArgumentMatchers.any())).thenReturn("HEALTHY");

        var dto = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .when()
                .pathParam("id", "provider-11-111")
                .get("/{id}/health")
                .then().statusCode(OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract()
                .body().as(ProviderHealthStatusDTO.class);

        assertThat(dto).isNotNull();
        assertThat(dto.getStatus()).isEqualTo(ProviderHealthStatusDTO.StatusEnum.HEALTHY);
        assertThat(dto.getCheckedAt()).isNotNull();
    }

    @Test
    void getProviderHealthStatusNotFoundTest() {
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .when()
                .pathParam("id", "provider-none-exists-id")
                .get("/{id}/health")
                .then().statusCode(NOT_FOUND.getStatusCode());

        verify(llmServiceFactory, never()).getProviderHealthStatus(org.mockito.ArgumentMatchers.any());
    }
}
