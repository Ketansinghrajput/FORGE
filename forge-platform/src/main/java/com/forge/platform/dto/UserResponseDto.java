package com.forge.platform.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserResponseDto(
        Long id,
        String email,
        String fullName,
        BigDecimal walletBalance,
        LocalDateTime createdAt
) {}