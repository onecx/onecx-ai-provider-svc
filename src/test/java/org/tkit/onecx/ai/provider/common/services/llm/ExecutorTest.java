package org.tkit.onecx.ai.provider.common.services.llm;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.tkit.onecx.ai.provider.common.services.mcp.McpTool;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolExecutionResult;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ExecutorTest {

    @Inject
    Executor executor;

    @Test
    void executeToolRequestWithRetry_successPath_isCovered() {
        McpClient client = Mockito.mock(McpClient.class);
        ToolExecutionRequest req = ToolExecutionRequest.builder()
                .name("searchTest")
                .arguments("{\"name\":\"Hallo\"}")
                .build();

        ToolExecutionResult ok = Mockito.mock(ToolExecutionResult.class);
        Mockito.when(ok.resultText()).thenReturn("ok-result");
        Mockito.when(client.executeTool(req)).thenReturn(ok);

        McpTool tool = new McpTool(Mockito.mock(ToolSpecification.class), client);

        String out = executor.executeToolRequestWithRetry(tool, req);

        Assertions.assertEquals("ok-result", out);
        Mockito.verify(client).executeTool(req);
    }
}
