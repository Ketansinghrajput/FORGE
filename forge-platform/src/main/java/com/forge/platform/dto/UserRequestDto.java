package com.forge.platform.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserRequestDto {
    private String fullName;
    private String currentPassword;
    private String newPassword;
}