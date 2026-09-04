package com.careeragent.api;

import com.careeragent.api.dto.AuthResponse;
import com.careeragent.api.dto.LoginRequest;
import com.careeragent.api.dto.RegisterRequest;
import com.careeragent.api.exception.DuplicateEmailException;
import com.careeragent.api.exception.PasswordValidationException;
import com.careeragent.domain.CandidateProfile;
import com.careeragent.infrastructure.security.JwtTokenProvider;
import com.careeragent.infrastructure.security.PasswordValidator;
import com.careeragent.repository.CandidateProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController — Requirements 12.2, 12.6, 12.7")
class AuthControllerTest {

    @Mock private CandidateProfileRepository repository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordValidator passwordValidator;

    @InjectMocks private AuthController authController;

    @Test
    @DisplayName("register returns 201 CREATED with a JWT token on success")
    void register_success_returnsCreatedWithToken() {
        var request = new RegisterRequest("new@example.com", "StrongPass1", "Alice");
        UUID profileId = UUID.randomUUID();
        CandidateProfile saved = CandidateProfile.builder()
                .id(profileId)
                .email("new@example.com")
                .passwordHash("hashed")
                .name("Alice")
                .build();

        when(repository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass1")).thenReturn("hashed");
        when(repository.save(any(CandidateProfile.class))).thenReturn(saved);
        when(jwtTokenProvider.generateToken(profileId, "new@example.com")).thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        var response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("jwt-token");
        assertThat(response.getBody().expiresIn()).isEqualTo(86400000L);
    }

    @Test
    @DisplayName("register throws DuplicateEmailException when email already exists")
    void register_duplicateEmail_throwsDuplicateEmailException() {
        var request = new RegisterRequest("taken@example.com", "StrongPass1", "Bob");
        when(repository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authController.register(request))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    @DisplayName("register propagates PasswordValidationException for weak passwords")
    void register_weakPassword_throwsPasswordValidationException() {
        var request = new RegisterRequest("weak@example.com", "short", "Carol");
        when(repository.existsByEmail("weak@example.com")).thenReturn(false);
        doThrow(new PasswordValidationException(List.of("Password must be between 8 and 128 characters")))
                .when(passwordValidator).validate("short");

        assertThatThrownBy(() -> authController.register(request))
                .isInstanceOf(PasswordValidationException.class);
    }

    @Test
    @DisplayName("register hashes the password before saving (never stores plaintext)")
    void register_hashesPasswordBeforeSaving() {
        var request = new RegisterRequest("hash@example.com", "MyPassword1", "Dave");
        UUID profileId = UUID.randomUUID();
        CandidateProfile saved = CandidateProfile.builder()
                .id(profileId)
                .email("hash@example.com")
                .passwordHash("bcrypt-hash")
                .name("Dave")
                .build();

        when(repository.existsByEmail("hash@example.com")).thenReturn(false);
        when(passwordEncoder.encode("MyPassword1")).thenReturn("bcrypt-hash");
        when(repository.save(any(CandidateProfile.class))).thenReturn(saved);
        when(jwtTokenProvider.generateToken(any(), any())).thenReturn("token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        authController.register(request);

        ArgumentCaptor<CandidateProfile> captor = ArgumentCaptor.forClass(CandidateProfile.class);
        verify(repository).save(captor.capture());
        CandidateProfile captured = captor.getValue();

        assertThat(captured.getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(captured.getPasswordHash()).isNotEqualTo("MyPassword1");
    }

    @Test
    @DisplayName("login returns 200 OK with a JWT token on valid credentials")
    void login_success_returnsOkWithToken() {
        var request = new LoginRequest("user@example.com", "CorrectPass1");
        UUID profileId = UUID.randomUUID();
        CandidateProfile profile = CandidateProfile.builder()
                .id(profileId)
                .email("user@example.com")
                .passwordHash("stored-hash")
                .name("Eve")
                .build();

        when(repository.findByEmail("user@example.com")).thenReturn(Optional.of(profile));
        when(passwordEncoder.matches("CorrectPass1", "stored-hash")).thenReturn(true);
        when(jwtTokenProvider.generateToken(profileId, "user@example.com")).thenReturn("login-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        var response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("login-token");
    }

    @Test
    @DisplayName("login throws BadCredentialsException when email is not found")
    void login_wrongEmail_throwsBadCredentials() {
        var request = new LoginRequest("unknown@example.com", "AnyPass1");
        when(repository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authController.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("login throws BadCredentialsException when password does not match")
    void login_wrongPassword_throwsBadCredentials() {
        var request = new LoginRequest("user@example.com", "WrongPass1");
        CandidateProfile profile = CandidateProfile.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("stored-hash")
                .name("Frank")
                .build();

        when(repository.findByEmail("user@example.com")).thenReturn(Optional.of(profile));
        when(passwordEncoder.matches("WrongPass1", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> authController.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
