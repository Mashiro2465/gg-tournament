package com.esports.platform.domain.user.service;

import com.esports.platform.domain.user.dto.TokenResponse;
import com.esports.platform.domain.user.entity.User;
import com.esports.platform.global.auth.JwtProvider;
import com.esports.platform.global.auth.TokenBlacklistService;
import com.esports.platform.global.exception.BusinessException;
import com.esports.platform.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtProvider jwtProvider;
    private final TokenBlacklistService tokenBlacklistService;

    public User signUp(String email, String password, String nickname) {
        return userService.signUp(email, password, nickname);
    }

    public TokenResponse login(String email, String password) {
        User user = userService.login(email, password);
        return issueTokens(user);
    }

    public TokenResponse refresh(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken) || tokenBlacklistService.isBlacklisted(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        User user = userService.findById(jwtProvider.getUserId(refreshToken));
        return issueTokens(user);
    }

    public void logout(String accessToken) {
        if (!jwtProvider.validateToken(accessToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        tokenBlacklistService.blacklist(accessToken, jwtProvider.getRemainingMillis(accessToken));
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getRole());
        return new TokenResponse(accessToken, refreshToken);
    }
}
