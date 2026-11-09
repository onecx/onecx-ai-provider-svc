package org.tkit.onecx.ai.provider.rs.internal.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.*;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.test.AbstractTest;
import org.tkit.quarkus.security.test.GenerateKeycloakClient;
import org.tkit.quarkus.test.WithDBData;

import gen.org.tkit.onecx.ai.provider.rs.internal.model.*;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(ModelRestController.class)
@WithDBData(value = "data/testdata-internal.xml", deleteBeforeInsert = true, rinseAndRepeat = true)
@GenerateKeycloakClient(clientName = "testClient", scopes = { "ocx-ai:all", "ocx-ai:read", "ocx-ai:write", "ocx-ai:delete" })
class ModelRestControllerTest extends AbstractTest {

    private static final String OPENAI_PROVIDER_ID = "provider-11-111";
    private static final String ANTHROPIC_PROVIDER_ID = "provider-22-222";

    @Test
    void createModelTest() {
        var dto = new CreateModelRequestDTO();
        dto.setName("model-created");
        dto.setCommunicationMode(CommunicationModeDTO.SYNC);

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract()
                .as(ModelDTO.class);

        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("model-created");
        assertThat(created.getCommunicationMode()).isEqualTo(CommunicationModeDTO.SYNC);
    }

    @Test
    void createOpenAiModelWithConfigRoundTripsUnchanged() {
        var config = "{\"api\":\"responses\",\"reasoningEffort\":\"medium\"}";
        var dto = openAiCreateRequest(config);

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract()
                .as(ModelDTO.class);

        assertThat(created.getModelConfig()).isEqualTo(config);
    }

    @Test
    void createOpenAiModelAcceptsBlankReasoningEffort() {
        var config = "{\"api\":\"chat-completions\",\"reasoningEffort\":\"  \"}";
        var dto = openAiCreateRequest(config);

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract()
                .as(ModelDTO.class);

        assertThat(created.getModelConfig()).isEqualTo(config);
    }

    @Test
    void createOpenAiModelAcceptsBlankConfigAsUnset() {
        var dto = openAiCreateRequest("");

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract()
                .as(ModelDTO.class);

        assertThat(created.getModelConfig()).isEmpty();
    }

    @Test
    void createOpenAiModelAllowsOmittedApiSetting() {
        var config = "{\"reasoningEffort\":\"medium\"}";
        var dto = openAiCreateRequest(config);

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract()
                .as(ModelDTO.class);

        assertThat(created.getModelConfig()).isEqualTo(config);
    }

    @Test
    void createOpenAiModelRejectsUnsupportedConfigsWithProblemDetail() {
        var invalidConfigs = new String[] {
                "{",
                "[]",
                "{\"api\":\"chat-completions\",\"unexpected\":true}",
                "{\"api\":\"legacy\"}",
                "{\"api\":1}",
                "{\"reasoningEffort\":true}",
                "x".repeat(256)
        };

        for (var config : invalidConfigs) {
            var response = given()
                    .auth().oauth2(getKeycloakClientToken("testClient"))
                    .contentType(APPLICATION_JSON)
                    .body(openAiCreateRequest(config))
                    .post()
                    .then()
                    .statusCode(BAD_REQUEST.getStatusCode())
                    .extract()
                    .as(ProblemDetailResponseDTO.class);

            assertThat(response.getErrorCode()).isEqualTo("INVALID_MODEL_CONFIG");
            assertThat(response.getDetail()).isNotBlank();
        }
    }

    @Test
    void createNonOpenAiModelKeepsGenericConfigUnvalidated() {
        var config = "legacy-provider-settings";
        var dto = openAiCreateRequest(config);
        dto.setProvider(provider(ANTHROPIC_PROVIDER_ID));

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract()
                .as(ModelDTO.class);

        assertThat(created.getModelConfig()).isEqualTo(config);
    }

