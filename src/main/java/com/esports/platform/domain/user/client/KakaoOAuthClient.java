package com.esports.platform.domain.user.client;

import com.esports.platform.global.exception.BusinessException;
import com.esports.platform.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class KakaoOAuthClient {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public KakaoOAuthClient(
            @Value("${kakao.client-id}") String clientId,
            @Value("${kakao.client-secret:}") String clientSecret,
            @Value("${kakao.redirect-uri}") String redirectUri
    ) {
        this.restClient = RestClient.create();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    public String requestAccessToken(String authorizationCode) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("redirect_uri", redirectUri);
        body.add("code", authorizationCode);
        if (StringUtils.hasText(clientSecret)) {
            body.add("client_secret", clientSecret);
        }

        try {
            KakaoTokenResponse response = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(KakaoTokenResponse.class);
            return response.accessToken();
        } catch (RestClientResponseException e) {
            log.error("카카오 액세스 토큰 발급 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.KAKAO_LOGIN_FAILED);
        }
    }

    public KakaoUserInfoResponse requestUserInfo(String kakaoAccessToken) {
        try {
            return restClient.get()
                    .uri(USER_INFO_URL)
                    .headers(headers -> headers.setBearerAuth(kakaoAccessToken))
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);
        } catch (RestClientResponseException e) {
            log.error("카카오 사용자 정보 조회 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.KAKAO_LOGIN_FAILED);
        }
    }
}
