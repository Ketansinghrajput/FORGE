package com.forge.platform.dto;

public record AuthResponse(
        String id,
        String token,
        String email,
        String fullName,
        String role
) {}