package com.rhaen.tracker.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Configuration
public class SwaggerPathCompatibilityConfig {

    @RestController
    static class SwaggerPathCompatibilityController {

        @GetMapping({"/backend/swagger-ui", "/backend/swagger-ui/", "/backend/swagger-ui/**", "/backend/swagger-ui.html"})
        ResponseEntity<Void> redirectBackendSwaggerUi(HttpServletRequest request) {
            return redirectByRemovingBackendPrefix(request);
        }

        @GetMapping("/backend/v3/api-docs/**")
        ResponseEntity<Void> redirectBackendApiDocs(HttpServletRequest request) {
            return redirectByRemovingBackendPrefix(request);
        }

        private ResponseEntity<Void> redirectByRemovingBackendPrefix(HttpServletRequest request) {
            String target = request.getRequestURI().replaceFirst("^/backend", "");
            String query = request.getQueryString();
            if (query != null && !query.isBlank()) {
                target = target + "?" + query;
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, URI.create(target).toString())
                    .build();
        }
    }
}
