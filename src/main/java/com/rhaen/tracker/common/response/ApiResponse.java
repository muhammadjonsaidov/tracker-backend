package com.rhaen.tracker.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        @Schema(description = "Response timestamp (UTC)")
        Instant timestamp,
        @Schema(description = "Response message")
        String message,
        @Schema(description = "Response data")
        T data
) {
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(Instant.now(), message, data);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok("OK", data);
    }
}
