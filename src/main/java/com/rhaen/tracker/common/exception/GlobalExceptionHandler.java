package com.rhaen.tracker.common.exception;

import com.rhaen.tracker.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.io.IOException;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(TrackerException.class)
        public ResponseEntity<ApiResponse<Void>> handleTrackerException(TrackerException ex) {
                return ResponseEntity.status(ex.getStatus())
                                .body(new ApiResponse<>(Instant.now(), ex.getMessage(), null));
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(new ApiResponse<>(Instant.now(), "Access denied: " + ex.getMessage(), null));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
                String msg = ex.getBindingResult().getFieldErrors().stream()
                                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                                .collect(Collectors.joining(", "));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(new ApiResponse<>(Instant.now(), "Validation failed: " + msg, null));
        }

        @ExceptionHandler(AsyncRequestNotUsableException.class)
        public void handleAsyncRequestNotUsable(AsyncRequestNotUsableException ex) {
                // Mijoz ulanishni uzganda (SSE/Async) sodir bo'ladi.
                // Hech narsa qilmaymiz, chunki ulanish allaqachon yopiq.
                log.debug("Client disconnected from async/SSE stream: {}", ex.getMessage());
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleGeneric(
                        Exception ex,
                        HttpServletRequest request) {
                String accept = request.getHeader("Accept");

                if (accept != null && accept.contains("text/event-stream")) {
                        if (ex instanceof IOException && isBrokenPipe(ex)) {
                                log.debug("SSE Connection closed by client (Broken pipe)");
                                return null;
                        }
                        log.error("Exception in SSE stream", ex);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new ApiResponse<>(Instant.now(), "Stream error", null));
                }

                log.error("Unhandled error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(new ApiResponse<>(Instant.now(), "An unexpected error occurred", null));
        }

        private boolean isBrokenPipe(Throwable ex) {
                String msg = ex.getMessage();
                if (msg == null)
                        return false;
                msg = msg.toLowerCase();
                return msg.contains("broken pipe") || msg.contains("connection reset by peer");
        }
}
