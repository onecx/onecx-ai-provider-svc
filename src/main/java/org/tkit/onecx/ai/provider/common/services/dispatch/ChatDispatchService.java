package org.tkit.onecx.ai.provider.common.services.dispatch;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.tkit.onecx.ai.provider.common.services.agent.AgentService;
import org.tkit.onecx.ai.provider.common.services.agentic.runtime.AgenticRuntimeResult;
import org.tkit.onecx.ai.provider.common.services.agentic.runtime.AgenticRuntimeService;
import org.tkit.onecx.ai.provider.common.services.agentic.runtime.AgenticRuntimeStatus;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatMessageDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ChatDispatchService {

    @Inject
    AgentService agentService;

    @Inject
    AgenticRuntimeService agenticRuntimeService;

    public Response chat(ChatRequestDTOV1 chatRequestDTO) {
        log.info("Received chat request: {}", chatRequestDTO);
        var agent = agentService.findAgentByRequestContext(chatRequestDTO.getRequestContext());
        if (agent == null) {
            log.error("No agent found for request context: {}", chatRequestDTO.getRequestContext());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("No agent found for the given request context")
                    .build();
        }
        AgenticRuntimeResult result = agenticRuntimeService.invokeRoot(agent, chatRequestDTO);
        Response.ResponseBuilder responseBuilder = result.successful()
                ? Response.ok(mapToChatMessageResponseDTO(result.responseText()))
                : Response.status(responseStatus(result))
                        .entity(result.responseText() != null ? result.responseText() : "Agent invocation failed");
        if (result.executionId() != null && !result.executionId().isBlank()) {
            responseBuilder.header("X-Execution-Id", result.executionId());
        }
        return responseBuilder.build();
    }

    private Response.Status responseStatus(AgenticRuntimeResult result) {
        return result != null && AgenticRuntimeStatus.TIMEOUT.equals(result.status())
                ? Response.Status.GATEWAY_TIMEOUT
                : Response.Status.BAD_REQUEST;
    }

    private ChatMessageDTOV1 mapToChatMessageResponseDTO(String responseMessage) {
        ChatMessageDTOV1 chatMessage = new ChatMessageDTOV1();
        chatMessage.setMessage(responseMessage != null ? responseMessage : "");
        chatMessage.setType(ChatMessageDTOV1.TypeEnum.ASSISTANT);
        return chatMessage;
    }
}
