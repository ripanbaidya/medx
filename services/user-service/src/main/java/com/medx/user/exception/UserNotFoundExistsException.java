package com.medx.user.exception;

import com.medx.user.enums.ErrorCode;

public class UserNotFoundExistsException extends BaseException {

    public UserNotFoundExistsException() {
        super(ErrorCode.USER_NOT_FOUND);
    }

    public UserNotFoundExistsException(String customMessage) {
        super(ErrorCode.USER_NOT_FOUND, customMessage);
    }
}
