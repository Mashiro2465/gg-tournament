package com.esports.platform.domain.user.dto;

import com.esports.platform.domain.user.entity.User;
import com.esports.platform.domain.user.entity.UserRole;

public record UserResponse(
        Long id,
        String email,
        String nickname,
        String profileImage,
        UserRole role
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImage(),
                user.getRole()
        );
    }
}
