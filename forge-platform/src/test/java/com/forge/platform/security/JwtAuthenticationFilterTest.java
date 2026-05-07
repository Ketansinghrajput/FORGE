package com.forge.platform.security;

import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks private JwtAuthenticationFilter jwtFilter;

    @AfterEach
    void tearDown() {
        // Har test ke baad context clear karna zaroori hai
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_ShouldSkipAuthEndpoints() throws Exception {
        when(request.getServletPath()).thenReturn("/api/v1/auth/login");

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void doFilterInternal_ShouldSkipIfNoBearerToken() throws Exception {
        when(request.getServletPath()).thenReturn("/api/v1/secure");
        when(request.getHeader("Authorization")).thenReturn("Basic user:pass");

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void doFilterInternal_ShouldAuthenticateValidToken() throws Exception {
        String token = "valid_token";
        UserDetails userDetails = User.builder()
                .username("sensei@forge.com")
                .password("pass")
                .authorities("USER")
                .build();

        when(request.getServletPath()).thenReturn("/api/v1/secure");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenReturn("sensei@forge.com");
        when(userDetailsService.loadUserByUsername("sensei@forge.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_ShouldCatchJwtExceptionsAndProceed() throws Exception {
        String token = "tampered_token";

        when(request.getServletPath()).thenReturn("/api/v1/secure");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractUsername(token)).thenThrow(new SignatureException("Tampered token"));

        jwtFilter.doFilterInternal(request, response, filterChain);

        // Security context null rehna chahiye, par filter chain call honi chahiye
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}