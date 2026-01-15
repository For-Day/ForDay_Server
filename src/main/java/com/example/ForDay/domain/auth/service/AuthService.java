package com.example.ForDay.domain.auth.service;

import com.example.ForDay.domain.auth.dto.request.AppleLoginReqDto;
import com.example.ForDay.domain.auth.dto.response.*;
import com.example.ForDay.domain.auth.dto.request.GuestLoginReqDto;
import com.example.ForDay.domain.auth.dto.request.KakaoLoginReqDto;
import com.example.ForDay.domain.auth.dto.request.RefreshReqDto;
import com.example.ForDay.domain.auth.entity.RefreshToken;
import com.example.ForDay.domain.auth.repository.RefreshTokenRepository;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
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
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final AppleService appleService;
    private final HobbyRepository hobbyRepository;

    @Transactional
    public LoginResDto kakaoLogin(@Valid KakaoLoginReqDto reqDto) {
        log.info("[LOGIN] kakao login process start");

        // accessToken을 활용하여 카카오 사용자 정보 얻기
        KakaoProfileDto kakaoProfileDto = kakaoService.getKakaoProfile(reqDto.getKakaoAccessToken());

        log.info("[LOGIN] Kakao userId={}", kakaoProfileDto.getId());

        boolean isNewUser = false;
        // 회원가입이 되어 있지 않다면 회원가입
        User user = userService.getUserBySocialId(kakaoProfileDto.getId());
        if (user == null) {
            log.info("[LOGIN] New Kakao user registered. kakaoId={}", kakaoProfileDto.getId());
            isNewUser = true;
            // 회원가입
            user = userService.createOauth(kakaoProfileDto.getId(), kakaoProfileDto.getKakao_account().getEmail(), SocialType.KAKAO);
        }

        log.info("[LOGIN] Kakao login success userId={}", user.getId());

        // 회원 가입 되어 있는 경우 -> 토큰 발급
        String accessToken = jwtUtil.createAccessToken(user.getSocialId(), Role.USER, SocialType.KAKAO);
        String refreshToken = jwtUtil.createRefreshToken(user.getSocialId());

        boolean isNicknameSet = isNicknameSet(user); // 닉네임 설정 여부
        boolean onboardingCompleted = user.isOnboardingCompleted(); // 온보딩 완료 여부
        OnboardingDataDto dataDto = getOnboardingData(user, isNicknameSet(user), onboardingCompleted);

        refreshTokenService.save(user.getSocialId(), refreshToken);

        return new LoginResDto(accessToken, refreshToken, isNewUser, SocialType.KAKAO, onboardingCompleted, isNicknameSet, dataDto);
    }

    private OnboardingDataDto getOnboardingData(User user, boolean isNicknameSet, boolean onboardingCompleted) {
        if(onboardingCompleted && !isNicknameSet) {
            return hobbyRepository.getOnboardingDate(user);
        }
        return null;
    }

    @Transactional
    public LoginResDto appleLogin(AppleLoginReqDto reqDto) {
        // 프론트에서 code값을 보내면서 로그인/회원가입 요청을 한다.
        // code와 애플 설정값을 이용하여 직접 JWT 토큰 생성후 apple api에 유저 정보 요청을 보낸다. -> 응답으로 idToken과 accessToken을 받는다.
        AppleTokenResDto appleTokenResDto = appleService.getAppleToken(reqDto.getCode());

        // 응답으로 받은 idToken에 대해 공개키로 무결성 검증을 진행한다.  (공개키 생성은 애플 api에 요청해서 받아오기)
        // 공개키 받아서 검증 후 payload 읽기
        Claims claims = appleService.verifyAndParseAppleIdToken(appleTokenResDto);

        String socialId = claims.getSubject();
        String email = claims.get("email", String.class);
        User user = userRepository.findBySocialId(socialId);

        boolean isNewUser = false;
        if (user == null) {
            // 처음 회원가입 하는 유저
            log.info("[LOGIN] New Apple user registered. appleId={}", socialId);
            isNewUser = true;
            user = userService.createOauth(socialId, email, SocialType.APPLE);
        }

        log.info("[LOGIN] Apple login success userId={}", user.getId());

        String accessToken = jwtUtil.createAccessToken(user.getSocialId(), Role.USER, SocialType.APPLE);
        String refreshToken = jwtUtil.createRefreshToken(user.getSocialId());

        boolean isNicknameSet = isNicknameSet(user);
        boolean onboardingCompleted = user.isOnboardingCompleted();
        OnboardingDataDto dataDto = getOnboardingData(user, isNicknameSet(user), onboardingCompleted);

        refreshTokenService.save(user.getSocialId(), refreshToken);

        return new LoginResDto(accessToken, refreshToken, isNewUser, SocialType.APPLE, onboardingCompleted, isNicknameSet, dataDto);
    }

    @Transactional
    public GuestLoginResDto guestLogin(GuestLoginReqDto reqDto) {
        User user;
        String guestUserId = reqDto.getGuestUserId();
        boolean newUser;

        if (guestUserId == null || guestUserId.isBlank()) {
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
            if (user == null) throw new CustomException(ErrorCode.USER_NOT_FOUND);

            if (user.getRole() != Role.GUEST) {
                throw new CustomException(ErrorCode.INVALID_USER_ROLE);
            }
            newUser = false;
        }
        user.updateLastActivity(); // 게스트 마지막 활동 일시 업데이트
        log.info("[GUEST] Last activity updated userId={}", user.getId());

        String accessToken = jwtUtil.createAccessToken(user.getSocialId(), user.getRole(), SocialType.GUEST);
        String refreshToken = jwtUtil.createRefreshToken(user.getSocialId());

        boolean isNicknameSet = isNicknameSet(user);
        boolean onboardingCompleted = user.isOnboardingCompleted();
        OnboardingDataDto dataDto = getOnboardingData(user, isNicknameSet(user), onboardingCompleted);

        refreshTokenService.save(user.getSocialId(), refreshToken);

        return new GuestLoginResDto(accessToken, refreshToken, newUser, SocialType.GUEST, user.getSocialId(), onboardingCompleted, isNicknameSet, dataDto);
    }

    private static boolean isNicknameSet(User user) {
        boolean isNicknameSet = false;
        // 닉네임 설정 완료 여부
        if (StringUtils.hasText(user.getNickname())) {
            isNicknameSet = true;
        }
        return isNicknameSet;
    }


    @Transactional
    public RefreshResDto refresh(@Valid RefreshReqDto reqDto) {
        String refreshToken = reqDto.getRefreshToken();

        // 리프레시 토큰 유효성 검사
        if (!jwtUtil.validate(refreshToken)) {
            throw new CustomException(ErrorCode.LOGIN_EXPIRED);
        }

        String username = jwtUtil.getUsername(refreshToken);

        // 저장된 refreshToken 조회
        String storedToken = refreshTokenService.get(username);

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new CustomException(ErrorCode.LOGIN_EXPIRED);
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

    @Transactional
    public TokenValidateResDto tokenValidate(CustomUserDetails user) {
        return new TokenValidateResDto(true);
    }

}
