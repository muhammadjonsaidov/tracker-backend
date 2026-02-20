package com.rhaen.tracker.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.HandlerMethod;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlesTrackerException() {
        var resp = handler.handleTrackerException(new NotFoundException("not found"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(Objects.requireNonNull(resp.getBody()).message()).isEqualTo("not found");
    }

    @Test
    void handlesAccessDenied() {
        var resp = handler.handleAccessDenied(new org.springframework.security.access.AccessDeniedException("denied"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(Objects.requireNonNull(resp.getBody()).message()).contains("Access denied");
    }

    @Test
    void handlesValidationErrors() throws NoSuchMethodException {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "obj");
        binding.addError(new FieldError("obj", "lat", "invalid"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                new org.springframework.core.MethodParameter(
                        new HandlerMethod(this, this.getClass().getDeclaredMethod("dummy", String.class)).getMethod(),
                        0),
                binding);

        var resp = handler.handleValidation(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(resp.getBody()).message()).contains("lat");
    }

    @SuppressWarnings("unused")
    private void dummy(String input) {
    }

    @Test
    void handleGeneric_returnsSseErrorForEventStream() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Accept")).thenReturn("text/event-stream");

        var resp = handler.handleGeneric(new RuntimeException("boom"), req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(Objects.requireNonNull(resp.getBody()).message()).isEqualTo("Stream error");
    }

    @Test
    void handleGeneric_returnsInternalErrorForNormalRequests() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Accept")).thenReturn("application/json");
        when(req.getRequestURI()).thenReturn("/api/test");

        var resp = handler.handleGeneric(new RuntimeException("boom"), req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(Objects.requireNonNull(resp.getBody()).message()).isEqualTo("An unexpected error occurred");
    }
}
