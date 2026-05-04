package com.forge.platform.controller;

import com.forge.platform.dto.UserCreateDto;
import com.forge.platform.dto.UserRequestDto;
import com.forge.platform.dto.UserResponseDto;
import com.forge.platform.entity.User;
import com.forge.platform.entity.Wallet;
import com.forge.platform.repository.WalletRepository; // 🔥 Added
import com.forge.platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final WalletRepository walletRepository; // 🔥 Added for live sync

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody UserCreateDto dto) {
        User user = User.builder()
                .email(dto.getEmail())
                .password(dto.getPassword())
                .fullName(dto.getFullName())
                .build();
        return ResponseEntity.ok(userService.registerUser(user));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getProfile(@AuthenticationPrincipal User user) {
        // 🔥 SENSEI FIX: Wallet table se live balance uthao, User entity se nahi!
        BigDecimal liveBalance = walletRepository.findByUser(user)
                .map(Wallet::getTotalBalance)
                .orElse(BigDecimal.ZERO);

        return ResponseEntity.ok(new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                liveBalance, // 👈 Passing live balance
                user.getCreatedAt()
        ));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponseDto> updateProfile(@AuthenticationPrincipal User user,
                                                         @RequestBody UserRequestDto dto) {
        return ResponseEntity.ok(userService.updateProfile(user, dto));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal User user,
                                               @RequestBody UserRequestDto dto) {
        userService.changePassword(user, dto);
        return ResponseEntity.ok().build();
    }
}