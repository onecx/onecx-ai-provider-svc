package org.tkit.onecx.ai.provider.common.services.mcp;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.*;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class McpToolRegistryTest {

    @Test
    void testCloseException() throws Exception {
        var mock = Mockito.mock(McpClient.class);
        Mockito.doThrow(new RuntimeException("Close failed")).when(mock).close();

        var spec = Mockito.mock(ToolSpecification.class);
        Mockito.when(spec.name()).thenReturn("mock");

        var tool = new McpTool(spec, mock);
        var tmp = new McpToolRegistry(List.of(tool));

        Assertions.assertDoesNotThrow(tmp::close);
    }

}
