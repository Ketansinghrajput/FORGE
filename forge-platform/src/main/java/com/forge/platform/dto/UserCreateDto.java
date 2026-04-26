package com.forge.platform.dto;

import lombok.Data;

@Data
public class UserCreateDto {
    private String email;
    private String password;
    private String fullName;
}