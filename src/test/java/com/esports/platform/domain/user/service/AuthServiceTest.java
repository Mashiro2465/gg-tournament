package com.esports.platform.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.esports.platform.domain.user.client.KakaoOAuthClient;
import com.esports.platform.domain.user.client.KakaoUserInfoResponse;
import com.esports.platform.domain.user.client.KakaoUserInfoResponse.KakaoAccount;
import com.esports.platform.domain.user.client.KakaoUserInfoResponse.KakaoProfile;
import com.esports.platform.domain.user.dto.TokenResponse;
import com.esports.platform.domain.user.entity.User;
import com.esports.platform.domain.user.entity.UserRole;
import com.esports.platform.global.auth.JwtProvider;
import com.esports.platform.global.auth.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private KakaoOAuthClient kakaoOAuthClient;

    @InjectMocks
    private AuthService authService;

    @Test
    void 카카오로그인_이메일과닉네임이있으면_그대로사용해회원조회및토큰발급() {
        KakaoUserInfoResponse userInfo = new KakaoUserInfoResponse(
                12345L, new KakaoAccount("kakao@example.com", new KakaoProfile("카카오유저", "http://image.url"))
        );
        User user = createUser(1L, UserRole.USER);
        when(kakaoOAuthClient.requestAccessToken("auth-code")).thenReturn("kakao-access-token");
        when(kakaoOAuthClient.requestUserInfo("kakao-access-token")).thenReturn(userInfo);
        when(userService.findOrCreateKakaoUser("12345", "kakao@example.com", "카카오유저", "http://image.url"))
                .thenReturn(user);
        when(jwtProvider.generateAccessToken(1L, UserRole.USER)).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken(1L, UserRole.USER)).thenReturn("refresh-token");

        TokenResponse result = authService.kakaoLogin("auth-code");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void 카카오로그인_이메일없으면_대체이메일로회원조회() {
        KakaoUserInfoResponse userInfo = new KakaoUserInfoResponse(
                999L, new KakaoAccount(null, new KakaoProfile("닉네임", null))
        );
        User user = createUser(2L, UserRole.USER);
        when(kakaoOAuthClient.requestAccessToken(any())).thenReturn("kakao-access-token");
        when(kakaoOAuthClient.requestUserInfo(any())).thenReturn(userInfo);
        when(userService.findOrCreateKakaoUser(eq("999"), eq("kakao_999@kakao.local"), eq("닉네임"), any()))
                .thenReturn(user);
        when(jwtProvider.generateAccessToken(any(), any())).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken(any(), any())).thenReturn("refresh-token");

        authService.kakaoLogin("auth-code");

        org.mockito.Mockito.verify(userService)
                .findOrCreateKakaoUser("999", "kakao_999@kakao.local", "닉네임", null);
    }

    @Test
    void 카카오로그인_카카오계정정보가없으면_대체이메일과대체닉네임사용() {
        KakaoUserInfoResponse userInfo = new KakaoUserInfoResponse(777L, null);
        User user = createUser(3L, UserRole.USER);
        when(kakaoOAuthClient.requestAccessToken(any())).thenReturn("kakao-access-token");
        when(kakaoOAuthClient.requestUserInfo(any())).thenReturn(userInfo);
        when(userService.findOrCreateKakaoUser(eq("777"), eq("kakao_777@kakao.local"), eq("카카오사용자777"), any()))
                .thenReturn(user);
        when(jwtProvider.generateAccessToken(any(), any())).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken(any(), any())).thenReturn("refresh-token");

        authService.kakaoLogin("auth-code");

        org.mockito.Mockito.verify(userService)
                .findOrCreateKakaoUser("777", "kakao_777@kakao.local", "카카오사용자777", null);
    }

    private User createUser(Long id, UserRole role) {
        User user = User.createKakaoUser("email@example.com", "닉네임", null, "kakao-id");
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "role", role);
        return user;
    }
}
