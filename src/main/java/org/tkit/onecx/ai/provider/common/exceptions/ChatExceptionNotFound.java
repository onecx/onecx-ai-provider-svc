package org.tkit.onecx.ai.provider.common.exceptions;

public class ChatExceptionNotFound extends ChatException {

    public ChatExceptionNotFound(String message) {
        super(ErrorType.NOT_FOUND, message);
    }

}
