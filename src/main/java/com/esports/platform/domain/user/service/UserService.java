package com.esports.platform.domain.user.service;

import com.esports.platform.domain.user.entity.User;
import com.esports.platform.domain.user.repository.UserRepository;
import com.esports.platform.global.exception.BusinessException;
import com.esports.platform.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User signUp(String email, String rawPassword, String nickname) {
        validateDuplicateEmail(email);
        String encodedPassword = passwordEncoder.encode(rawPassword);
        return userRepository.save(User.createLocalUser(email, encodedPassword, nickname));
    }

    public User login(String email, String rawPassword) {
        User user = findByEmail(email);
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
        return user;
    }

    @Transactional
    public User findOrCreateKakaoUser(String kakaoId, String email, String nickname, String profileImage) {
        return userRepository.findByKakaoId(kakaoId)
                .orElseGet(() -> userRepository.save(User.createKakaoUser(email, nickname, profileImage, kakaoId)));
    }

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public void updateProfile(Long userId, String nickname, String profileImage) {
        User user = findById(userId);
        user.changeNickname(nickname);
        user.changeProfileImage(profileImage);
    }

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
    }
}
