package org.tkit.onecx.ai.provider.common.models;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChatResponseModel {

    private String conversationId;
    private String message;
    private Type type;

    public enum Type {
        ASSISTANT,
        SYSTEM,
        USER,
        ACTION;
    }

}
