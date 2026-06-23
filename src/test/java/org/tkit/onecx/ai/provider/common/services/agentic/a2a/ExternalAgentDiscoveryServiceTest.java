package org.tkit.onecx.ai.provider.common.services.agentic.a2a;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class ExternalAgentDiscoveryServiceTest {

    private ExternalAgentDiscoveryService service;

    @BeforeEach
    void setUp() {
        service = new ExternalAgentDiscoveryService();
        service.objectMapper = new ObjectMapper();
    }

    // -----------------------------------------------------------------------
    // Null / blank input guard tests (no network needed)
    // -----------------------------------------------------------------------

    @Test
    void fetchAgentCard_nullUrl_returnsNull() {
        AgentCard card = service.fetchAgentCard(null);
        assertThat(card).isNull();
    }

    @Test
    void fetchAgentCard_blankUrl_returnsNull() {
        AgentCard card = service.fetchAgentCard("   ");
        assertThat(card).isNull();
    }

    @Test
    void fetchAgentCard_emptyUrl_returnsNull() {
        AgentCard card = service.fetchAgentCard("");
        assertThat(card).isNull();
    }

    // -----------------------------------------------------------------------
    // AgentCard record tests (no network needed)
    // -----------------------------------------------------------------------

    @Test
    void agentCard_withValidUrl_hasInvokeUrl() {
        AgentCard card = new AgentCard("http://agent/tasks/send", "my-agent", "desc");
        assertThat(card.hasInvokeUrl()).isTrue();
        assertThat(card.url()).isEqualTo("http://agent/tasks/send");
        assertThat(card.name()).isEqualTo("my-agent");
        assertThat(card.description()).isEqualTo("desc");
    }

    @Test
    void agentCard_withNullUrl_hasNoInvokeUrl() {
        AgentCard card = new AgentCard(null, "my-agent", null);
        assertThat(card.hasInvokeUrl()).isFalse();
    }

    @Test
    void agentCard_withBlankUrl_hasNoInvokeUrl() {
        AgentCard card = new AgentCard("  ", "my-agent", null);
        assertThat(card.hasInvokeUrl()).isFalse();
    }

    @Test
    void agentCard_deserializesFromJson() throws Exception {
        String json = """
                {
                  "url": "http://remote-agent/tasks",
                  "name": "remote-bot",
                  "description": "A remote agent",
                  "extraField": "ignored"
                }
                """;
        AgentCard card = new ObjectMapper().readValue(json, AgentCard.class);
        assertThat(card.url()).isEqualTo("http://remote-agent/tasks");
        assertThat(card.name()).isEqualTo("remote-bot");
        assertThat(card.description()).isEqualTo("A remote agent");
    }

    @Test
    void agentCard_deserializesFromMinimalJson() throws Exception {
        String json = """
                { "url": "http://agent/invoke" }
                """;
        AgentCard card = new ObjectMapper().readValue(json, AgentCard.class);
        assertThat(card.hasInvokeUrl()).isTrue();
        assertThat(card.url()).isEqualTo("http://agent/invoke");
        assertThat(card.name()).isNull();
        assertThat(card.description()).isNull();
    }

    @Test
    void agentCard_deserializesFromJsonWithoutUrl() throws Exception {
        String json = """
                { "name": "agent-without-url" }
                """;
        AgentCard card = new ObjectMapper().readValue(json, AgentCard.class);
        assertThat(card.hasInvokeUrl()).isFalse();
        assertThat(card.url()).isNull();
    }

    // -----------------------------------------------------------------------
    // Network-failure handling (unreachable URL → returns null, no exception)
    // -----------------------------------------------------------------------

    @Test
    void fetchAgentCard_unreachableHost_returnsNull() {
        // This must not throw; it should log and return null gracefully
        AgentCard card = service.fetchAgentCard("http://127.0.0.1:19999/agent.json");
        assertThat(card).isNull();
    }
}
