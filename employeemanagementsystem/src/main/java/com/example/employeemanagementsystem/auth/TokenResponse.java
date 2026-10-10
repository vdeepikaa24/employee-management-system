package com.example.employeemanagementsystem.auth;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String role) {
}
