package org.tkit.onecx.ai.provider.common.exceptions;

public class ChatException extends RuntimeException {

    private final ErrorType errorType;

    ChatException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public enum ErrorType {
        BAD_REQUEST,
        NOT_FOUND
    }
}
