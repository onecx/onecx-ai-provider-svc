package org.tkit.onecx.ai.provider.common.services.mcp;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import dev.langchain4j.agent.tool.ToolSpecification;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Registry that holds all available MCP tools and allows lookup by tool name.
 */
@RegisterForReflection
public record McpToolRegistry(
        List<McpTool> tools,
        Map<String, McpTool> toolsByName) {
    public McpToolRegistry(List<McpTool> tools) {
        this(tools, tools.stream()
                .collect(Collectors.toMap(
                        McpTool::toolName,
                        Function.identity())));
    }

    public static McpToolRegistry empty() {
        return new McpToolRegistry(List.of(), Map.of());
    }

    public Optional<McpTool> findByName(String name) {
        return Optional.ofNullable(toolsByName.get(name));
    }

    public List<ToolSpecification> getToolSpecifications() {
        return tools.stream()
                .map(McpTool::toolSpecification)
                .toList();
    }

    /**
     * Closes all MCP clients.
     */
    public void close() {
        tools.stream()
                .map(McpTool::mcpClient)
                .distinct()
                .forEach(client -> {
                    try {
                        client.close();
                    } catch (Exception e) {
                        // Ignore close exceptions
                    }
                });
    }
}
