package com.rhaen.tracker.common.exception;

import org.springframework.http.HttpStatus;

public class ServiceUnavailableException extends TrackerException {
    public ServiceUnavailableException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
