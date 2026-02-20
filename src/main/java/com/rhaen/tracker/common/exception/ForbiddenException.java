package com.rhaen.tracker.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends TrackerException {
    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
