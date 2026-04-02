package com.medx.user.exception;


import com.medx.user.enums.ErrorCode;

public class UserAlreadyExistsException extends BaseException {

    public UserAlreadyExistsException() {
        super(ErrorCode.USER_ALREADY_EXISTS);
    }

    public UserAlreadyExistsException(String customMessage) {
        super(ErrorCode.USER_ALREADY_EXISTS, customMessage);
    }
}
