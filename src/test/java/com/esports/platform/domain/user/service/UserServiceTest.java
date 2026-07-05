package com.esports.platform.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.esports.platform.domain.user.entity.User;
import com.esports.platform.domain.user.repository.UserRepository;
import com.esports.platform.global.exception.BusinessException;
import com.esports.platform.global.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void 회원가입_이메일중복시_예외발생() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.signUp("test@example.com", "password1!", "닉네임"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    void 회원가입_정상요청시_회원저장() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1!")).thenReturn("encoded-password");
        User savedUser = User.createLocalUser("test@example.com", "encoded-password", "닉네임");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.signUp("test@example.com", "password1!", "닉네임");

        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getNickname()).isEqualTo("닉네임");
    }

    @Test
    void 로그인_존재하지않는이메일이면_예외발생() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login("unknown@example.com", "password1!"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void 로그인_비밀번호불일치시_예외발생() {
        User user = User.createLocalUser("test@example.com", "encoded-password", "닉네임");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> userService.login("test@example.com", "wrong-password"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PASSWORD);
    }

    @Test
    void 카카오회원조회_이미가입된카카오id면_기존회원반환() {
        User existing = User.createKakaoUser("kakao@example.com", "닉네임", null, "kakao-id-1");
        when(userRepository.findByKakaoId("kakao-id-1")).thenReturn(Optional.of(existing));

        User result = userService.findOrCreateKakaoUser("kakao-id-1", "kakao@example.com", "닉네임", null);

        assertThat(result).isEqualTo(existing);
        verify(userRepository, never()).save(any());
    }

    @Test
    void 카카오회원조회_신규카카오id면_회원가입후반환() {
        when(userRepository.findByKakaoId("kakao-id-2")).thenReturn(Optional.empty());
        User saved = User.createKakaoUser("kakao2@example.com", "닉네임2", "http://image.url", "kakao-id-2");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        User result = userService.findOrCreateKakaoUser("kakao-id-2", "kakao2@example.com", "닉네임2", "http://image.url");

        assertThat(result).isEqualTo(saved);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void 회원조회_존재하지않는id면_예외발생() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
