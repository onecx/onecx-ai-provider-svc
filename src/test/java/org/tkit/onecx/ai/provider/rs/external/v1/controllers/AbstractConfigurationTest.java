package org.tkit.onecx.ai.provider.rs.external.v1.controllers;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.time.OffsetDateTime;
import java.util.*;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockserver.client.MockServerClient;
import org.mockserver.matchers.MatchType;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;
import org.mockserver.serialization.ObjectMapperFactory;
import org.tkit.onecx.ai.provider.test.AbstractTest;

import dev.langchain4j.mcp.client.protocol.*;
import io.quarkiverse.mockserver.test.InjectMockServerClient;

abstract class AbstractConfigurationTest extends AbstractTest {

    @InjectMockServerClient
    MockServerClient mockServerClient;

    @AfterEach
    @BeforeEach
    void resetExpectation() {
        clearExpectation(mockServerClient);
    }

    protected Map<?, ?> createAssistantMessage(String message) {
        return createMessage("assistant", message);
    }

    protected Map<?, ?> createAssistantMessage(String message, Map<?, ?>... toolCalls) {
        Map<Object, Object> result = new HashMap<>(createMessage("assistant", message));
        if (toolCalls != null && toolCalls.length > 0) {
            result.put("tool_calls", Arrays.asList(toolCalls));
        }
        return result;
    }

    protected Map<?, ?> createUserMessage(String message) {
        return createMessage("user", message);
    }

    protected Map<?, ?> createMessage(String role, String message) {
        return Map.of("role", role, "content", message);
    }

