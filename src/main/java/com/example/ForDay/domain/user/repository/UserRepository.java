package com.example.ForDay.domain.user.repository;

import com.example.ForDay.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    User findBySocialId(String socialId);

    boolean existsByNickname(String nickname);

    boolean existsByNicknameAndIdNot(String nickname, String currentUserId);

    boolean existsBySocialId(String socialId);
}
