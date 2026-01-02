package com.example.ForDay.domain.user.repository;

import com.example.ForDay.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    User findBySocialId(String socialId);
}
