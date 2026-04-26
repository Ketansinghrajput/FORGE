package com.forge.platform.controller;

import com.forge.platform.dto.UserCreateDto;
import com.forge.platform.entity.User;
import com.forge.platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody UserCreateDto dto) {
        // ERROR FIX: DTO ko Entity mein map kar rahe hain builder se
        User user = User.builder()
                .email(dto.getEmail())
                .password(dto.getPassword())
                .fullName(dto.getFullName())
                .build();

        return ResponseEntity.ok(userService.registerUser(user));
    }
}