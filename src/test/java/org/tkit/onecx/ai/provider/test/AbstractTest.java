package org.tkit.onecx.ai.provider.test;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static io.restassured.RestAssured.config;
import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.security.PrivateKey;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.jwt.Claims;
import org.tkit.onecx.ai.provider.common.services.agent.AgentConfigurationService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import gen.org.tkit.onecx.ai.provider.rs.internal.model.AgentConfigurationVersionDTO;
import io.quarkiverse.mockserver.test.MockServerTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.config.RestAssuredConfig;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.util.KeyUtils;

@SuppressWarnings("java:S2187")
@QuarkusTestResource(MockServerTestResource.class)
public class AbstractTest {

    KeycloakTestClient keycloakClient = new KeycloakTestClient();

    protected static final String APM_HEADER_PARAM = "apm-principal-token";
    protected static final String CLAIMS_ORG_ID = ConfigProvider.getConfig()
            .getValue("%test.tkit.rs.context.tenant-id.mock.claim-org-id", String.class);;

    static {
        config = RestAssuredConfig.config().objectMapperConfig(
                objectMapperConfig().jackson2ObjectMapperFactory(
                        (cls, charset) -> {
                            var objectMapper = new ObjectMapper();
                            objectMapper.registerModule(new JavaTimeModule());
                            objectMapper.configure(WRITE_DATES_AS_TIMESTAMPS, false);
                            return objectMapper;
                        }));
    }

    /**
     * Publishes the current agent configuration as a new version and activates it, the same way the UI does.
     * Chat only runs against an active published configuration.
     */
    protected static void publishActiveConfiguration(AgentConfigurationService service, String agentId) {
        var created = service.createVersion(agentId, "Published for test", "test-user");
        assertThat(created.getStatus()).isEqualTo(201);
        var versionId = ((AgentConfigurationVersionDTO) created.getEntity()).getId();
        assertThat(service.activateAgentConfigurationVersion(agentId, versionId).getStatus()).isEqualTo(200);
    }

    protected String getKeycloakClientToken(String clientId) {
        return keycloakClient.getClientAccessToken(clientId);
    }

    protected static String createToken(String organizationId) {
        try {
            String userName = "test-user";
            JsonObjectBuilder claims = Json.createObjectBuilder();
            claims.add(Claims.preferred_username.name(), userName);
            claims.add(Claims.sub.name(), userName);
            claims.add(CLAIMS_ORG_ID, organizationId);
            PrivateKey privateKey = KeyUtils.generateKeyPair(2048).getPrivate();
            return Jwt.claims(claims.build()).sign(privateKey);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
