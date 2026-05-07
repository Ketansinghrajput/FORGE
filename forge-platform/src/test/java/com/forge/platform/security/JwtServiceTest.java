package com.forge.platform.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // 🔥 SENSEI FIX: @Value fields ko mock karne ke liye ReflectionTestUtils
        // 256-bit Hex Key for HMAC-SHA256
        String dummySecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        ReflectionTestUtils.setField(jwtService, "secretKey", dummySecret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1000 * 60 * 60 * 24); // 1 Day

        userDetails = User.builder()
                .username("sensei@forge.com")
                .password("Barclays@2026")
                .authorities("USER")
                .build();
    }

    @Test
    void testGenerateAndValidateToken() {
        // Act: Generate Token
        String token = jwtService.generateToken(userDetails);

        // Assert: Extraction and Validation
        assertNotNull(token);
        String username = jwtService.extractUsername(token);
        assertEquals("sensei@forge.com", username);
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }
}