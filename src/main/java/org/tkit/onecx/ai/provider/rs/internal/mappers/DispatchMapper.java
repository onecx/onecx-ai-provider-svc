package org.tkit.onecx.ai.provider.rs.internal.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ConversationDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.RequestContextDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ChatRequestDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.ConversationDTO;
import gen.org.tkit.onecx.ai.provider.rs.internal.model.RequestContextDTO;

@Mapper
public interface DispatchMapper {

    ChatRequestDTOV1 map(ChatRequestDTO chatRequestDTO);

    @Mapping(target = "removeAiContextItem", ignore = true)
    RequestContextDTOV1 map(RequestContextDTO requestContextDTO);

    @Mapping(target = "removeHistoryItem", ignore = true)
    ConversationDTOV1 map(ConversationDTO conversationDTO);
}
