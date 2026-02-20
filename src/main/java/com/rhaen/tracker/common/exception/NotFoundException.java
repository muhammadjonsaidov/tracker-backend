package com.rhaen.tracker.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends TrackerException {
    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
