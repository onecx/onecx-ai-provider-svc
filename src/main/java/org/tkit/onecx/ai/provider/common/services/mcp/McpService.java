package org.tkit.onecx.ai.provider.common.services.mcp;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.tkit.onecx.ai.provider.common.models.DispatchConfig;
import org.tkit.onecx.ai.provider.domain.models.Configuration;
import org.tkit.onecx.ai.provider.domain.models.MCPServer;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for MCP tool discovery and registry creation.
 */
@Slf4j
@ApplicationScoped
public class McpService {

    @Inject
    DispatchConfig dispatchConfig;

    /**
     * Creates a tool registry from all MCP servers defined in the context.
     */
    public McpToolRegistry createToolRegistry(Configuration configuration) {
        if (configuration == null || configuration.getMcpServers() == null || configuration.getMcpServers().isEmpty()) {
            log.debug("No MCP servers configured in context");
            return McpToolRegistry.empty();
        }

        List<McpTool> allTools = new ArrayList<>();

        for (MCPServer mcpServer : configuration.getMcpServers()) {
            allTools.addAll(discoverToolsFromServer(mcpServer));
        }

        log.info("Created tool registry with {} tools from {} servers",
                allTools.size(), configuration.getMcpServers().size());

        return new McpToolRegistry(allTools);
    }

    private List<McpTool> discoverToolsFromServer(MCPServer mcpServer) {
        log.info("Discovering tools from MCP server: {}", mcpServer.getUrl());

        try {
            // client must stay open for later tool execution
            McpClient client = createMcpClient(mcpServer);
            try {
                client.checkHealth();
                List<ToolSpecification> specs = receiveToolSpecifications(client);
                log.info("Discovered {} tool(s) from {}", specs.size(), mcpServer.getUrl());
                return specs.stream()
                        .map(spec -> new McpTool(mcpServer.getUrl(), spec, client))
                        .toList();
            } catch (Exception ex) {
                log.error("MCP server not available {}: {}", mcpServer.getUrl(), ex.getMessage(), ex);
                return List.of();
            }

        } catch (Exception e) {
            log.error("Error discovering tools from {}: {}", mcpServer.getUrl(), e.getMessage(), e);
            return List.of();
        }
    }

    @Retry
    @Fallback(fallbackMethod = "receiveToolSpecificationsFallback")
    protected List<ToolSpecification> receiveToolSpecifications(McpClient client) {
        return client.listTools();
    }

    private List<ToolSpecification> receiveToolSpecificationsFallback(McpClient client) {
        log.error("Failed to receive tool specifications after retries: {}",
                dispatchConfig.mcpConfig().maxToolExecutionRetries());
        return List.of();
    }

    private McpClient createMcpClient(MCPServer mcpServer) {
        var transportBuilder = StreamableHttpMcpTransport.builder()
                .url(mcpServer.getUrl())
                .timeout(Duration.ofSeconds(dispatchConfig.mcpConfig().maxTimeout()))
                .logRequests(dispatchConfig.mcpConfig().logRequests())
                .logResponses(dispatchConfig.mcpConfig().logResponse());

        if (mcpServer.getApiKey() != null && !mcpServer.getApiKey().isBlank()) {
            transportBuilder.customHeaders(Map.of("Authorization", mcpServer.getApiKey()));
        }

        return DefaultMcpClient.builder()
                .transport(transportBuilder.build())
                .build();
    }
}
