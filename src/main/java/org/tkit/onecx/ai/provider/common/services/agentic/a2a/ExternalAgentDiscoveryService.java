package org.tkit.onecx.ai.provider.common.services.agentic.a2a;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Performs strict A2A protocol discovery: fetches and parses the agent card from
 * an external agent's discovery URL.
 *
 * <p>
 * Protocol flow:
 * <ol>
 * <li>HTTP GET {@code discoveryUrl} → receive agent card JSON</li>
 * <li>Parse the JSON into an {@link AgentCard}</li>
 * <li>Caller uses {@link AgentCard#url()} as the actual invocation endpoint</li>
 * </ol>
 * </p>
 */
@Slf4j
@ApplicationScoped
public class ExternalAgentDiscoveryService {

    /** Shared HTTP client for discovery calls; short connect timeout is intentional. */
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Inject
    ObjectMapper objectMapper;

    /**
     * Fetches and parses the agent card from the given discovery URL.
     *
     * @param discoveryUrl the URL to GET for the agent card
     * @return the parsed {@link AgentCard}, or {@code null} if discovery fails for any reason
     */
    public AgentCard fetchAgentCard(String discoveryUrl) {
        if (discoveryUrl == null || discoveryUrl.isBlank()) {
            log.debug("Discovery URL is blank; skipping discovery");
            return null;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(discoveryUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            log.debug("Fetching agent card from discovery URL '{}'", discoveryUrl);
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Agent card discovery at '{}' returned non-success status {}", discoveryUrl,
                        response.statusCode());
                return null;
            }

            AgentCard card = objectMapper.readValue(response.body(), AgentCard.class);
            if (card == null || !card.hasInvokeUrl()) {
                log.warn("Agent card from '{}' has no valid invoke URL; strict protocol requires a 'url' field",
                        discoveryUrl);
                return null;
            }

            log.debug("Discovered agent card from '{}': invokeUrl='{}', name='{}'",
                    discoveryUrl, card.url(), card.name());
            return card;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Agent card discovery at '{}' was interrupted", discoveryUrl, e);
            return null;
        } catch (IOException e) {
            log.warn("Failed to fetch agent card from '{}'", discoveryUrl, e);
            return null;
        }
    }
}
