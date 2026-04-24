package com.forge.platform.dto;

public record UserCreateDto(
        String email,
        String password,
        String fullName
) {}