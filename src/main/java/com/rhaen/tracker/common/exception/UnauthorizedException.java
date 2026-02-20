package com.rhaen.tracker.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends TrackerException {
    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
