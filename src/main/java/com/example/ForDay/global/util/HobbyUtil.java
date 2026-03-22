package com.example.ForDay.global.util;

import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HobbyUtil {
    private final HobbyRepository hobbyRepository;
    private final UserUtil userUtil;

    public Hobby getHobby(Long hobbyId) {
        return hobbyRepository.findById(hobbyId).orElseThrow(() -> new CustomException(ErrorCode.HOBBY_NOT_FOUND));
    }

    public Hobby getHobbyByUserId(Long hobbyId, User currentUser) {
        return hobbyRepository.findByIdAndUserId(hobbyId, currentUser.getId()).orElseThrow(() -> new CustomException(ErrorCode.HOBBY_NOT_FOUND));
    }

    public void verifyHobbyOwner(Hobby hobby, User currentUser) {
        if (!hobby.getUser().getId().equals(currentUser.getId())) {
            throw new CustomException(ErrorCode.NOT_HOBBY_OWNER);
        }
    }

    public Hobby checkHobbyUpdateable(Long hobbyId, CustomUserDetails user) {
        Hobby hobby = getHobby(hobbyId);
        User currentUser = userUtil.getCurrentUser(user);

        verifyHobbyOwner(hobby, currentUser);

        if (!hobby.isUpdatable()) {
            throw new CustomException(ErrorCode.INVALID_HOBBY_STATUS);
        }
        return hobby;
    }
}
