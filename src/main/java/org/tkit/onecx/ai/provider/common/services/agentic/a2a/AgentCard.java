package org.tkit.onecx.ai.provider.common.services.agentic.a2a;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the agent card returned from an external agent's discovery URL.
 * <p>
 * Per strict A2A protocol, the discovery URL (GET) returns this card which contains
 * the actual invocation endpoint ({@code url}) and descriptive metadata.
 * The {@code url} field is the target for subsequent task/chat invocations (POST).
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentCard(
        @JsonProperty("url") String url,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description) {

    /**
     * Returns {@code true} if this card has a non-blank invocation URL.
     */
    public boolean hasInvokeUrl() {
        return url != null && !url.isBlank();
    }
}
