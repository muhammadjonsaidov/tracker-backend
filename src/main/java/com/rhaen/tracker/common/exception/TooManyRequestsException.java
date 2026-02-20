package com.rhaen.tracker.common.exception;

import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends TrackerException {
    public TooManyRequestsException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS);
    }
}