    @Test
    void findModelByCriteriaTest() {
        var criteria = new ModelSearchCriteriaDTO();
        criteria.setPageNumber(0);
        criteria.setPageSize(10);

        var data = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(criteria)
                .post("/search")
                .then()
                .statusCode(OK.getStatusCode())
                .extract()
                .as(ModelPageResultDTO.class);

        assertThat(data).isNotNull();
        assertThat(data.getTotalElements()).isEqualTo(2);
        assertThat(data.getStream()).isNotNull().hasSize(2);

        criteria.setName("model1");
        criteria.setCommunicationMode(CommunicationModeDTO.SYNC);
        data = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(criteria)
                .post("/search")
                .then()
                .statusCode(OK.getStatusCode())
                .extract()
                .as(ModelPageResultDTO.class);

        assertThat(data.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getModelByIdTest() {
        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .pathParam("id", "model-none-exists-id")
                .get("/{id}")
                .then().statusCode(NOT_FOUND.getStatusCode());

        var dto = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .pathParam("id", "model-11-111")
                .get("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(ModelDTO.class);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo("model-11-111");
        assertThat(dto.getName()).isEqualTo("model1");
    }

    @Test
    void deleteModelTest() {
        var create = new CreateModelRequestDTO();
        create.setName("model-delete");

        var created = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(create)
                .post()
                .then()
                .statusCode(CREATED.getStatusCode())
                .extract().as(ModelDTO.class);

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
    void updateModelByIdTest() {
        var dto = new UpdateModelRequestDTO();
        dto.setName("model-updated");

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "does-not-exists")
                .put("/{id}")
                .then().statusCode(BAD_REQUEST.getStatusCode());

        dto.setModificationCount(0);
        dto.setCommunicationMode(CommunicationModeDTO.ASYNC);

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "does-not-exists")
                .put("/{id}")
                .then().statusCode(NOT_FOUND.getStatusCode());

        var updated = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "model-11-111")
                .put("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(ModelDTO.class);

        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo("model-updated");
        assertThat(updated.getModificationCount()).isNotEqualTo(dto.getModificationCount());
        assertThat(updated.getModelConfig()).isEqualTo("temperature=0.2");

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .pathParam("id", "model-11-111")
                .put("/{id}")
                .then().statusCode(BAD_REQUEST.getStatusCode());
    }

    @Test
    void updateOpenAiModelValidatesChangedConfigAndPreservesLegacyWhenOmitted() {
        var unchanged = new UpdateModelRequestDTO();
        unchanged.setModificationCount(0);
        unchanged.setName("editable-legacy-model");

        var updated = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(unchanged)
                .pathParam("id", "model-11-111")
                .put("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(ModelDTO.class);
        assertThat(updated.getModelConfig()).isEqualTo("temperature=0.2");

        var invalid = new UpdateModelRequestDTO();
        invalid.setModificationCount(updated.getModificationCount());
        invalid.setModelConfig("{\"api\":\"unsupported\"}");
        invalid.setProvider(provider(OPENAI_PROVIDER_ID));

        var error = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(invalid)
                .pathParam("id", "model-11-111")
                .put("/{id}")
                .then().statusCode(BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);
        assertThat(error.getErrorCode()).isEqualTo("INVALID_MODEL_CONFIG");
    }

    @Test
    void updateOpenAiModelPreservesLegacyConfigWhenExplicitlyNull() {
        var updated = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body("{\"modificationCount\":0,\"name\":\"legacy-null-update\",\"modelConfig\":null}")
                .pathParam("id", "model-11-111")
                .put("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(ModelDTO.class);

        assertThat(updated.getModelConfig()).isEqualTo("temperature=0.2");
    }

    @Test
    void updateNonOpenAiModelKeepsGenericConfigUnvalidated() {
        var update = new UpdateModelRequestDTO();
        update.setModificationCount(0);
        update.setModelConfig("legacy-provider-settings");

        var updated = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(update)
                .pathParam("id", "model-22-222")
                .put("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(ModelDTO.class);

        assertThat(updated.getModelConfig()).isEqualTo("legacy-provider-settings");
    }

    @Test
    void updateOpenAiModelPersistsAndReturnsValidChangedConfig() {
        var config = "{\"api\":\"responses\",\"reasoningEffort\":\"low\"}";
        var update = new UpdateModelRequestDTO();
        update.setModificationCount(0);
        update.setProvider(provider(OPENAI_PROVIDER_ID));
        update.setModelConfig(config);

        var updated = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(update)
                .pathParam("id", "model-11-111")
                .put("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(ModelDTO.class);

        assertThat(updated.getModelConfig()).isEqualTo(config);
    }

    private CreateModelRequestDTO openAiCreateRequest(String modelConfig) {
        var dto = new CreateModelRequestDTO();
        dto.setName("openai-model-config-test");
        dto.setModelIdentifier("gpt-test");
        dto.setModelConfig(modelConfig);
        dto.setCommunicationMode(CommunicationModeDTO.SYNC);
        dto.setProvider(provider(OPENAI_PROVIDER_ID));
        return dto;
    }

    private ProviderDTO provider(String id) {
        var provider = new ProviderDTO();
        provider.setId(id);
        provider.setName("test-provider");
        return provider;
    }
}
