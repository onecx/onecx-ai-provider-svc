package org.tkit.onecx.ai.provider.common.services.dispatch;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.tkit.onecx.ai.provider.common.services.agent.AgentService;
import org.tkit.onecx.ai.provider.common.services.runtime.ProviderRuntimeClient;
import org.tkit.onecx.ai.provider.common.services.runtime.RuntimeSnapshotMapper;
import org.tkit.onecx.ai.provider.common.services.runtime.dto.RuntimeDtos.RuntimeChatResponse;
import org.tkit.onecx.ai.provider.common.services.runtime.dto.RuntimeDtos.RuntimeStatus;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatMessageDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ChatDispatchService {

    @Inject
    AgentService agentService;

    @Inject
    RuntimeSnapshotMapper runtimeSnapshotMapper;

    @Inject
    @RestClient
    ProviderRuntimeClient providerRuntimeClient;

    public Response chat(ChatRequestDTOV1 chatRequestDTO) {
        log.info("Received chat request: {}", chatRequestDTO);
        var agent = agentService.findAgentByRequestContext(chatRequestDTO.getRequestContext());
        if (agent == null) {
            log.error("No agent found for request context: {}", chatRequestDTO.getRequestContext());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("No agent found for the given request context")
                    .build();
        }
        RuntimeChatResponse result = providerRuntimeClient.chat(runtimeSnapshotMapper.toRuntimeRequest(agent, chatRequestDTO));
        return result != null && RuntimeStatus.SUCCESS.equals(result.status())
                ? Response.ok(mapToChatMessageResponseDTO(result.message())).build()
                : Response.status(responseStatus(result))
                        .entity(result != null && result.message() != null ? result.message() : "Agent invocation failed")
                        .build();
    }

    private Response.Status responseStatus(RuntimeChatResponse result) {
        return result != null && RuntimeStatus.TIMEOUT.equals(result.status())
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
