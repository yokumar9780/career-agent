package com.careeragent.api;

import com.careeragent.api.dto.*;
import com.careeragent.api.dto.LoginRequest;
import com.careeragent.api.dto.RegisterRequest;
import com.careeragent.api.exception.DuplicateEmailException;
import com.careeragent.domain.CandidateProfile;
import com.careeragent.infrastructure.security.JwtTokenProvider;
import com.careeragent.infrastructure.security.PasswordValidator;
import com.careeragent.repository.CandidateProfileRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final CandidateProfileRepository candidateProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordValidator passwordValidator;

    public AuthController(CandidateProfileRepository candidateProfileRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider jwtTokenProvider,
                          PasswordValidator passwordValidator) {
        this.candidateProfileRepository = candidateProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordValidator = passwordValidator;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
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

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token, jwtTokenProvider.getExpirationMs()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        CandidateProfile profile = candidateProfileRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), profile.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtTokenProvider.generateToken(profile.getId(), profile.getEmail());

        return ResponseEntity.ok(new AuthResponse(token, jwtTokenProvider.getExpirationMs()));
    }
}
