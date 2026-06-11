package com.mes.ai.service.impl;

public class AiModelUnavailableException extends RuntimeException {

    public AiModelUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
