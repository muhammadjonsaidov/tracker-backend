package com.rhaen.tracker.feature.auth.command;

import com.rhaen.tracker.common.exception.BadRequestException;
import com.rhaen.tracker.feature.auth.dto.AuthDtos;
import com.rhaen.tracker.feature.user.persistence.UserEntity;
import com.rhaen.tracker.feature.user.persistence.UserRepository;
import com.rhaen.tracker.security.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthCommandService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    @Transactional
    public void register(AuthDtos.RegisterRequest req) {
        if (userRepository.findByUsername(req.username()).isPresent()) {
            throw new BadRequestException("Username already taken");
        }
        if (userRepository.findByEmail(req.email()).isPresent()) {
            throw new BadRequestException("Email already taken");
        }

        UserEntity user = UserEntity.builder()
                .username(req.username())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(UserEntity.Role.USER)
                .build();

        userRepository.save(user);
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
        UserEntity user = userRepository.findByUsername(req.usernameOrEmail())
                .or(() -> userRepository.findByEmail(req.usernameOrEmail()))
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid credentials");
        }

        String token = generateAccessToken(user);
        return new AuthDtos.AuthResponse(token, "Bearer");
    }

    private String generateAccessToken(UserEntity user) {
        Instant now = Instant.now();
        Instant exp = now.plus(jwtProperties.accessTokenMinutes(), ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiresAt(exp)
                .subject(user.getUsername())
                .claim("uid", user.getId().toString())
                .claim("roles", List.of(user.getRole().name()))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
