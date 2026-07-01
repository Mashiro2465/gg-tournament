package com.esports.platform.domain.user.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
