package org.tkit.onecx.ai.provider.rs.internal.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.*;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"api\":\"responses\",\"reasoningEffort\":\"medium\"}",
            "{\"api\":\"chat-completions\",\"reasoningEffort\":\"  \"}",
            "{\"reasoningEffort\":\"medium\"}",
            "{}",
            ""
    })
    void createOpenAiModelWithConfigRoundTripsUnchanged(String config) {
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

    static Stream<String> unsupportedOpenAiConfigs() {
        return Stream.of(
                "{",
                "[]",
                "null",
                "{\"api\":\"chat-completions\",\"unexpected\":true}",
                "{\"api\":\"legacy\"}",
                "{\"api\":1}",
                "{\"reasoningEffort\":true}",
                "{\"api\":\"chat-completions\",\"api\":\"responses\",\"reasoningEffort\":\"medium\"}",
                "{\"api\":\"chat-completions\",\"\\u0061pi\":\"responses\"}",
                "{\"reasoningEffort\":\"low\",\"reasoningEffort\":\"medium\"}",
                "{\"reasoningEffort\":\"low\",\"reasoning\\u0045ffort\":\"medium\"}",
                "x".repeat(256));
    }

    @ParameterizedTest
    @MethodSource("unsupportedOpenAiConfigs")
    void createOpenAiModelRejectsUnsupportedConfigsWithProblemDetail(String config) {
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

    static Stream<String> opaqueConfigs() {
        return Stream.of("legacy-provider-settings", "{\"api\":\"chat-completions\",\"api\":\"responses\"}",
                "x".repeat(255));
    }

    @ParameterizedTest
    @MethodSource("opaqueConfigs")
    void createNonOpenAiModelKeepsGenericConfigUnvalidated(String config) {
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
        assertThat(updated.getProvider().getId()).isEqualTo(OPENAI_PROVIDER_ID);

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
        assertThat(updated.getProvider()).isNotNull();
        assertThat(updated.getProvider().getId()).isEqualTo(OPENAI_PROVIDER_ID);
        var stored = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .pathParam("id", "model-11-111")
                .get("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(ModelDTO.class);
        assertThat(stored.getProvider().getId()).isEqualTo(OPENAI_PROVIDER_ID);

        var invalid = new UpdateModelRequestDTO();
        invalid.setModificationCount(updated.getModificationCount());
        invalid.setModelConfig("{\"api\":\"unsupported\"}");

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

    @ParameterizedTest
    @ValueSource(strings = { ANTHROPIC_PROVIDER_ID, "", "provider-does-not-exist" })
    void createModelRejectsOversizeConfigForAnyProvider(String providerId) {
        var dto = openAiCreateRequest("x".repeat(256));
        dto.setProvider(providerId.isEmpty() ? null : provider(providerId));

        var error = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(dto)
                .post()
                .then().statusCode(BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo("INVALID_MODEL_CONFIG");
        assertThat(error.getDetail()).contains("255");
    }

    @Test
    void updateNonOpenAiModelRejectsOversizeConfig() {
        var update = new UpdateModelRequestDTO();
        update.setModificationCount(0);
        update.setModelConfig("x".repeat(256));

        var error = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(update)
                .pathParam("id", "model-22-222")
                .put("/{id}")
                .then().statusCode(BAD_REQUEST.getStatusCode())
                .extract().as(ProblemDetailResponseDTO.class);

        assertThat(error.getErrorCode()).isEqualTo("INVALID_MODEL_CONFIG");
        assertThat(error.getDetail()).contains("255");
    }

    @ParameterizedTest
    @CsvSource({
            "model-22-222, provider-11-111, 400",
            "model-11-111, provider-22-222, 200"
    })
    void updateModelValidatesConfigAgainstNewProvider(String modelId, String providerId, int status) {
        var update = new UpdateModelRequestDTO();
        update.setModificationCount(0);
        update.setProvider(provider(providerId));
        update.setModelConfig("opaque-provider-config");

        var response = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(update)
                .pathParam("id", modelId)
                .put("/{id}")
                .then().statusCode(status);

        if (status == BAD_REQUEST.getStatusCode()) {
            assertThat(response.extract().as(ProblemDetailResponseDTO.class).getErrorCode())
                    .isEqualTo("INVALID_MODEL_CONFIG");
        } else {
            var stored = given()
                    .auth().oauth2(getKeycloakClientToken("testClient"))
                    .pathParam("id", modelId)
                    .get("/{id}")
                    .then().statusCode(OK.getStatusCode())
                    .extract().as(ModelDTO.class);
            assertThat(stored.getProvider().getId()).isEqualTo(providerId);
            assertThat(stored.getModelConfig()).isEqualTo("opaque-provider-config");
        }
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

    @ParameterizedTest
    @MethodSource("opaqueConfigs")
    void updateNonOpenAiModelKeepsGenericConfigUnvalidated(String config) {
        var update = new UpdateModelRequestDTO();
        update.setModificationCount(0);
        update.setModelConfig(config);

        var updated = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(update)
                .pathParam("id", "model-22-222")
                .put("/{id}")
                .then().statusCode(OK.getStatusCode())
                .extract().as(ModelDTO.class);

        assertThat(updated.getModelConfig()).isEqualTo(config);
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

    @Test
    void updateOpenAiModelRejectsDuplicateKeysWithoutChangingStoredModel() {
        for (var config : new String[] {
                "{\"api\":\"chat-completions\",\"api\":\"responses\",\"reasoningEffort\":\"medium\"}",
                "{\"api\":\"chat-completions\",\"\\u0061pi\":\"responses\"}",
                "{\"reasoningEffort\":\"low\",\"reasoningEffort\":\"medium\"}",
                "{\"reasoningEffort\":\"low\",\"reasoning\\u0045ffort\":\"medium\"}"
        }) {
            var update = new UpdateModelRequestDTO();
            update.setModificationCount(0);
            update.setProvider(provider(OPENAI_PROVIDER_ID));
            update.setCommunicationMode(CommunicationModeDTO.SYNC);
            update.setName("must-not-be-saved");
            update.setModelConfig(config);

            var error = given()
                    .auth().oauth2(getKeycloakClientToken("testClient"))
                    .contentType(APPLICATION_JSON)
                    .body(update)
                    .put("/model-11-111")
                    .then().statusCode(BAD_REQUEST.getStatusCode())
                    .extract().as(ProblemDetailResponseDTO.class);
            assertThat(error.getErrorCode()).isEqualTo("INVALID_MODEL_CONFIG");
        }
        var stored = getModel("model-11-111");
        assertThat(stored.getName()).isEqualTo("model1");
        assertThat(stored.getModificationCount()).isZero();
        assertThat(stored.getModelConfig()).isEqualTo("temperature=0.2");
        assertThat(stored.getProvider().getId()).isEqualTo(OPENAI_PROVIDER_ID);
        assertThat(stored.getCommunicationMode()).isEqualTo(CommunicationModeDTO.SYNC);
    }

    @Test
    void fixedUiUpdatePreservesProviderCommunicationModeAndProviderListMembership() {
        assertFixedUiUpdate("model-11-111", OPENAI_PROVIDER_ID, "{\"api\":\"responses\",\"reasoningEffort\":\"medium\"}");
        assertFixedUiUpdate("model-22-222", ANTHROPIC_PROVIDER_ID, "temperature=0.6");
    }

    private void assertFixedUiUpdate(String modelId, String providerId, String config) {
        var original = getModel(modelId);
        // The edit dialog changes identifier/config only; all replacement fields match the UI effect payload.
        var update = new UpdateModelRequestDTO();
        update.setModificationCount(original.getModificationCount());
        update.setName("edited-" + modelId);
        update.setModelIdentifier("edited-" + modelId);
        update.setModelConfig(config);
        update.setProvider(original.getProvider());
        update.setCommunicationMode(original.getCommunicationMode());

        given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(update)
                .put("/" + modelId)
                .then().statusCode(OK.getStatusCode());

        var stored = getModel(modelId);
        assertThat(stored.getName()).isEqualTo("edited-" + modelId);
        assertThat(stored.getModelIdentifier()).isEqualTo("edited-" + modelId);
        assertThat(stored.getProvider().getId()).isEqualTo(providerId);
        assertThat(stored.getCommunicationMode()).isEqualTo(original.getCommunicationMode());
        assertThat(stored.getModelConfig()).isEqualTo(config);

        var criteria = new ModelSearchCriteriaDTO();
        criteria.setProviderId(providerId);
        criteria.setPageNumber(0);
        criteria.setPageSize(10);
        var models = given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .body(criteria)
                .post("/search")
                .then().statusCode(OK.getStatusCode())
                .extract().as(ModelPageResultDTO.class);
        assertThat(models.getStream()).extracting(ModelDTO::getId).containsExactly(modelId);
    }

    private ModelDTO getModel(String modelId) {
        return given()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .contentType(APPLICATION_JSON)
                .get("/" + modelId)
                .then().statusCode(OK.getStatusCode())
                .extract().as(ModelDTO.class);
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
