package com.example.ForDay.domain.user.service;

import com.example.ForDay.domain.user.dto.response.NicknameCheckResDto;
import com.example.ForDay.domain.user.dto.response.NicknameRegisterResDto;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getUserBySocialId(String id) {
        return userRepository.findBySocialId(id);
    }

    @Transactional
    public User createOauth(String socialId, String email, SocialType socialType) {
        return userRepository.save(User.builder()
                .role(Role.USER)
                .email(email)
                .socialType(socialType)
                .socialId(socialId)
                .build());
    }

    @Transactional
    public NicknameCheckResDto nicknameCheck(String nickname) {

        // 길이 체크 (10자 초과)
        if (nickname.length() > 10) {
            return new NicknameCheckResDto(
                    nickname,
                    false,
                    "닉네임은 10자 이내로 입력해주세요."
            );
        }

        // 허용 문자 체크 (한글, 영어, 숫자만 허용)
        if (!nickname.matches("^[가-힣a-zA-Z0-9]+$")) {
            return new NicknameCheckResDto(
                    nickname,
                    false,
                    "한글, 영어, 숫자만 사용할 수 있습니다."
            );
        }

        // DB 중복 체크
        boolean exists = userRepository.existsByNickname(nickname);

        if (exists) {
            return new NicknameCheckResDto(
                    nickname,
                    false,
                    "이미 사용 중인 닉네임입니다."
            );
        }

        // 사용 가능
        return new NicknameCheckResDto(
                nickname,
                true,
                "사용 가능한 닉네임입니다."
        );
    }

    @Transactional
    public NicknameRegisterResDto nicknameRegister(String nickname, CustomUserDetails user) {
        boolean exists = userRepository.existsByNickname(nickname);
        if(exists) {
            throw new CustomException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        User currentUser = userRepository.findBySocialId(user.getUsername());
        currentUser.changeNickname(nickname);

        return new NicknameRegisterResDto("사용자 이름이 성공적으로 등록되었습니다.", nickname);
    }
}
