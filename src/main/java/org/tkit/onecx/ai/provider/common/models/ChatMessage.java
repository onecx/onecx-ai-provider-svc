package org.tkit.onecx.ai.provider.common.models;

public class ChatMessage {

    private String message;
    private Type type;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public enum Type {

        ASSISTANT,
        SYSTEM,
        USER,
        ACTION;
    }
}
