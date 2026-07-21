package org.tkit.onecx.ai.provider.rs.internal.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.*;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.test.AbstractTest;
import org.tkit.quarkus.security.test.GenerateKeycloakClient;
import org.tkit.quarkus.test.WithDBData;

import gen.org.tkit.onecx.ai.provider.rs.internal.model.*;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(ScaffoldRestController.class)
@WithDBData(value = "data/testdata-internal.xml", deleteBeforeInsert = true, deleteAfterTest = true, rinseAndRepeat = true)
@GenerateKeycloakClient(clientName = "testClient", scopes = { "ocx-ai:all", "ocx-ai:read", "ocx-ai:write", "ocx-ai:delete" })
class ScaffoldRestControllerTest extends AbstractTest {

    @Test
    void createScaffoldTest() {
        var dto = new CreateScaffoldRequestDTO();
        dto.setName("scaffold-created");
        dto.setSkills(List.of(new SkillDTO().name("skill1").instruction("test")));

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract()
                .as(ScaffoldDTO.class);

        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("scaffold-created");

        //create with null skills list
        dto.setName("scaffold-created-2");
        dto.setSkills(null);

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract()
                .as(ScaffoldDTO.class);
    }

    @Test
    void createScaffoldWithEmptySkillListTest() {
        var dto = new CreateScaffoldRequestDTO();
        dto.setName("scaffold-created");
        dto.setSkills(List.of());

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract()
                .as(ScaffoldDTO.class);

        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("scaffold-created");

        dto.setName("scaffold-created-2");
        dto.setSkills(List.of(new SkillDTO().name("skill1").instruction("test1")));

        var created2 = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract()
                .as(ScaffoldDTO.class);

        assertThat(created2).isNotNull();
        assertThat(created2.getName()).isEqualTo("scaffold-created-2");

        dto.setName("scaffold-created-3");
        dto.setSkills(created2.getSkills());
        dto.addSkillsItem(new SkillDTO().name("skill2").instruction("test2").id("fakeId"));

        var created3 = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract()
                .as(ScaffoldDTO.class);

        assertThat(created3).isNotNull();
        assertThat(created3.getName()).isEqualTo("scaffold-created-3");
    }

    @Test
    void findScaffoldByCriteriaTest() {
        var criteria = new ScaffoldSearchCriteriaDTO();
        criteria.setPageNumber(0);
        criteria.setPageSize(10);
        criteria.setSourceProduct("onecx");

        var response = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(criteria)
                .post("/search")
                .then()
                .statusCode(OK.getStatusCode())
                .extract().as(ScaffoldPageResultDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findScaffoldByEmptyCriteriaTest() {
        var criteria = new ScaffoldSearchCriteriaDTO();
        criteria.setPageNumber(0);
        criteria.setPageSize(10);

        var response = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(criteria)
                .post("/search")
                .then()
                .statusCode(OK.getStatusCode())
                .extract().as(ScaffoldPageResultDTO.class);

        assertThat(response).isNotNull();
        assertThat(response.getTotalElements()).isEqualTo(2);
    }

    @Test
    void getScaffoldByIdTest() {
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .pathParam("id", "scaffold-none-exists-id")
                .get("/{id}")
                .then().statusCode(NOT_FOUND.getStatusCode());

        var dto = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .pathParam("id", "scaffold-11-111")
                .get("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(ScaffoldDTO.class);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo("scaffold-11-111");
        assertThat(dto.getName()).isEqualTo("scaffold1");
    }

    @Test
    void deleteScaffoldTest() {
        var create = new CreateScaffoldRequestDTO();
        create.setName("scaffold-delete");

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(create)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract().as(ScaffoldDTO.class);

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .pathParam("id", created.getId())
                .delete("/{id}")
                .then().statusCode(NO_CONTENT.getStatusCode());

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .pathParam("id", created.getId())
                .get("/{id}")
                .then().statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    void updateScaffoldByIdTest() {
        var dto = new UpdateScaffoldRequestDTO();
        dto.setName("scaffold-updated");
        dto.setSkills(List.of(new SkillDTO().name("skill1").instruction("test")));
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "does-not-exists")
                .put("/{id}")
                .then().statusCode(BAD_REQUEST.getStatusCode());

        dto.setModificationCount(0);

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "does-not-exists")
                .put("/{id}")
                .then().statusCode(NOT_FOUND.getStatusCode());

        var res = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "scaffold-11-111")
                .put("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(ScaffoldDTO.class);

        Assertions.assertThat(res.getSkills()).hasSize(1);

        // optimistic lock

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "scaffold-11-111")
                .put("/{id}")
                .then().statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    void updateScaffoldByIdWithNullSkillListTest() {
        var dto = new UpdateScaffoldRequestDTO();
        dto.setName("scaffold-updated");
        dto.setSkills(null);
        dto.setModificationCount(0);

        var res = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "scaffold-11-111")
                .put("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(ScaffoldDTO.class);

        Assertions.assertThat(res.getSkills()).isEmpty();
    }

    @Test
    void updateScaffoldWithEmptySkillListByIdTest() {
        var dto = new UpdateScaffoldRequestDTO();
        dto.setName("scaffold-updated");
        dto.setSkills(List.of());
        dto.setModificationCount(0);

        var res = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "scaffold-11-111")
                .put("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(ScaffoldDTO.class);

        Assertions.assertThat(res.getSkills()).isEmpty();

        dto.setSkills(res.getSkills());
        dto.setModificationCount(res.getModificationCount());
        var res2 = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "scaffold-11-111")
                .put("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(ScaffoldDTO.class);

        Assertions.assertThat(res2.getSkills()).isEmpty();

        //should ignore skill with non existing id
        dto.addSkillsItem(new SkillDTO().name("skill3").instruction("test3").id("fakeId"));
        dto.setModificationCount(res2.getModificationCount());
        var res3 = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "scaffold-11-111")
                .put("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(ScaffoldDTO.class);

        Assertions.assertThat(res3.getSkills()).isEmpty();
    }
}
