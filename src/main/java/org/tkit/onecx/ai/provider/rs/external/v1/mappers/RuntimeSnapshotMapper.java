package org.tkit.onecx.ai.provider.rs.external.v1.mappers;

import java.util.List;

import org.mapstruct.Mapper;
import org.tkit.onecx.ai.provider.domain.models.Agent;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatMessageDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import gen.org.tkit.onecx.ai.provider.runtime.client.model.*;

@Mapper(uses = OffsetDateTimeMapper.class)
public interface RuntimeSnapshotMapper {

    default RuntimeChatRequest toRuntimeRequest(Agent agent, ChatRequestDTOV1 chatRequestDTO) {
        RuntimeChatRequest request = new RuntimeChatRequest();
        request.setChatRequest(mapChatRequest(chatRequestDTO));
        request.setRootAgent(mapAgent(agent, true));
        return request;
    }

    AgentSnapshot mapAgent(Agent agent, boolean b);

    private ChatRequest mapChatRequest(ChatRequestDTOV1 request) {
        if (request == null) {
            return null;
        }
        RequestContext requestContext = new RequestContext();
        if (request.getRequestContext() != null) {
            AgentFilter filter = null;
            if (request.getRequestContext().getFilter() != null) {
                filter = new AgentFilter();
                filter.setKey(request.getRequestContext().getFilter().getKey() != null
                        ? request.getRequestContext().getFilter().getKey().value()
                        : null);
                filter.setValue(request.getRequestContext().getFilter().getValue());
            }
            requestContext.setAiContext(request.getRequestContext().getAiContext());
            requestContext.setFilter(filter);
        }

        Conversation conversation = new Conversation();
        if (request.getConversation() != null) {
            List<ChatMessage> history = request.getConversation().getHistory() != null
                    ? request.getConversation().getHistory().stream().map(this::mapChatMessage).toList()
                    : List.of();
            conversation.setConversationId(request.getConversation().getConversationId());
            conversation.setConversationType(request.getConversation().getConversationType() != null
                    ? request.getConversation().getConversationType().value()
                    : null);
            conversation.setHistory(history);
        }
        ChatRequest runtimeChatRequest = new ChatRequest();
        runtimeChatRequest.setChatMessage(mapChatMessage(request.getChatMessage()));
        runtimeChatRequest.setRequestContext(requestContext);
        runtimeChatRequest.setConversation(conversation);
        return runtimeChatRequest;
    }

    ChatMessage mapChatMessage(ChatMessageDTOV1 chatMessageDTOV1);

    Object mapRuntimeChatMessage(String message);
}
