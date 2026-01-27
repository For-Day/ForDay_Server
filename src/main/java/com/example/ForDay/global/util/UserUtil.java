package com.example.ForDay.global.util;

import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class UserUtil {
    private final UserRepository userRepository;
    private final EntityManager em; // Repository 대신 EntityManager 주입

    public User getCurrentUser(CustomUserDetails user) {
        //return userRepository.findBySocialId(user.getUsername());
        return user.getUser();
    }

}
