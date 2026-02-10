package org.tkit.onecx.ai.provider.common.services.mcp;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolExecutionResult;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Represents a tool provided by an MCP server.
 */
@RegisterForReflection
public record McpTool(
        String serverUrl,
        ToolSpecification toolSpecification,
        McpClient mcpClient) {

    public String toolName() {
        return toolSpecification.name();
    }

    /**
     * Executes this tool with the given arguments.
     */
    public String execute(ToolExecutionRequest request) {
        ToolExecutionResult result = mcpClient.executeTool(request);
        return result.resultText();
    }
}
