package org.tkit.onecx.ai.provider.common.models;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "onecx.ai.dispatch")
public interface DispatchConfig {

    /**
     * MCP related configuration
     */
    @WithName("mcp")
    MCPConfig mcpConfig();

    /**
     * LLM-Provider related configuration
     */
    @WithName("provider")
    ProviderConfig providerConfig();

    /**
     * A2A orchestration related configuration.
     */
    @WithName("a2a")
    A2AConfig a2aConfig();

    interface MCPConfig {

        /**
         * Maximum number of iterations for tool calls in MCP processing
         */
        @WithName("max-iterations")
        @WithDefault("3")
        long maxIterations();

        /**
         * Maximum timeout in seconds for MCP processing
         */
        @WithName("timeout")
        @WithDefault("60")
        long maxTimeout();

        /**
         * Whether to log the full response from MCP calls
         */
        @WithName("log-response")
        @WithDefault("false")
        boolean logResponse();

        /**
         * Whether to log the full requests to MCP calls
         */
        @WithName("log-requests")
        @WithDefault("false")
        boolean logRequests();

        /**
         * Maximum number of retries for tool execution in MCP processing
         */
        @WithName("max-tool-execution-retries")
        @WithDefault("3")
        long maxToolExecutionRetries();

        /**
         * Delay in milliseconds between retries for tool execution in MCP processing
         */
        @WithName("tool-execution-retry-delay")
        @WithDefault("1000")
        long toolExecutionRetryDelay();
    }

    interface ProviderConfig {

        /**
         * Timeout in seconds for provider calls
         */
        @WithName("timeout")
        @WithDefault("60")
        long timeout();

        /**
         * Whether to log the full response from provider calls
         */
        @WithName("log-response")
        @WithDefault("false")
        boolean logResponse();

        /**
         * Whether to log the full requests to provider calls
         */
        @WithName("log-requests")
        @WithDefault("false")
        boolean logRequests();
    }

    interface A2AConfig {

        /**
         * Maximum recursive depth for agent-to-agent orchestration.
         */
        @WithName("max-depth")
        @WithDefault("10")
        int maxDepth();
    }
}
