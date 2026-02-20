package com.rhaen.tracker.security.jwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreamTokenServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Test
    void issue_includesStreamClaims_andRoles() {
        StreamTokenService service = new StreamTokenService(jwtEncoder, new JwtProperties("12345678901234567890123456789012", "tracker", 60, 5));

        Jwt source = new Jwt("src", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "HS256"),
                Map.of("sub", "alice", "uid", "u1", "roles", List.of("ADMIN")));

        when(jwtEncoder.encode(any())).thenReturn(
                Jwt.withTokenValue("tkn")
                        .header("alg", "HS256")
                        .subject("alice")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(300))
                        .build()
        );

        StreamTokenService.StreamToken out = service.issue(source);

        ArgumentCaptor<JwtEncoderParameters> cap = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(cap.capture());

        assertThat(cap.getValue().getClaims().getClaimAsString("typ")).isEqualTo("stream");
        assertThat(cap.getValue().getClaims().getClaimAsStringList("roles")).containsExactly("ADMIN");
        assertThat(out.token()).isEqualTo("tkn");
        assertThat(out.expiresAt()).isAfter(Instant.now());
    }

    @Test
    void issue_setsEmptyRoles_whenMissing() {
        StreamTokenService service = new StreamTokenService(jwtEncoder, new JwtProperties("12345678901234567890123456789012", "tracker", 60, 5));

        Jwt source = new Jwt("src", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "HS256"),
                Map.of("sub", "alice", "uid", "u1"));

        when(jwtEncoder.encode(any())).thenReturn(
                Jwt.withTokenValue("tkn")
                        .header("alg", "HS256")
                        .subject("alice")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(300))
                        .build()
        );

        service.issue(source);

        ArgumentCaptor<JwtEncoderParameters> cap = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(cap.capture());
        assertThat(cap.getValue().getClaims().getClaimAsStringList("roles")).isEmpty();
    }
}
