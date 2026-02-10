package org.tkit.onecx.ai.provider.rs.internal.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.*;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.test.AbstractTest;
import org.tkit.quarkus.security.test.GenerateKeycloakClient;
import org.tkit.quarkus.test.WithDBData;

import gen.org.tkit.onecx.ai.provider.rs.internal.model.*;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(MCPServerRestController.class)
@WithDBData(value = "data/testdata-internal.xml", deleteBeforeInsert = true, deleteAfterTest = true, rinseAndRepeat = true)
@GenerateKeycloakClient(clientName = "testClient", scopes = { "ocx-ai:all", "ocx-ai:read", "ocx-ai:write", "ocx-ai:delete" })
class MCPServerRestControllerTest extends AbstractTest {

    @Test
    void createMCPServerTest() {
        // create mcpServer
        var mcpServerDto = new CreateMCPServerRequestDTO();
        mcpServerDto.setName("MCPServer");
        mcpServerDto.setUrl("someUrl");
        mcpServerDto.setApiKey("someAPIkey");

        var dto = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .when()
                .contentType(APPLICATION_JSON)
                .body(mcpServerDto)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract()
                .body().as(MCPServerDTO.class);

        assertThat(dto).isNotNull();
        assertThat(dto.getName()).isEqualTo(mcpServerDto.getName());
        assertThat(dto.getUrl()).isEqualTo(mcpServerDto.getUrl());
    }

    @Test
    void findMCPServerBySearchCriteriaTest() {
        var criteria = new MCPServerSearchCriteriaDTO();

        var data = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(criteria)
                .post("/search")
                .then()
                .statusCode(OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract()
                .as(MCPServerPageResultDTO.class);

        assertThat(data).isNotNull();
        assertThat(data.getTotalElements()).isEqualTo(3);
        assertThat(data.getStream()).isNotNull().hasSize(3);

        criteria.setName("mcpServer2");
        data = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(criteria)
                .post("/search")
                .then()
                .statusCode(OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract()
                .as(MCPServerPageResultDTO.class);

        assertThat(data).isNotNull();
        assertThat(data.getTotalElements()).isEqualTo(1);
        assertThat(data.getStream()).isNotNull().hasSize(1);
    }

    @Test
    void getMCPServerByIdTest() {
        //mcpServer none exists
        given().contentType(APPLICATION_JSON)
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .when()
                .pathParam("id", "mcpServer-none-exists-id")
                .get("/{id}")
                .then().statusCode(NOT_FOUND.getStatusCode());

        //mcpServer exists
        var dto = given().contentType(APPLICATION_JSON)
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .when()
                .pathParam("id", "mcp-server-11-111")
                .get("/{id}")
                .then().statusCode(OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract()
                .body().as(MCPServerDTO.class);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo("mcp-server-11-111");
        assertThat(dto.getName()).isEqualTo("mcpServer1");
        assertThat(dto.getDescription()).isEqualTo("mcp_server_description_1");
        assertThat(dto.getUrl()).isEqualTo("http://mcp.server.org");
    }

    @Test
    void deleteMCPServerTest() {
        //get mcpServer and check if exists
        var dto = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .when()
                .pathParam("id", "mcp-server-DELETE_1")
                .get("/{id}")
                .then().statusCode(OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract().as(MCPServerDTO.class);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo("mcp-server-DELETE_1");

        //delete mcpServer
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .pathParam("id", "mcp-server-DELETE_1")
                .delete("/{id}")
                .then().statusCode(NO_CONTENT.getStatusCode());

        //check if mcpServer exists
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .pathParam("id", "mcp-server-DELETE_1")
                .get("/{id}")
                .then().statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    void updateMCPServerTest() {

        var mcpServerDto = new UpdateMCPServerRequestDTO();
        mcpServerDto.setName("updated-MCPServer");

        //update with missing modificationCount => constraint exception
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(mcpServerDto)
                .when()
                .pathParam("id", "does-not-exists")
                .put("/{id}")
                .then().statusCode(BAD_REQUEST.getStatusCode());

        mcpServerDto.setModificationCount(0);

        //update none existing mcpServer
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(mcpServerDto)
                .when()
                .pathParam("id", "does-not-exists")
                .put("/{id}")
                .then().statusCode(NOT_FOUND.getStatusCode());

        //update mcpServer
        var dto = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(mcpServerDto)
                .when()
                .pathParam("id", "mcp-server-11-111")
                .put("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(MCPServerDTO.class);
        assertThat(dto.getModificationCount()).isNotEqualTo(mcpServerDto.getModificationCount());
        assertThat(dto).isNotNull();
        assertThat(dto.getName()).isEqualTo(mcpServerDto.getName());

        //update with same modificationCount => optimistic lock exception
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(mcpServerDto)
                .when()
                .pathParam("id", "mcp-server-11-111")
                .put("/{id}")
                .then().statusCode(BAD_REQUEST.getStatusCode());
    }
}
