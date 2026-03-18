package org.tkit.onecx.ai.provider.rs.external.v1.mappers;

import java.time.Instant;

import org.mapstruct.Mapper;
import org.tkit.onecx.ai.provider.common.models.ChatRequestModel;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import dev.langchain4j.model.chat.response.ChatResponse;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatMessageDTOV1;
import gen.org.tkit.onecx.ai.provider.rs.external.v1.model.ChatRequestDTOV1;

@Mapper(uses = { OffsetDateTimeMapper.class })
public interface DispatchMapper {

    ChatRequestModel map(ChatRequestDTOV1 dto);

    default ChatMessageDTOV1 create(ChatResponse response) {
        if (response == null) {
            return null;
        }
        var dto = new ChatMessageDTOV1();
        dto.setConversationId(response.id());
        if (response.aiMessage() != null) {
            dto.setMessage(response.aiMessage().text());
        }
        dto.setType(ChatMessageDTOV1.TypeEnum.ASSISTANT);
        dto.setCreationDate(Instant.now().toEpochMilli());
        return dto;
    }
}
