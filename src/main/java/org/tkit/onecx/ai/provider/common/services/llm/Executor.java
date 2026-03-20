package org.tkit.onecx.ai.provider.common.services.llm;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.tkit.onecx.ai.provider.common.config.DispatchConfig;
import org.tkit.onecx.ai.provider.common.services.mcp.McpTool;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class Executor {

    @Inject
    DispatchConfig dispatchConfig;

    @Retry
    @Fallback(fallbackMethod = "toolExecutionFallback")
    public String executeToolRequestWithRetry(McpTool tool, ToolExecutionRequest toolRequest) {
        var client = tool.mcpClient();
        var result = client.executeTool(toolRequest);
        return result.resultText();
    }

    public String toolExecutionFallback(McpTool tool, ToolExecutionRequest toolRequest) {
        log.error("Tool execution failed after {} retries for tool: {}", dispatchConfig.mcpConfig().maxToolExecutionRetries(),
                toolRequest.name());
        return "Error: Tool execution failed for '" + toolRequest.name() + "'";
    }
}
