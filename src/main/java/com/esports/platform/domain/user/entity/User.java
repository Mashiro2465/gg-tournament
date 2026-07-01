package com.esports.platform.domain.user.entity;

import com.esports.platform.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "kakao_id", length = 100)
    private String kakaoId;

    @Builder
    private User(String email, String password, String nickname, String profileImage, UserRole role, String kakaoId) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.profileImage = profileImage;
        this.role = role != null ? role : UserRole.USER;
        this.kakaoId = kakaoId;
    }

    public static User createLocalUser(String email, String encodedPassword, String nickname) {
        return User.builder()
                .email(email)
                .password(encodedPassword)
                .nickname(nickname)
                .role(UserRole.USER)
                .build();
    }

    public static User createKakaoUser(String email, String nickname, String profileImage, String kakaoId) {
        return User.builder()
                .email(email)
                .nickname(nickname)
                .profileImage(profileImage)
                .kakaoId(kakaoId)
                .role(UserRole.USER)
                .build();
    }

    public boolean isKakaoUser() {
        return this.kakaoId != null;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changeProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void linkKakaoAccount(String kakaoId) {
        this.kakaoId = kakaoId;
    }
}
