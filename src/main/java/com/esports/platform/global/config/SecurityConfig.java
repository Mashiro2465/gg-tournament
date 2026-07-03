package com.esports.platform.global.config;

import com.esports.platform.domain.user.service.UserService;
import com.esports.platform.global.auth.JwtAuthenticationEntryPoint;
import com.esports.platform.global.auth.JwtAuthenticationFilter;
import com.esports.platform.global.auth.JwtProvider;
import com.esports.platform.global.auth.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PERMIT_ALL_PATHS = {
            "/api/auth/signup",
            "/api/auth/login",
            "/api/auth/kakao",
            "/api/auth/refresh",
            "/api/payments/webhook"
    };

    private final UserService userService;
    private final JwtProvider jwtProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PERMIT_ALL_PATHS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/tournaments/*/settlements").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/tournaments/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider, userService, tokenBlacklistService),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
