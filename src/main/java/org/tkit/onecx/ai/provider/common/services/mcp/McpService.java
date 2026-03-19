package org.tkit.onecx.ai.provider.common.services.mcp;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;

import org.tkit.onecx.ai.provider.common.config.DispatchConfig;
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
    public McpToolRegistry createToolRegistry(List<MCPServer> mcpServers) {
        if (mcpServers.isEmpty()) {
            log.debug("No MCP servers configured in context");
            return McpToolRegistry.empty();
        }

        List<McpTool> allTools = new ArrayList<>();

        for (MCPServer mcpServer : mcpServers) {
            allTools.addAll(discoverToolsFromServer(mcpServer));
        }

        log.info("Created tool registry with {} tools from {} servers",
                allTools.size(), mcpServers.size());

        return new McpToolRegistry(allTools);
    }

    private List<McpTool> discoverToolsFromServer(MCPServer mcpServer) {
        log.info("Discovering tools from MCP server: {}", mcpServer.getUrl());

        try {
            // client must stay open for later tool execution
            McpClient client = createMcpClient(mcpServer);
            client.checkHealth();
            log.info("Discovering tool specifications from MCP server: {} client: {}", mcpServer.getUrl(), client.key());
            List<ToolSpecification> specs = client.listTools();
            log.info("Discovered {} tool(s) from {}", specs.size(), mcpServer.getUrl());
            return specs.stream()
                    .map(spec -> new McpTool(mcpServer.getUrl(), spec, client))
                    .toList();
        } catch (Exception e) {
            log.error("Error discovering tools from {}: {}", mcpServer.getUrl(), e.getMessage(), e);
            return List.of();
        }
    }

    //    @Retry
    //    @Fallback(fallbackMethod = "receiveToolSpecificationsFallback")
    //    protected List<ToolSpecification> receiveToolSpecifications(McpClient client) {
    //        log.info("Discovering tool specifications from MCP client: {}", client.key());
    //        return client.listTools();
    //    }
    //
    //    private List<ToolSpecification> receiveToolSpecificationsFallback(McpClient client) {
    //        log.error("Failed to receive tool specifications after retries: {}",
    //                dispatchConfig.mcpConfig().maxToolExecutionRetries());
    //        return List.of();
    //    }

    private McpClient createMcpClient(MCPServer mcpServer) {
        var transportBuilder = StreamableHttpMcpTransport.builder()
                .url(mcpServer.getUrl())
                .timeout(Duration.ofSeconds(dispatchConfig.mcpConfig().maxTimeout()))
                .logRequests(dispatchConfig.mcpConfig().logRequests())
                .logResponses(dispatchConfig.mcpConfig().logResponse());

        if (mcpServer.getApiKey() != null) {
            transportBuilder.customHeaders(Map.of(HttpHeaders.AUTHORIZATION, mcpServer.getApiKey()));
        }

        return DefaultMcpClient.builder().transport(transportBuilder.build()).build();
    }
}
