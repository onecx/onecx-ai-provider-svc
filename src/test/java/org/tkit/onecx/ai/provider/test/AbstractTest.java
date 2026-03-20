package org.tkit.onecx.ai.provider.test;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static io.restassured.RestAssured.config;
import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.jwt.Claims;
import org.mockserver.client.MockServerClient;
import org.mockserver.mock.Expectation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.quarkiverse.mockserver.test.MockServerTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.config.RestAssuredConfig;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.util.KeyUtils;

@SuppressWarnings("java:S2187")
@QuarkusTestResource(MockServerTestResource.class)
public class AbstractTest {

    private static final List<String> MOCK_IDS = new ArrayList<>();

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

    protected void addExpectation(Expectation[] exceptions) {
        for (Expectation e : List.of(exceptions)) {
            MOCK_IDS.add(e.getId());
        }

    }

    protected void clearExpectation(MockServerClient client) {
        MOCK_IDS.forEach(x -> {
            try {
                client.clear(x);
            } catch (Exception ex) {
                //  mockId not existing
            }
        });
        MOCK_IDS.clear();
    }

}
