package com.esports.platform.global.auth;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "blacklist:token:";

    private final RedisTemplate<String, String> redisTemplate;

    public void blacklist(String token, long ttlMillis) {
        if (ttlMillis > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + token, "logout", Duration.ofMillis(ttlMillis));
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + token));
    }
}
