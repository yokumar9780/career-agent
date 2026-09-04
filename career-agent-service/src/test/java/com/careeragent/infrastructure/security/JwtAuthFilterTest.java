package com.careeragent.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthFilter — Requirements 12.1, 12.3")
class JwtAuthFilterTest {

    private static final String SECRET = "test-secret-key-for-jwt-unit-tests-must-be-at-least-64-characters-long-for-hmac-sha512";

    private JwtTokenProvider tokenProvider;
    private JwtAuthFilter authFilter;

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(SECRET, 86400000L);
        authFilter = new JwtAuthFilter(tokenProvider);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("sets SecurityContext authentication when a valid Bearer token is present")
    void doFilter_setsAuthenticationWhenValidToken() throws Exception {
        UUID candidateId = UUID.randomUUID();
        String token = tokenProvider.generateToken(candidateId, "auth@example.com");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        authFilter.doFilterInternal(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(candidateId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("continues filter chain without setting auth when no Authorization header")
    void doFilter_continuesChainWhenNoAuthHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        authFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("continues filter chain without setting auth when token is invalid")
    void doFilter_continuesChainWhenInvalidToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid.token.here");

        authFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("ignores non-Bearer authorization schemes")
    void doFilter_continuesChainWhenNonBearerScheme() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        authFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("always calls filterChain.doFilter regardless of token validity")
    void doFilter_alwaysCallsFilterChain() throws Exception {
        // Case 1: valid token
        UUID candidateId = UUID.randomUUID();
        String token = tokenProvider.generateToken(candidateId, "chain@example.com");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        authFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(request, response);

        // Case 2: no token
        SecurityContextHolder.clearContext();
        reset(filterChain);
        when(request.getHeader("Authorization")).thenReturn(null);
        authFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(request, response);

        // Case 3: invalid token
        SecurityContextHolder.clearContext();
        reset(filterChain);
        when(request.getHeader("Authorization")).thenReturn("Bearer bad");
        authFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