    protected Map<?, ?> createTooCall(int index, String toolName, Map<?, ?> arguments) {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "index", index,
                        "name", toolName,
                        "arguments", arguments));
    }

    protected void ollamaCreateChat(String name, String model, String response) {
        ollamaCreateChat(name, model, createUserMessage(response));
    }

    protected void ollamaCreateChat(String name, String model, Map<?, ?> response) {

        var responseData = new HashMap<>();
        responseData.putAll(Map.of(
                "model", model,
                "created_at", "2026-03-19T10:00:00Z",
                "message", response));
        responseData.putAll(Map.of(
                "done_reason", "stop",
                "done", true,
                "total_duration", 1000000000,
                "load_duration", 100000000,
                "prompt_eval_count", 5,
                "prompt_eval_duration", 200000000,
                "eval_count", 10,
                "eval_duration", 700000000));

        addExpectation(mockServerClient
                .when(
                        request().withPath("/" + name + "/api/chat")
                                .withMethod(HttpMethod.POST)
                                .withBody(JsonBody.json(Map.of("model", model),
                                        MatchType.ONLY_MATCHING_FIELDS)))
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(responseData))));

    }

    protected void mcpCreateToolsCall(String name) {

        var responseData = new HashMap<>();
        responseData.put("isError", false);
        responseData.put("content", List.of(
                Map.of(
                        "type", "text",
                        "text", "searchTest executed successfully for name='Hallo'."),
                Map.of(
                        "type", "json",
                        "json", Map.of(
                                "query", "Hallo",
                                "matches", List.of(
                                        Map.of(
                                                "id", "t-1001",
                                                "label", "Hallo Welt",
                                                "score", 0.91),
                                        Map.of(
                                                "id", "t-1002",
                                                "label", "Hallo (greeting)",
                                                "score", 0.77)),
                                "count", 2))));

        addExpectation(mockServerClient
                .when(
                        request().withPath("/" + name)
                                .withMethod(HttpMethod.POST)
                                .withBody(JsonBody.json(Map.of("method", "tools/call"), MatchType.ONLY_MATCHING_FIELDS)))
                .respond(httpRequest -> {
                    var tmp = httpRequest.getBodyAsRawBytes();
                    var data = ObjectMapperFactory.createObjectMapper().readValue(tmp, Map.class);
                    var id = Long.valueOf((Integer) data.get("id"));

                    return response()
                            .withStatusCode(Response.Status.OK.getStatusCode())
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(JsonBody.json(Map.of(
                                    "jsonrpc", "2.0",
                                    "id", id,
                                    "result", responseData)));

                }));
    }

    protected void mcpCreateToolsList(String name) {
        addExpectation(mockServerClient
                .when(
                        request().withPath("/" + name)
                                .withMethod(HttpMethod.POST)
                                .withBody(JsonBody.json(new McpListToolsRequest(null), MatchType.ONLY_MATCHING_FIELDS)))
                .respond(httpRequest -> {
                    var tmp = httpRequest.getBodyAsRawBytes();
                    var data = ObjectMapperFactory.createObjectMapper().readValue(tmp, Map.class);
                    var id = Long.valueOf((Integer) data.get("id"));

                    return response()
                            .withStatusCode(Response.Status.OK.getStatusCode())
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(JsonBody.json(Map.of(
                                    "jsonrpc", "2.0",
                                    "id", id,
                                    "result", Map.of(
                                            "tools", List.of(
                                                    Map.of(
                                                            "name", "searchTest",
                                                            "description", "Search for available test",
                                                            "inputSchema",
                                                            Map.of("type", "object",
                                                                    "properties",
                                                                    Map.of("name",
                                                                            Map.of("type", "string", "description",
                                                                                    "Name of the test")),
                                                                    "required", List.of("name"))))))));

                }));
    }

    protected void mcpCreateToolsListResponse(String name, Response.Status status) {
        addExpectation(mockServerClient
                .when(
                        request().withPath("/" + name)
                                .withMethod(HttpMethod.POST)
                                .withBody(JsonBody.json(new McpListToolsRequest(null), MatchType.ONLY_MATCHING_FIELDS)))
                .respond(httpRequest -> response()
                        .withStatusCode(status.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)));
    }

    protected void mcpCreateNotify(String name) {
        addExpectation(mockServerClient
                .when(
                        request().withPath("/" + name)
                                .withMethod(HttpMethod.POST)
                                .withBody(JsonBody.json(new McpInitializationNotification(), MatchType.ONLY_MATCHING_FIELDS)))
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(Map.of(
                                "jsonrpc", "2.0",
                                "result", Map.of(
                                        "protocolVersion", "2025-06-18",
                                        "capabilities", new Object(),
                                        "serverInfo", Map.of(
                                                "name", name,
                                                "version", "1.0.0")))))));
    }

    protected void mcpCreateInit(String name) {
        var mcpInitRequest = new McpInitializeRequest(0L);
        // mcp initialize
        addExpectation(mockServerClient
                .when(
                        request().withPath("/" + name)
                                .withMethod(HttpMethod.POST)
                                .withBody(JsonBody.json(mcpInitRequest, MatchType.ONLY_MATCHING_FIELDS)))
                .respond(httpRequest -> response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(Map.of(
                                "jsonrpc", "2.0",
                                "id", 0,
                                "result", Map.of(
                                        "protocolVersion", "2025-06-18",
                                        "capabilities", new Object(),
                                        "serverInfo", Map.of(
                                                "name", name,
                                                "version", "1.0.0")))))));
    }

    protected void mcpCreatePing(String name) {
        // mcp ping request - response
        addExpectation(mockServerClient
                .when(
                        request().withPath("/" + name)
                                .withMethod(HttpMethod.POST)
                                .withBody(JsonBody.json(new McpPingRequest(null), MatchType.ONLY_MATCHING_FIELDS)))
                .respond(httpRequest -> {
                    var tmp = httpRequest.getBodyAsRawBytes();
                    var data = ObjectMapperFactory.createObjectMapper().readValue(tmp, Map.class);
                    var id = Long.valueOf((Integer) data.get("id"));

                    var pingResponse1 = new McpPingResponse(id);
                    pingResponse1.getResult().putAll(Map.of(
                            "status", "ok",
                            "server", name,
                            "timestamp", OffsetDateTime.now().toString()));

                    return response()
                            .withStatusCode(Response.Status.OK.getStatusCode())
                            .withContentType(MediaType.APPLICATION_JSON)
                            .withBody(JsonBody.json(pingResponse1));
                }));
    }
}
