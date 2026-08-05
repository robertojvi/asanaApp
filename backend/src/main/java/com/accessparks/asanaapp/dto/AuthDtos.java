package com.accessparks.asanaapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {

    public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
    ) {}

    public record LoginResponse(
        String token,
        String email,
        String role,
        String fullName
    ) {}

    public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        String fullName,
        @NotBlank String role // SUPER_USER | ADMIN | USER
    ) {}

    public record UpdateUserRequest(
        String fullName,
        String role,
        Boolean enabled,
        String newPassword // optional - only set if changing password
    ) {}
}
