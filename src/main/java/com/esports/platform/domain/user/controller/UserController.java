package com.esports.platform.domain.user.controller;

import com.esports.platform.domain.user.dto.UpdateProfileRequest;
import com.esports.platform.domain.user.dto.UserResponse;
import com.esports.platform.domain.user.entity.User;
import com.esports.platform.domain.user.service.UserService;
import com.esports.platform.global.auth.UserPrincipal;
import com.esports.platform.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = userService.findById(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(UserResponse.from(user)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        userService.updateProfile(userPrincipal.getId(), request.nickname(), request.profileImage());
        User user = userService.findById(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(UserResponse.from(user), "프로필이 수정되었습니다"));
    }
}
