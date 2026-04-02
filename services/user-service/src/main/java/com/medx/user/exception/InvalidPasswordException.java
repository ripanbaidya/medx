package com.medx.user.exception;


import com.medx.user.enums.ErrorCode;

public class InvalidPasswordException extends BaseException {

    public InvalidPasswordException(String message) {
        super(ErrorCode.VALIDATION_FAILED, message);
    }
}