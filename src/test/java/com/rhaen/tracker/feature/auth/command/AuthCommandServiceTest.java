package com.rhaen.tracker.feature.auth.command;

import com.rhaen.tracker.common.audit.AuditService;
import com.rhaen.tracker.common.exception.BadRequestException;
import com.rhaen.tracker.feature.auth.dto.AuthDtos;
import com.rhaen.tracker.feature.user.persistence.UserEntity;
import com.rhaen.tracker.feature.user.persistence.UserRepository;
import com.rhaen.tracker.security.jwt.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthCommandServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtEncoder jwtEncoder;
    @Mock
    private AuditService auditService;

    private AuthCommandService service;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties(
                "0123456789abcdef0123456789abcdef",
                "tracker-backend",
                60,
                5
        );
        service = new AuthCommandService(userRepository, passwordEncoder, jwtEncoder, props, auditService);
    }

    @Test
    void register_success() {
        var req = new AuthDtos.RegisterRequest("user1", "u1@mail.com", "secret");
        when(userRepository.findByUsername("user1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("u1@mail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("hashed");

        UUID id = UUID.randomUUID();
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            u.setId(id);
            return u;
        });

        service.register(req);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("user1");
        assertThat(captor.getValue().getEmail()).isEqualTo("u1@mail.com");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(captor.getValue().getRole()).isEqualTo(UserEntity.Role.USER);
        verify(auditService).logUserAction(any(), any(), any(), any(), any());
    }

    @Test
    void register_duplicateUsername_throws() {
        var req = new AuthDtos.RegisterRequest("user1", "u1@mail.com", "secret");
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(new UserEntity()));

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    void register_duplicateEmail_throws() {
        var req = new AuthDtos.RegisterRequest("user1", "u1@mail.com", "secret");
        when(userRepository.findByUsername("user1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("u1@mail.com")).thenReturn(Optional.of(new UserEntity()));

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email already taken");
    }

    @Test
    void login_success_returnsBearerToken() {
        UUID id = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .id(id)
                .username("user1")
                .email("u1@mail.com")
                .passwordHash("hashed")
                .role(UserEntity.Role.USER)
                .build();
        var req = new AuthDtos.LoginRequest("user1", "secret");

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                java.util.Map.of("alg", "HS256", "typ", "JWT"),
                java.util.Map.of("uid", id.toString(), "roles", List.of("USER"))
        );
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);

        var resp = service.login(req);

        assertThat(resp.accessToken()).isEqualTo("mock-token");
        assertThat(resp.tokenType()).isEqualTo("Bearer");
        verify(auditService).logUserAction(any(), any(), any(), any(), any());
    }

    @Test
    void login_userNotFound_throws() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new AuthDtos.LoginRequest("missing", "secret")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void login_invalidPassword_throws() {
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .username("user1")
                .passwordHash("hashed")
                .role(UserEntity.Role.USER)
                .build();
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new AuthDtos.LoginRequest("user1", "bad")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void login_generatesJwtWithExpectedClaims() {
        UUID id = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .id(id)
                .username("user1")
                .email("u1@mail.com")
                .passwordHash("hashed")
                .role(UserEntity.Role.ADMIN)
                .build();
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenAnswer(inv -> {
            JwtEncoderParameters p = inv.getArgument(0);
            JwtClaimsSet claims = p.getClaims();
            assertThat(claims.getSubject()).isEqualTo("user1");
            assertThat((Object) claims.getClaim("uid")).isEqualTo(id.toString());
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.getClaim("roles");
            assertThat(roles).contains("ADMIN");
            JwsHeader header = p.getJwsHeader();
            assertThat(header.getAlgorithm().getName()).isEqualTo(MacAlgorithm.HS256.getName());
            return new Jwt(
                    "token",
                    Instant.now(),
                    Instant.now().plusSeconds(300),
                    java.util.Map.of("alg", "HS256"),
                    java.util.Map.of("sub", "user1")
            );
        });

        service.login(new AuthDtos.LoginRequest("user1", "secret"));
    }
}
