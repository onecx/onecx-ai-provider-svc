package org.tkit.onecx.ai.provider.rs.external.v1.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.domain.daos.AgentConfigurationVersionDAO;
import org.tkit.onecx.ai.provider.domain.daos.AgentDAO;
import org.tkit.onecx.ai.provider.domain.models.AgentConfigurationVersion;
import org.tkit.onecx.ai.provider.test.AbstractTest;
import org.tkit.quarkus.security.test.GenerateKeycloakClient;
import org.tkit.quarkus.test.WithDBData;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.AgentConfigurationDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.AgentConfigurationVersionDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ProblemDetailResponseDTOV1;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(AgentRestV1Controller.class)
@WithDBData(value = "data/testdata-internal.xml", deleteBeforeInsert = true, deleteAfterTest = true, rinseAndRepeat = true)
@GenerateKeycloakClient(clientName = "versionEditor", scopes = { "ocx-ai:read", "ocx-ai:write" })
class AgentConfigurationVersionRestV1ControllerTest extends AbstractTest {

    @Inject
    AgentDAO agentDAO;

    @Inject
    AgentConfigurationVersionDAO versionDAO;

    @Test
    void configurationVersionsPersistAcrossDraftChangesAndActivation() {
        var first = createVersion("First");
        assertThat(first.getActive()).isTrue();

        QuarkusTransaction.requiringNew().run(() -> {
            var agent = agentDAO.findById("agent-11-111");
            agent.setName("Updated draft");
        });
        var second = createVersion("Second");
        assertThat(second.getActive()).isFalse();
        assertThat(second.getId()).isNotEqualTo(first.getId());

        assertThat(readConfiguration("/agent-11-111/configuration").getAgent().getName()).isEqualTo("agent1");
        given().auth().oauth2(getKeycloakClientToken("versionEditor"))
                .basePath("/v1/agents")
                .put("/agent-11-111/configuration/versions/" + second.getId() + "/activate")
                .then().statusCode(OK.getStatusCode());

        assertThat(readConfiguration("/agent-11-111/configuration").getAgent().getName()).isEqualTo("Updated draft");
        assertThat(readConfiguration("/agent-11-111/configuration/versions/" + first.getId()).getAgent().getName())
                .isEqualTo("agent1");

        given().auth().oauth2(getKeycloakClientToken("versionEditor"))
                .basePath("/v1/agents")
                .get("/agent-22-222/configuration/versions/" + first.getId())
                .then().statusCode(NOT_FOUND.getStatusCode());
        given().auth().oauth2(getKeycloakClientToken("versionEditor"))
                .basePath("/v1/agents")
                .put("/agent-22-222/configuration/versions/" + first.getId() + "/activate")
                .then().statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    void duplicateVersionNamesReturnConflictWithoutChangingActiveVersion() {
        var first = createVersion("Release 1");
        for (var name : List.of("Release 1", "  Release 1  ")) {
            var problem = given().auth().oauth2(getKeycloakClientToken("versionEditor"))
                    .basePath("/v1/agents").contentType(APPLICATION_JSON)
                    .body(Map.of("name", name))
                    .post("/agent-11-111/configuration/versions")
                    .then().statusCode(CONFLICT.getStatusCode()).extract()
                    .as(ProblemDetailResponseDTOV1.class);
            assertThat(problem.getErrorCode()).isEqualTo("DUPLICATE_AGENT_CONFIGURATION_VERSION_NAME");
        }
        var response = given().auth().oauth2(getKeycloakClientToken("versionEditor"))
                .basePath("/v1/agents").get("/agent-11-111/configuration")
                .then().statusCode(OK.getStatusCode()).extract().response();
        assertThat(response.header("Content-Location")).endsWith("/" + first.getId());
        assertThat(response.as(AgentConfigurationDTOV1.class).getAgent().getName()).isEqualTo("agent1");
    }

    @Test
    void namesAreCaseSensitiveAndCanBeReusedByOtherAgents() {
        var first = createVersion("  Release 1  ");
        assertThat(first.getName()).isEqualTo("Release 1");
        var differentCase = createVersion("release 1");
        assertThat(differentCase.getId()).isNotEqualTo(first.getId());
        assertThat(differentCase.getActive()).isFalse();

        var otherAgentVersion = given().auth().oauth2(getKeycloakClientToken("versionEditor"))
                .basePath("/v1/agents").contentType(APPLICATION_JSON)
                .body(Map.of("name", "Release 1"))
                .post("/agent-22-222/configuration/versions")
                .then().statusCode(CREATED.getStatusCode()).extract().as(AgentConfigurationVersionDTOV1.class);
        assertThat(otherAgentVersion.getId()).isNotEqualTo(first.getId());
        assertThat(otherAgentVersion.getActive()).isTrue();
    }

    @Test
    void databaseRejectsDuplicateNamesEvenWhenBypassingController() {
        createVersion("Release 1");
        assertThatThrownBy(() -> QuarkusTransaction.requiringNew().run(() -> {
            var agent = agentDAO.findById("agent-11-111");
            versionDAO.create(new AgentConfigurationVersion(agent, "Release 1", "{}"));
        })).hasStackTraceContaining("uq_agent_configuration_version_name");
    }

    @Test
    void invalidVersionNamesAndMissingAgentsAreRejected() {
        for (var name : List.of("", "  ", "a".repeat(256))) {
            given().auth().oauth2(getKeycloakClientToken("versionEditor"))
                    .basePath("/v1/agents").contentType(APPLICATION_JSON)
                    .body(Map.of("name", name))
                    .post("/agent-11-111/configuration/versions")
                    .then().statusCode(BAD_REQUEST.getStatusCode());
        }
        given().auth().oauth2(getKeycloakClientToken("versionEditor"))
                .basePath("/v1/agents").contentType(APPLICATION_JSON)
                .body(Map.of("name", "Missing"))
                .post("/missing/configuration/versions")
                .then().statusCode(NOT_FOUND.getStatusCode());
    }

    private AgentConfigurationVersionDTOV1 createVersion(String name) {
        var response = given().auth().oauth2(getKeycloakClientToken("versionEditor"))
                .basePath("/v1/agents").contentType(APPLICATION_JSON)
                .body(Map.of("name", name))
                .post("/agent-11-111/configuration/versions")
                .then().statusCode(CREATED.getStatusCode()).extract().response();
        var version = response.as(AgentConfigurationVersionDTOV1.class);
        assertThat(response.header("Location")).endsWith("/configuration/versions/" + version.getId());
        return version;
    }

    private AgentConfigurationDTOV1 readConfiguration(String path) {
        return given().auth().oauth2(getKeycloakClientToken("versionEditor"))
                .basePath("/v1/agents").get(path)
                .then().statusCode(OK.getStatusCode()).extract().as(AgentConfigurationDTOV1.class);
    }
}