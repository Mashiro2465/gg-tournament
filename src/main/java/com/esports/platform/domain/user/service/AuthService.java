package com.esports.platform.domain.user.service;

import com.esports.platform.domain.user.client.KakaoOAuthClient;
import com.esports.platform.domain.user.client.KakaoUserInfoResponse;
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
    private final KakaoOAuthClient kakaoOAuthClient;

    public User signUp(String email, String password, String nickname) {
        return userService.signUp(email, password, nickname);
    }

    public TokenResponse login(String email, String password) {
        User user = userService.login(email, password);
        return issueTokens(user);
    }

    public TokenResponse kakaoLogin(String authorizationCode) {
        String kakaoAccessToken = kakaoOAuthClient.requestAccessToken(authorizationCode);
        KakaoUserInfoResponse userInfo = kakaoOAuthClient.requestUserInfo(kakaoAccessToken);

        String kakaoId = String.valueOf(userInfo.id());
        KakaoUserInfoResponse.KakaoAccount account = userInfo.kakaoAccount();
        KakaoUserInfoResponse.KakaoProfile profile = account != null ? account.profile() : null;

        String email = account != null && account.email() != null ? account.email() : "kakao_" + kakaoId + "@kakao.local";
        String nickname = profile != null && profile.nickname() != null ? profile.nickname() : "카카오사용자" + kakaoId;
        String profileImage = profile != null ? profile.profileImageUrl() : null;

        User user = userService.findOrCreateKakaoUser(kakaoId, email, nickname, profileImage);
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
