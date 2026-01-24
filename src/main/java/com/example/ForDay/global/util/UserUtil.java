package com.example.ForDay.global.util;

import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.global.oauth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserUtil {
    private final UserRepository userRepository;

    public User getCurrentUser(CustomUserDetails user) {
        return user.getUser();
    }
}
