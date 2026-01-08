package com.example.ForDay.domain.auth.service;

import com.example.ForDay.domain.auth.dto.KakaoProfileDto;
import com.example.ForDay.domain.auth.dto.request.GuestLoginReqDto;
import com.example.ForDay.domain.auth.dto.request.KakaoLoginReqDto;
import com.example.ForDay.domain.auth.dto.request.RefreshReqDto;
import com.example.ForDay.domain.auth.dto.response.LoginResDto;
import com.example.ForDay.domain.auth.dto.response.RefreshResDto;
import com.example.ForDay.domain.auth.repository.RefreshTokenRepository;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.domain.user.service.UserService;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.common.response.dto.MessageResDto;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final KakaoService kakaoService;
    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public LoginResDto kakaoLogin(@Valid KakaoLoginReqDto reqDto) {
        log.info("[LOGIN] kakao login process start");

        // accessToken을 활용하여 카카오 사용자 정보 얻기
        KakaoProfileDto kakaoProfileDto = kakaoService.getKakaoProfile(reqDto.getKakaoAccessToken());

        log.info("[LOGIN] Kakao userId={}", kakaoProfileDto.getId());

        boolean isNewUser = false;
        // 회원가입이 되어 있지 않다면 회원가입
        User originalUser = userService.getUserBySocialId(kakaoProfileDto.getId());
        if(originalUser == null) {
            log.info("[LOGIN] New Kakao user registered. kakaoId={}", kakaoProfileDto.getId());
            isNewUser = true;
            // 회원가입
            originalUser = userService.createOauth(kakaoProfileDto.getId(), kakaoProfileDto.getKakao_account(), SocialType.KAKAO);
        }

        log.info("[LOGIN] Kakao login success userId={}", originalUser.getId());

        // 회원 가입 되어 있는 경우 -> 토큰 발급
        String accessToken = jwtUtil.createAccessToken(originalUser.getSocialId(), Role.USER, SocialType.KAKAO);
        String refreshToken = jwtUtil.createRefreshToken(originalUser.getSocialId());

        refreshTokenService.save(originalUser.getSocialId(), refreshToken);

        return new LoginResDto(accessToken, refreshToken, isNewUser, SocialType.KAKAO);
    }

    @Transactional
    public LoginResDto guestLogin(GuestLoginReqDto reqDto) {
        User user;
        String guestUserId = reqDto.getGuestUserId();
        boolean newUser;

        if(guestUserId == null || guestUserId.isBlank()) {
            String socialId = "guest_" + UUID.randomUUID(); // 게스트용 socialId 생성

            user = userRepository.save(User.builder()
                    .role(Role.GUEST)
                    .socialType(SocialType.GUEST)
                    .socialId(socialId)
                    .build());
            newUser = true;

            log.info("[GUEST] New guest created id={}", user.getId());

        } else {
             user = userRepository.findBySocialId(guestUserId);
             if(user == null) throw new CustomException(ErrorCode.USER_NOT_FOUND);

            if (user.getRole() != Role.GUEST) {
                throw new CustomException(ErrorCode.INVALID_USER_ROLE);
            }
            newUser = false;
        }
        user.updateLastActivity(); // 게스트 마지막 활동 일시 업데이트
        log.info("[GUEST] Last activity updated userId={}", user.getId());

        String accessToken = jwtUtil.createAccessToken(user.getSocialId(), user.getRole(), SocialType.GUEST);
        String refreshToken = jwtUtil.createRefreshToken(user.getSocialId());

        refreshTokenService.save(user.getSocialId(), refreshToken);

        return new LoginResDto(accessToken, refreshToken, newUser, SocialType.GUEST);
    }


    @Transactional
    public RefreshResDto refresh(@Valid RefreshReqDto reqDto) {
        String refreshToken = reqDto.getRefreshToken();

        // 리프레시 토큰 유효성 검사
        if (!jwtUtil.validate(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String username = jwtUtil.getUsername(refreshToken);

        // 저장된 refreshToken 조회
        String storedToken = refreshTokenService.get(username);

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 토큰 재발급
        User user = userRepository.findBySocialId(username);
        if (user == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        String newAccessToken = jwtUtil.createAccessToken(username, user.getRole(), user.getSocialType());
        String newRefreshToken = jwtUtil.createRefreshToken(username);

        refreshTokenService.save(username, newRefreshToken);

        return new RefreshResDto(newAccessToken, newRefreshToken);
    }

    @Transactional
    public MessageResDto logout(CustomUserDetails user) {
        String username = user.getUsername();
        refreshTokenRepository.deleteById(username);
        return new MessageResDto("로그아웃 되었습니다.");
    }
}
