package org.tkit.onecx.ai.provider.common.models;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChatRequestModel {
    private RequestContext requestContext;

    private ChatMessage chatMessage;
    private Conversation conversation;

    public boolean hasMessage() {
        return conversation != null && conversation.getHistory() != null && !conversation.getHistory().isEmpty();
    }
}
