package org.tkit.onecx.ai.provider.common.exceptions;

public class ChatExceptionBadRequest extends ChatException {

    public ChatExceptionBadRequest(String message) {
        super(ErrorType.BAD_REQUEST, message);
    }

}
