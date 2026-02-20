package com.rhaen.tracker.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SseQueryTokenAuthFilterTest {

    @Mock
    private JwtDecoder jwtDecoder;
    @Mock
    private JwtAuthenticationConverter converter;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void skips_nonSseAdminUri() throws ServletException, IOException {
        SseQueryTokenAuthFilter filter = new SseQueryTokenAuthFilter(jwtDecoder, converter);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/tracking/sessions");
        req.setServletPath("/api/v1/tracking/sessions");

        filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

        verifyNoInteractions(jwtDecoder, converter);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void setsAuth_whenStreamTokenIsValid() throws ServletException, IOException {
        SseQueryTokenAuthFilter filter = new SseQueryTokenAuthFilter(jwtDecoder, converter);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/admin/stream/last-locations");
        req.setServletPath("/api/v1/admin/stream/last-locations");
        req.setParameter("access_token", "abc");

        Jwt jwt = new Jwt("abc", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "HS256"), Map.of("typ", "stream", "sub", "admin"));
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList("ROLE_ADMIN"));
        when(jwtDecoder.decode("abc")).thenReturn(jwt);
        when(converter.convert(jwt)).thenReturn(auth);

        filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(jwtDecoder).decode("abc");
        verify(converter).convert(jwt);
    }

    @Test
    void doesNotSetAuth_whenTypIsNotStream() throws ServletException, IOException {
        SseQueryTokenAuthFilter filter = new SseQueryTokenAuthFilter(jwtDecoder, converter);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/admin/stream/last-locations");
        req.setServletPath("/api/v1/admin/stream/last-locations");
        req.setParameter("token", "abc");

        Jwt jwt = new Jwt("abc", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "HS256"), Map.of("typ", "access", "sub", "admin"));
        when(jwtDecoder.decode(anyString())).thenReturn(jwt);

        filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtDecoder).decode("abc");
        verifyNoInteractions(converter);
    }
}
