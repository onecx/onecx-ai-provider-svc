package org.tkit.onecx.ai.provider.common.services.mcp;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;

/**
 * Represents a tool provided by an MCP server.
 */
public record McpTool(
        ToolSpecification toolSpecification,
        McpClient mcpClient) {

}
