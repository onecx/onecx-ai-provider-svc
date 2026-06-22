package org.tkit.onecx.ai.provider.common.services.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.ai.provider.test.AbstractTest;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class McpToolRegistryTest extends AbstractTest {

    @Test
    void findByName_returnsTool_whenPresent() {
        McpClient client = mock(McpClient.class);
        McpTool tool = new McpTool("tool-id", "http://mcp", toolSpec("tool-a"), client);
        McpToolRegistry registry = new McpToolRegistry(List.of(tool));

        var result = registry.findByName("tool-a");

        assertThat(result).contains(tool);
    }

    @Test
    void findByName_returnsEmpty_whenMissing() {
        McpClient client = mock(McpClient.class);
        McpTool tool = new McpTool("tool-id", "http://mcp", toolSpec("tool-a"), client);
        McpToolRegistry registry = new McpToolRegistry(List.of(tool));

        var result = registry.findByName("tool-missing");

        assertThat(result).isEmpty();
    }

    @Test
    void close_ignoresClientCloseExceptions_andClosesDistinctClientsOnce() throws Exception {
        McpClient failingClient = mock(McpClient.class);
        McpClient healthyClient = mock(McpClient.class);
        doThrow(new Exception("close failed")).when(failingClient).close();

        McpTool tool1 = new McpTool("tool-id-1", "http://mcp-1", toolSpec("tool-a"), failingClient);
        McpTool tool2 = new McpTool("tool-id-2", "http://mcp-2", toolSpec("tool-b"), failingClient);
        McpTool tool3 = new McpTool("tool-id-3", "http://mcp-3", toolSpec("tool-c"), healthyClient);

        McpToolRegistry registry = new McpToolRegistry(List.of(tool1, tool2, tool3));

        assertThatCode(registry::close).doesNotThrowAnyException();

        verify(failingClient, times(1)).close();
        verify(healthyClient, times(1)).close();
    }

    private static ToolSpecification toolSpec(String name) {
        return ToolSpecification.builder()
                .name(name)
                .description("desc")
                .parameters(JsonObjectSchema.builder().build())
                .build();
    }
}
