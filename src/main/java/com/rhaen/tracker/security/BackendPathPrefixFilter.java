package com.rhaen.tracker.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class BackendPathPrefixFilter extends OncePerRequestFilter {

    private static final String PREFIX = "/backend";

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith(PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String stripped = uri.substring(PREFIX.length());
        if (stripped.isEmpty()) {
            stripped = "/";
        }

        String rewrittenPath = stripped;
        HttpServletRequest wrapped = new HttpServletRequestWrapper(request) {
            @Override
            public String getRequestURI() {
                return rewrittenPath;
            }

            @Override
            public String getServletPath() {
                return rewrittenPath;
            }

            @Override
            public StringBuffer getRequestURL() {
                return new StringBuffer(getScheme())
                        .append("://")
                        .append(getServerName())
                        .append(":")
                        .append(getServerPort())
                        .append(rewrittenPath);
            }
        };

        filterChain.doFilter(wrapped, response);
    }
}
