package com.forge.platform.controller;

import com.forge.platform.dto.AuthResponse;
import com.forge.platform.dto.LoginRequest;
import com.forge.platform.dto.UserCreateDto;
import com.forge.platform.entity.User;
import com.forge.platform.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody UserCreateDto request) {
        // Sirf request aayi aur service ko de di
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // Service token banayegi aur Controller usko frontend pe bhej dega
        return ResponseEntity.ok(authService.login(request));
    }
}