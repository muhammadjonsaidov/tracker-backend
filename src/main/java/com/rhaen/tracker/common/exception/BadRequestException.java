package com.rhaen.tracker.common.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends TrackerException {
    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
