package com.careeragent.service;

import com.careeragent.api.dto.AuthResponse;
import com.careeragent.api.dto.LoginRequest;
import com.careeragent.api.dto.RegisterRequest;
import com.careeragent.api.exception.DuplicateEmailException;
import com.careeragent.domain.CandidateProfile;
import com.careeragent.infrastructure.security.JwtTokenProvider;
import com.careeragent.infrastructure.security.PasswordValidator;
import com.careeragent.repository.CandidateProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Handles candidate registration and authentication.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final CandidateProfileRepository candidateProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordValidator passwordValidator;

    /**
     * Registers a new candidate, hashes password, and generates JWT token.
     */
    public AuthResponse register(RegisterRequest request) {
        if (candidateProfileRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        passwordValidator.validate(request.password());

        CandidateProfile profile = CandidateProfile.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name())
                .build();

        profile = candidateProfileRepository.save(profile);

        String token = jwtTokenProvider.generateToken(profile.getId(), profile.getEmail());
        return new AuthResponse(token, jwtTokenProvider.getExpirationMs());
    }

    /**
     * Authenticates a candidate by email/password and generates JWT token.
     */
    public AuthResponse login(LoginRequest request) {
        CandidateProfile profile = candidateProfileRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), profile.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtTokenProvider.generateToken(profile.getId(), profile.getEmail());
        return new AuthResponse(token, jwtTokenProvider.getExpirationMs());
    }
}
