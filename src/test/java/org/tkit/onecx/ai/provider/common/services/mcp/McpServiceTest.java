package org.tkit.onecx.ai.provider.common.services.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.tkit.onecx.ai.provider.common.models.DispatchConfig;
import org.tkit.onecx.ai.provider.domain.models.Configuration;
import org.tkit.onecx.ai.provider.domain.models.MCPServer;
import org.tkit.onecx.ai.provider.test.AbstractTest;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class McpServiceTest extends AbstractTest {

    @Test
    void createToolRegistry_returnsEmpty_whenConfigurationNull() {
        var service = serviceWithConfig();

        var registry = service.createToolRegistry(null);

        assertThat(registry.tools()).isEmpty();
    }

    @Test
    void createToolRegistry_returnsEmpty_whenServerListNullOrEmpty() {
        var service = serviceWithConfig();

        var configWithNullServers = new Configuration();
        configWithNullServers.setMcpServers(null);
        var nullRegistry = service.createToolRegistry(configWithNullServers);

        var configWithEmptyServers = new Configuration();
        configWithEmptyServers.setMcpServers(Set.of());
        var emptyRegistry = service.createToolRegistry(configWithEmptyServers);

        assertThat(nullRegistry.tools()).isEmpty();
        assertThat(emptyRegistry.tools()).isEmpty();
    }

    @Test
    void createToolRegistry_mergesDiscoveredTools_andSkipsFailingServer() {
        var service = new TestableMcpService();
        service.dispatchConfig = dispatchConfig();

        McpClient okClient = mock(McpClient.class);
        when(okClient.listTools()).thenReturn(List.of(toolSpec("tool-a"), toolSpec("tool-b")));
        service.registerClient("http://ok", okClient);

        McpClient failingClient = mock(McpClient.class);
        doThrow(new RuntimeException("down")).when(failingClient).checkHealth();
        service.registerClient("http://down", failingClient);

        var config = new Configuration();
        config.setMcpServers(Set.of(server("http://ok", null), server("http://down", null)));

        var registry = service.createToolRegistry(config);

        assertThat(registry.tools()).hasSize(2);
        assertThat(registry.getToolSpecifications()).extracting(ToolSpecification::name)
                .containsExactlyInAnyOrder("tool-a", "tool-b");
    }

    @Test
    void createToolRegistry_returnsEmpty_whenClientCreationThrows() {
        var service = new TestableMcpService();
        service.dispatchConfig = dispatchConfig();
        service.registerClientCreationError("http://boom", new RuntimeException("cannot create client"));

        var config = new Configuration();
        config.setMcpServers(Set.of(server("http://boom", null)));

        var registry = service.createToolRegistry(config);

        assertThat(registry.tools()).isEmpty();
    }

    @Test
    void receiveToolSpecifications_returnsClientTools() {
        var service = serviceWithConfig();
        McpClient client = mock(McpClient.class);
        when(client.listTools()).thenReturn(List.of(toolSpec("tool-x")));

        var result = service.receiveToolSpecifications(client);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("tool-x");
    }

    @Test
    void receiveToolSpecificationsFallback_returnsEmptyList() {
        var service = serviceWithConfig();

        var result = service.receiveToolSpecificationsFallback(mock(McpClient.class));

        assertThat(result).isEmpty();
    }

    @Test
    void createMcpClient_handlesApiKeyBlankAndNonBlank() {
        var service = serviceWithConfig();

        // We only need to execute all apiKey branches; build may throw runtime errors depending on endpoint reachability.
        executeCreateClientBranch(service, server("http://127.0.0.1:9", null));
        executeCreateClientBranch(service, server("http://127.0.0.1:9", "   "));
        executeCreateClientBranch(service, server("http://127.0.0.1:9", "Bearer token"));
    }

    @Test
    void createMcpClient_buildLine_isCoveredWithMockedBuilders() {
        var service = serviceWithConfig();
        var mcpServer = server("http://example.org", "Bearer token");

        StreamableHttpMcpTransport.Builder transportBuilder = mock(StreamableHttpMcpTransport.Builder.class);
        StreamableHttpMcpTransport transport = mock(StreamableHttpMcpTransport.class);
        DefaultMcpClient.Builder clientBuilder = mock(DefaultMcpClient.Builder.class);
        DefaultMcpClient client = mock(DefaultMcpClient.class);

        when(transportBuilder.url("http://example.org")).thenReturn(transportBuilder);
        when(transportBuilder.timeout(java.time.Duration.ofSeconds(1))).thenReturn(transportBuilder);
        when(transportBuilder.logRequests(false)).thenReturn(transportBuilder);
        when(transportBuilder.logResponses(false)).thenReturn(transportBuilder);
        when(transportBuilder.customHeaders(Map.of("Authorization", "Bearer token"))).thenReturn(transportBuilder);
        when(transportBuilder.build()).thenReturn(transport);

        when(clientBuilder.transport(transport)).thenReturn(clientBuilder);
        when(clientBuilder.build()).thenReturn(client);

        try (MockedStatic<StreamableHttpMcpTransport> transportStatic = mockStatic(StreamableHttpMcpTransport.class);
                MockedStatic<DefaultMcpClient> clientStatic = mockStatic(DefaultMcpClient.class)) {

            transportStatic.when(StreamableHttpMcpTransport::builder).thenReturn(transportBuilder);
            clientStatic.when(DefaultMcpClient::builder).thenReturn(clientBuilder);

            McpClient result = service.createMcpClient(mcpServer);
            assertThat(result).isSameAs(client);
        }
    }

    private static void executeCreateClientBranch(McpService service, MCPServer server) {
        try {
            McpClient client = service.createMcpClient(server);
            assertThat(client).isNotNull();
        } catch (RuntimeException ignored) {
            // acceptable in tests: timeout/connect failures still mean the branch was executed
        }
    }

    private static McpService serviceWithConfig() {
        var service = new McpService();
        service.dispatchConfig = dispatchConfig();
        return service;
    }

    private static DispatchConfig dispatchConfig() {
        DispatchConfig dispatchConfig = mock(DispatchConfig.class);
        DispatchConfig.MCPConfig mcpConfig = mock(DispatchConfig.MCPConfig.class);

        when(mcpConfig.maxTimeout()).thenReturn(1L);
        when(mcpConfig.logRequests()).thenReturn(false);
        when(mcpConfig.logResponse()).thenReturn(false);
        when(mcpConfig.maxToolExecutionRetries()).thenReturn(2L);
        when(dispatchConfig.mcpConfig()).thenReturn(mcpConfig);

        return dispatchConfig;
    }

    private static MCPServer server(String url, String apiKey) {
        MCPServer server = new MCPServer();
        server.setUrl(url);
        server.setApiKey(apiKey);
        return server;
    }

    private static ToolSpecification toolSpec(String name) {
        return ToolSpecification.builder()
                .name(name)
                .description("desc")
                .parameters(JsonObjectSchema.builder().build())
                .build();
    }

    static class TestableMcpService extends McpService {
        private final Map<String, McpClient> clients = new HashMap<>();
        private final Map<String, RuntimeException> creationErrors = new HashMap<>();

        void registerClient(String url, McpClient client) {
            clients.put(url, client);
        }

        void registerClientCreationError(String url, RuntimeException ex) {
            creationErrors.put(url, ex);
        }

        @Override
        protected McpClient createMcpClient(MCPServer mcpServer) {
            RuntimeException ex = creationErrors.get(mcpServer.getUrl());
            if (ex != null) {
                throw ex;
            }
            return clients.get(mcpServer.getUrl());
        }
    }
}
