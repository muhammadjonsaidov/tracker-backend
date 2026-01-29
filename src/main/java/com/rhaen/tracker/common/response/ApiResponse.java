package com.rhaen.tracker.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        Instant timestamp,
        String message,
        T data
) {
    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .timestamp(Instant.now())
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok("OK", data);
    }
}
