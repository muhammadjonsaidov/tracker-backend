package com.rhaen.tracker.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StreamTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public StreamToken issue(Jwt sourceJwt) {
        Instant now = Instant.now();
        Instant exp = now.plus(jwtProperties.streamTokenMinutes(), ChronoUnit.MINUTES);

        List<String> roles = sourceJwt.getClaimAsStringList("roles");
        if (roles == null) {
            roles = List.of();
        }

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiresAt(exp)
                .subject(sourceJwt.getSubject())
                .claim("uid", sourceJwt.getClaimAsString("uid"))
                .claim("roles", roles)
                .claim("typ", "stream")
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new StreamToken(token, exp);
    }

    public record StreamToken(String token, Instant expiresAt) {}
}
