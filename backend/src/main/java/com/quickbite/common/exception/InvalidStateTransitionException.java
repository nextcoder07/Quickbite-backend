package com.quickbite.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidStateTransitionException extends ApiException {
    public InvalidStateTransitionException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
