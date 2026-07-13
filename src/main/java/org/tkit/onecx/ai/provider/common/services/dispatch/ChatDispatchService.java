package org.tkit.onecx.ai.provider.common.services.dispatch;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.tkit.onecx.ai.provider.common.services.agent.AgentService;
import org.tkit.onecx.ai.provider.domain.daos.AgentDAO;
import org.tkit.onecx.ai.provider.domain.daos.ExternalAgentDAO;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.onecx.ai.provider.domain.models.AgentGroup;
import org.tkit.onecx.ai.provider.rs.external.v1.mappers.RuntimeSnapshotMapper;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import gen.org.tkit.onecx.ai.provider.runtime.client.api.RuntimeInternalApi;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.AgentGroupSnapshot;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.RuntimeChatResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ChatDispatchService {

    @Inject
    AgentService agentService;

    @Inject
    AgentDAO agentDAO;

    @Inject
    ExternalAgentDAO externalAgentDAO;

    @Inject
    RuntimeSnapshotMapper runtimeSnapshotMapper;

    @Inject
    @RestClient
    RuntimeInternalApi providerRuntimeClient;

    public Response chat(ChatRequestDTOV1 chatRequestDTO) {
        log.info("Received chat request: {}", chatRequestDTO);
        var agent = agentService.findAgentByRequestContext(chatRequestDTO.getRequestContext());
        if (agent == null) {
            log.error("No agent found for request context: {}", chatRequestDTO.getRequestContext());
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("No agent found for the given request context")
                    .build();
        }
        int runtimeStatus;
        RuntimeChatResponse result;
        try (Response response = providerRuntimeClient.chat(
                runtimeSnapshotMapper.toRuntimeRequest(agent, chatRequestDTO, mapGroups(agent)))) {
            runtimeStatus = response.getStatus();
            result = response.readEntity(RuntimeChatResponse.class);
        } catch (Exception e) {
            log.error("Error invoking runtime chat API: {}", e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error invoking runtime chat API: " + e.getMessage())
                    .build();
        }
        if (runtimeStatus == Response.Status.OK.getStatusCode()) {
            return Response.ok(runtimeSnapshotMapper.mapRuntimeChatMessage(result.getMessage(),
                    conversationId(chatRequestDTO))).build();
        }
        return Response.status(responseStatus(runtimeStatus))
                .entity(result != null && result.getMessage() != null ? result.getMessage()
                        : "Agent invocation failed")
                .build();
    }

    private Response.Status responseStatus(int runtimeStatus) {
        return runtimeStatus == Response.Status.GATEWAY_TIMEOUT.getStatusCode()
                ? Response.Status.GATEWAY_TIMEOUT
                : Response.Status.BAD_REQUEST;
    }

    private String conversationId(ChatRequestDTOV1 request) {
        return request.getChatMessage() != null && request.getChatMessage().getConversationId() != null
                ? request.getChatMessage().getConversationId()
                : null;
    }

    private List<AgentGroupSnapshot> mapGroups(Agent agent) {
        if (agent.getGroups() == null || agent.getGroups().isEmpty()) {
            return List.of();
        }
        return agent.getGroups().stream().map(this::mapGroup).toList();
    }

    private AgentGroupSnapshot mapGroup(AgentGroup group) {
        var agents = agentDAO.findAgentsByGroupId(group.getId()).stream()
                .map(runtimeSnapshotMapper::mapAgent)
                .toList();
        var externalAgents = externalAgentDAO.findExternalAgentsByGroupId(group.getId()).stream()
                .map(runtimeSnapshotMapper::mapExternalAgent)
                .toList();
        return runtimeSnapshotMapper.mapGroup(group, agents, externalAgents);
    }
}
