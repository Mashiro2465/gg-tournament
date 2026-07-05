package com.esports.platform.domain.user.repository;

import com.esports.platform.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByKakaoId(String kakaoId);

    boolean existsByEmail(String email);
}
