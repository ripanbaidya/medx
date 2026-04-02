package com.medx.user.exception;


import com.medx.user.enums.ErrorCode;

public class KeycloakRegistrationException extends BaseException {

    public KeycloakRegistrationException(String customMessage) {
        super(ErrorCode.INTERNAL_SERVER_ERROR, customMessage);
    }

    public KeycloakRegistrationException(String customMessage, Throwable cause) {
        super(ErrorCode.INTERNAL_SERVER_ERROR, customMessage, cause);
    }
}