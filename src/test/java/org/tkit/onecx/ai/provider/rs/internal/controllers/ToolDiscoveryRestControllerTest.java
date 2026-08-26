package org.tkit.onecx.ai.provider.rs.internal.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.test.AbstractTest;
import org.tkit.quarkus.security.test.GenerateKeycloakClient;
import org.tkit.quarkus.test.WithDBData;

import gen.org.tkit.onecx.ai.provider.rs.internal.model.*;
import gen.org.tkit.onecx.ai.provider.runtime.client.api.RuntimeInternalApi;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.DiscoveredTool;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.DiscoveredToolAnnotations;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.ToolDiscoveryResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(ToolRestController.class)
@WithDBData(value = "data/testdata-internal.xml", deleteBeforeInsert = true, deleteAfterTest = true, rinseAndRepeat = true)
@GenerateKeycloakClient(clientName = "testClient", scopes = { "ocx-ai:all", "ocx-ai:read", "ocx-ai:write", "ocx-ai:delete" })
class ToolDiscoveryRestControllerTest extends AbstractTest {

    @InjectMock
    @RestClient
    RuntimeInternalApi providerRuntimeClient;

    @Test
    void getDiscoveredTools_toolNotFound_returnsNotFound() {
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .pathParam("toolId", "tool-not-exists")
                .queryParam("agentId", "agent-11-111")
                .post("/{toolId}/discovered-tools")
                .then().statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    void getDiscoveredTools_runtimeReturnsNonOk_returnsBadGateway() {
        when(providerRuntimeClient.discoverTools(any()))
                .thenReturn(Response.status(INTERNAL_SERVER_ERROR).build());

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .pathParam("toolId", "tool-11-111")
                .post("/{toolId}/discovered-tools")
                .then().statusCode(BAD_GATEWAY.getStatusCode());
    }

    @Test
    void getDiscoveredTools_returnsToolsWithAnnotationsAndRules() {
        var annotations = new DiscoveredToolAnnotations();
        annotations.setReadOnlyHint(true);
        annotations.setDestructiveHint(false);

        var discoveredTool = new DiscoveredTool();
        discoveredTool.setName("getProposal");
        discoveredTool.setDescription("Reads a proposal");
        discoveredTool.setAnnotations(annotations);

        var response = new ToolDiscoveryResponse();
        response.setTools(java.util.List.of(discoveredTool));

        when(providerRuntimeClient.discoverTools(any()))
                .thenReturn(Response.ok(response).build());

        var result = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .pathParam("toolId", "tool-11-111")
                .queryParam("agentId", "agent-11-111")
                .post("/{toolId}/discovered-tools")
                .then().statusCode(OK.getStatusCode())
                .extract().as(DiscoveredToolInfoListDTO.class);

        assertThat(result.getTools()).hasSize(2);
        var matched = result.getTools().stream().filter(t -> "getProposal".equals(t.getName())).findFirst().orElseThrow();
        assertThat(matched.getOrphaned()).isFalse();
        assertThat(matched.getAnnotations()).isNotNull();
        assertThat(matched.getAnnotations().getReadOnlyHint()).isTrue();
        assertThat(matched.getExistingRule()).isNotNull();
        assertThat(matched.getExistingRule().getToolName()).isEqualTo("getProposal");
        var orphaned = result.getTools().stream().filter(t -> "deleteProposal".equals(t.getName())).findFirst().orElseThrow();
        assertThat(orphaned.getOrphaned()).isTrue();
        assertThat(orphaned.getExistingRule()).isNotNull();
    }

    @Test
    void getDiscoveredTools_globalTool_returnsToolsWithoutAgentRules() {
        var discoveredTool = new DiscoveredTool();
        discoveredTool.setName("globalRead");
        discoveredTool.setDescription("Global read tool");

        var response = new ToolDiscoveryResponse();
        response.setTools(java.util.List.of(discoveredTool));

        when(providerRuntimeClient.discoverTools(any()))
                .thenReturn(Response.ok(response).build());

        var result = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .pathParam("toolId", "gtool-11-111")
                .post("/{toolId}/discovered-tools")
                .then().statusCode(OK.getStatusCode())
                .extract().as(DiscoveredToolInfoListDTO.class);

        assertThat(result.getTools()).hasSize(1);
        assertThat(result.getTools().get(0).getName()).isEqualTo("globalRead");
        assertThat(result.getTools().get(0).getOrphaned()).isFalse();
    }

    @Test
    void getDiscoveredTools_nullToolsInBody_noAgentId_returnsEmptyList() {
        var response = new ToolDiscoveryResponse();
        response.setTools(null);

        when(providerRuntimeClient.discoverTools(any()))
                .thenReturn(Response.ok(response).build());

        var result = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .pathParam("toolId", "tool-11-111")
                .post("/{toolId}/discovered-tools")
                .then().statusCode(OK.getStatusCode())
                .extract().as(DiscoveredToolInfoListDTO.class);

        assertThat(result.getTools()).isEmpty();
    }

    @Test
    void getDiscoveredTools_nullToolsInBody_withAgentId_returnsOrphanedRules() {
        var response = new ToolDiscoveryResponse();
        response.setTools(null);

        when(providerRuntimeClient.discoverTools(any()))
                .thenReturn(Response.ok(response).build());

        var result = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .pathParam("toolId", "tool-11-111")
                .queryParam("agentId", "agent-11-111")
                .post("/{toolId}/discovered-tools")
                .then().statusCode(OK.getStatusCode())
                .extract().as(DiscoveredToolInfoListDTO.class);

        assertThat(result.getTools()).hasSize(2);
        assertThat(result.getTools()).allSatisfy(t -> assertThat(t.getOrphaned()).isTrue());
    }

    @Test
    void getDiscoveredTools_toolWithoutAuthMode_returnsTools() {
        var discoveredTool = new DiscoveredTool();
        discoveredTool.setName("simpleTool");
        discoveredTool.setDescription("A simple tool");

        var response = new ToolDiscoveryResponse();
        response.setTools(java.util.List.of(discoveredTool));

        when(providerRuntimeClient.discoverTools(any()))
                .thenReturn(Response.ok(response).build());

        var result = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .pathParam("toolId", "tool-33-333")
                .queryParam("agentId", "")
                .post("/{toolId}/discovered-tools")
                .then().statusCode(OK.getStatusCode())
                .extract().as(DiscoveredToolInfoListDTO.class);

        assertThat(result.getTools()).hasSize(1);
        assertThat(result.getTools().get(0).getName()).isEqualTo("simpleTool");
    }

    @Test
    void getDiscoveredTools_discoveredToolWithoutAnnotations_coversLoopAndOrphanedFalse() {
        // Discovered tool without annotations → loop body executes, info.setOrphaned(false)
        var discoveredTool = new DiscoveredTool();
        discoveredTool.setName("simpleTool");
        discoveredTool.setDescription("A simple tool");

        var response = new ToolDiscoveryResponse();
        response.setTools(java.util.List.of(discoveredTool));

        when(providerRuntimeClient.discoverTools(any()))
                .thenReturn(Response.ok(response).build());

        var result = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .pathParam("toolId", "tool-33-333")
                .post("/{toolId}/discovered-tools")
                .then().statusCode(OK.getStatusCode())
                .extract().as(DiscoveredToolInfoListDTO.class);

        assertThat(result.getTools()).hasSize(1);
        assertThat(result.getTools().get(0).getName()).isEqualTo("simpleTool");
        assertThat(result.getTools().get(0).getOrphaned()).isFalse();
        assertThat(result.getTools().get(0).getAnnotations()).isNull();
    }
}
