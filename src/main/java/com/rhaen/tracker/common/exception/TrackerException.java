package com.rhaen.tracker.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class TrackerException extends RuntimeException {
    private final HttpStatus status;

    protected TrackerException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
