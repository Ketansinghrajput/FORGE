package com.forge.platform.service;

import com.forge.platform.dto.AuthResponse;
import com.forge.platform.dto.LoginRequest;
import com.forge.platform.dto.UserCreateDto;
import com.forge.platform.entity.User;
import com.forge.platform.enums.UserRole;
import com.forge.platform.repository.UserRepository;
import com.forge.platform.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(UserCreateDto request) {
        // 1. Naya user object banao
        var user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password())) // Password hash karna zaroori hai
                .role(UserRole.USER)
                .build();

        // 2. DB mein save karo
        userRepository.save(user);

        // 3. Token generate karo
        var jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken);
    }

    public AuthResponse login(LoginRequest request) {
        // 1. Spring Security se authenticate karwao
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        // 2. Agar yahan tak aaya matlab login sahi hai
        var user = userRepository.findByEmail(request.email())
                .orElseThrow();

        var jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken);
    }
}