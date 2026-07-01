package com.esports.platform.domain.user.controller;

import com.esports.platform.domain.user.dto.LoginRequest;
import com.esports.platform.domain.user.dto.RefreshTokenRequest;
import com.esports.platform.domain.user.dto.SignUpRequest;
import com.esports.platform.domain.user.dto.TokenResponse;
import com.esports.platform.domain.user.dto.UserResponse;
import com.esports.platform.domain.user.entity.User;
import com.esports.platform.domain.user.service.AuthService;
import com.esports.platform.global.common.ApiResponse;
import com.esports.platform.global.exception.BusinessException;
import com.esports.platform.global.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponse>> signUp(@Valid @RequestBody SignUpRequest request) {
        User user = authService.signUp(request.email(), request.password(), request.nickname());
        return ResponseEntity.ok(ApiResponse.success(UserResponse.from(user), "회원가입이 완료되었습니다"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request.email(), request.password());
        return ResponseEntity.ok(ApiResponse.success(tokenResponse, "로그인이 완료되었습니다"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse tokenResponse = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(tokenResponse, "토큰이 재발급되었습니다"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authorizationHeader) {
        authService.logout(resolveToken(authorizationHeader));
        return ResponseEntity.ok(ApiResponse.success(null, "로그아웃이 완료되었습니다"));
    }

    private String resolveToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }
}
