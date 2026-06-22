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
import org.tkit.onecx.ai.provider.common.services.agentic.tool.ToolPolicyService;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.Tool;
import org.tkit.onecx.ai.provider.domain.models.enums.ToolType;

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

    @Inject
    ToolPolicyService toolPolicyService;

    /**
     * Creates a tool registry from all MCP-type tools defined in the agent.
     */
    public McpToolRegistry createToolRegistry(Agent agent) {
        return createToolRegistry(agent, null);
    }

    /**
     * Creates a tool registry from all MCP-type tools defined in the agent.
     */
    public McpToolRegistry createToolRegistry(Agent agent, String executionId) {
        if (agent == null || agent.getTools() == null || agent.getTools().isEmpty()) {
            log.debug("No tools configured in agent");
            return McpToolRegistry.empty();
        }

        List<McpTool> allTools = new ArrayList<>();

        // Filter only MCP-type tools
        for (Tool tool : agent.getTools()) {
            if (tool.getType() == ToolType.MCP && isAllowedByPolicy(agent, tool)) {
                allTools.addAll(discoverToolsFromServer(tool));
            }
        }

        log.info("Created tool registry with {} MCP tools from agent",
                allTools.size());

        return new McpToolRegistry(allTools);
    }

    private List<McpTool> discoverToolsFromServer(Tool tool) {
        log.info("Discovering tools from MCP server: {}", tool.getUrl());

        try {
            // client must stay open for later tool execution
            McpClient client = createMcpClient(tool);
            try {
                client.checkHealth();
                List<ToolSpecification> specs = receiveToolSpecifications(client);
                log.info("Discovered {} tool(s) from {}", specs.size(), tool.getUrl());
                return specs.stream()
                        .map(spec -> new McpTool(tool.getId() != null ? tool.getId().toString() : null, tool.getUrl(), spec,
                                client))
                        .toList();
            } catch (Exception ex) {
                log.error("MCP server not available {}: {}", tool.getUrl(), ex.getMessage(), ex);
                return List.of();
            }

        } catch (Exception e) {
            log.error("Error discovering tools from {}: {}", tool.getUrl(), e.getMessage(), e);
            return List.of();
        }
    }

    @Retry
    @Fallback(fallbackMethod = "receiveToolSpecificationsFallback")
    protected List<ToolSpecification> receiveToolSpecifications(McpClient client) {
        return client.listTools();
    }

    protected List<ToolSpecification> receiveToolSpecificationsFallback(McpClient client) {
        log.error("Failed to receive tool specifications after retries: {}",
                dispatchConfig.mcpConfig().maxToolExecutionRetries());
        return List.of();
    }

    protected McpClient createMcpClient(Tool tool) {
        var transportBuilder = StreamableHttpMcpTransport.builder()
                .url(tool.getUrl())
                .timeout(Duration.ofSeconds(dispatchConfig.mcpConfig().maxTimeout()))
                .logRequests(dispatchConfig.mcpConfig().logRequests())
                .logResponses(dispatchConfig.mcpConfig().logResponse());

        if (tool.getApiKey() != null && !tool.getApiKey().isBlank()) {
            transportBuilder.customHeaders(Map.of("Authorization", tool.getApiKey()));
        }

        return DefaultMcpClient.builder()
                .transport(transportBuilder.build())
                .build();
    }

    private boolean isAllowedByPolicy(Agent agent, Tool tool) {
        if (toolPolicyService == null) {
            return true;
        }
        if (tool.getId() == null) {
            log.debug("Tool '{}' has no ID; skipping allow-list enforcement", tool.getName());
            return true;
        }
        boolean allowed = toolPolicyService.isToolAllowed(agent, tool.getId().toString());
        if (!allowed) {
            log.warn("Tool '{}' denied by policy for agent '{}'", tool.getName(), agent.getName());
        }
        return allowed;
    }
}
