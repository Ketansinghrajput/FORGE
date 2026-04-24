package com.forge.platform.controller;

import com.forge.platform.dto.UserCreateDto;
import com.forge.platform.dto.UserResponseDto;
import com.forge.platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(@RequestBody UserCreateDto dto) {
        UserResponseDto response = userService.registerUser(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}