package com.example.employeemanagementsystem.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Duration tokenLifetime;
    private final String issuer;

    public AuthService(
            UserAccountRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            @Value("${app.security.jwt.expiration-minutes:60}") long expirationMinutes,
            @Value("${app.security.jwt.issuer:employee-management-system}") String issuer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.tokenLifetime = Duration.ofMinutes(expirationMinutes);
        this.issuer = issuer;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An account with this email already exists");
        }

        UserAccount account = new UserAccount();
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setRole(UserRole.USER);
        return issueToken(userRepository.save(account));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        UserAccount account = userRepository.findByEmailIgnoreCase(
                        normalizeEmail(request.email()))
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw invalidCredentials();
        }
        return issueToken(account);
    }

    @Transactional
    public void createBootstrapAdmin(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        if (!userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            UserAccount admin = new UserAccount();
            admin.setEmail(normalizedEmail);
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setRole(UserRole.ADMIN);
            userRepository.save(admin);
        }
    }

    private TokenResponse issueToken(UserAccount account) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(tokenLifetime))
                .subject(account.getEmail())
                .claim("role", account.getRole().name())
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", tokenLifetime.toSeconds(),
                account.getRole().name());
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Invalid email or password");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
