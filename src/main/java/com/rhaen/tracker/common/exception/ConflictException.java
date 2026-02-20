package com.rhaen.tracker.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends TrackerException {
    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
