package com.careeragent.api;

import com.careeragent.api.dto.AuthResponse;
import com.careeragent.api.dto.LoginRequest;
import com.careeragent.api.dto.RegisterRequest;
import com.careeragent.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController — Requirements 12.2, 12.6, 12.7")
class AuthControllerTest {

    @Mock private AuthService authService;

    @InjectMocks private AuthController authController;

    @Test
    @DisplayName("register returns 201 CREATED with a JWT token on success")
    void register_success_returnsCreatedWithToken() {
        var request = new RegisterRequest("new@example.com", "StrongPass1", "Alice");
        var authResponse = new AuthResponse("jwt-token", 86400000L);

        when(authService.register(request)).thenReturn(authResponse);

        var response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("jwt-token");
        assertThat(response.getBody().expiresIn()).isEqualTo(86400000L);
    }

    @Test
    @DisplayName("login returns 200 OK with a JWT token on valid credentials")
    void login_success_returnsOkWithToken() {
        var request = new LoginRequest("user@example.com", "CorrectPass1");
        var authResponse = new AuthResponse("login-token", 86400000L);

        when(authService.login(request)).thenReturn(authResponse);

        var response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("login-token");
    }
}
