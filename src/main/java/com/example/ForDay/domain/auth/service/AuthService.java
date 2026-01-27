package com.example.ForDay.domain.auth.service;

import com.example.ForDay.domain.auth.dto.request.*;
import com.example.ForDay.domain.auth.dto.response.*;
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
import com.example.ForDay.global.util.UserUtil;
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
    private final UserUtil userUtil;

    @Transactional
    public LoginResDto kakaoLogin(KakaoLoginReqDto reqDto) {
        log.info("[LOGIN] kakao login process start");

        // 카카오 accessToken을 활용하여 카카오 사용자 정보 얻기
        KakaoProfileDto kakaoProfileDto = kakaoService.getKakaoProfile(reqDto.getKakaoAccessToken());
        String socialId = SocialType.KAKAO.toString().toLowerCase() + "_" + kakaoProfileDto.getId();

        log.info("[LOGIN] Kakao userId={}", kakaoProfileDto.getId());

        boolean isNewUser = false;

        User user = userRepository.findBySocialId(socialId);
        if (user == null) {
            // 회원가입이 되어 있지 않다면 회원가입
            log.info("[LOGIN] New Kakao user registered. kakaoId={}", kakaoProfileDto.getId());
            isNewUser = true;
            // 회원가입 (유저 엔티티 생성)
            user = userService.createOauth(socialId, kakaoProfileDto.getKakao_account().getEmail(), SocialType.KAKAO);
        }

        log.info("[LOGIN] Kakao login success userId={}", user.getId());

        // 회원 가입 되어 있는 경우 -> 토큰 발급
        String accessToken = jwtUtil.createAccessToken(socialId, Role.USER, SocialType.KAKAO);
        String refreshToken = jwtUtil.createRefreshToken(socialId);
        refreshTokenService.save(socialId, refreshToken);

        boolean isNicknameSet = hasNickname(user); // 닉네임 설정 여부
        boolean onboardingCompleted = user.isOnboardingCompleted(); // 온보딩 완료 여부
        OnboardingDataDto dataDto = getOnboardingData(user, isNicknameSet, onboardingCompleted);

        return new LoginResDto(accessToken, refreshToken, isNewUser, SocialType.KAKAO, onboardingCompleted, isNicknameSet, dataDto);
    }

    @Transactional
    public LoginResDto appleLogin(AppleLoginReqDto reqDto) {
        // 프론트에서 code값을 보내면서 로그인/회원가입 요청을 한다.
        // code와 애플 설정값을 이용하여 직접 JWT 토큰 생성후 apple api에 유저 정보 요청을 보낸다. -> 응답으로 idToken과 accessToken을 받는다.
        AppleTokenResDto appleTokenResDto = appleService.getAppleToken(reqDto.getCode());

        // 응답으로 받은 idToken에 대해 공개키로 무결성 검증을 진행한다.  (공개키 생성은 애플 api에 요청해서 받아오기)
        // 공개키 받아서 검증 후 payload 읽기
        Claims claims = appleService.verifyAndParseAppleIdToken(appleTokenResDto);

        // 사용자 정보에서 socialId와 email 추출
        String socialId = SocialType.APPLE.toString().toLowerCase() + "_" + claims.getSubject();
        String email = claims.containsKey("email")
                ? claims.get("email", String.class)
                : null;
        User user = userRepository.findBySocialId(socialId);

        boolean isNewUser = false;
        if (user == null) {
            // 처음 회원가입 하는 유저
            log.info("[LOGIN] New Apple user registered. appleId={}", socialId);
            isNewUser = true;
            user = userService.createOauth(socialId, email, SocialType.APPLE);
        }

        log.info("[LOGIN] Apple login success userId={}", user.getId());

        String accessToken = jwtUtil.createAccessToken(socialId, Role.USER, SocialType.APPLE);
        String refreshToken = jwtUtil.createRefreshToken(socialId);
        refreshTokenService.save(socialId, refreshToken);

        boolean isNicknameSet = hasNickname(user);
        boolean onboardingCompleted = user.isOnboardingCompleted();
        OnboardingDataDto dataDto = getOnboardingData(user, isNicknameSet, onboardingCompleted);

        return new LoginResDto(accessToken, refreshToken, isNewUser, SocialType.APPLE, onboardingCompleted, isNicknameSet, dataDto);
    }

    @Transactional
    public GuestLoginResDto guestLogin(GuestLoginReqDto reqDto) {
        User user;
        String guestUserId = reqDto.getGuestUserId();
        boolean newUser;

        // 처음 가입하는 게스트 로그인일 때
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

        String socialId = user.getSocialId();

        user.updateLastActivity(); // 게스트 마지막 활동 일시 업데이트
        log.info("[GUEST] Last activity updated userId={}", user.getId());

        String accessToken = jwtUtil.createAccessToken(socialId, user.getRole(), SocialType.GUEST);
        String refreshToken = jwtUtil.createRefreshToken(socialId);

        boolean isNicknameSet = hasNickname(user);
        boolean onboardingCompleted = user.isOnboardingCompleted();
        OnboardingDataDto dataDto = getOnboardingData(user, isNicknameSet, onboardingCompleted);

        refreshTokenService.save(socialId, refreshToken);

        return new GuestLoginResDto(accessToken, refreshToken, newUser, SocialType.GUEST, socialId, onboardingCompleted, isNicknameSet, dataDto);
    }

    @Transactional
    public RefreshResDto refresh(@Valid RefreshReqDto reqDto) {
        String refreshToken = reqDto.getRefreshToken();

        // 리프레시 토큰 유효성 검사
        if (!jwtUtil.validate(refreshToken)) {
            throw new CustomException(ErrorCode.LOGIN_EXPIRED);
        }

        String socialId = jwtUtil.getUsername(refreshToken);

        // 저장된 refreshToken 조회
        String storedToken = refreshTokenService.get(socialId);

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new CustomException(ErrorCode.LOGIN_EXPIRED);
        }

        // 토큰 재발급
        User user = userRepository.findBySocialId(socialId);
        if (user == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        String newAccessToken = jwtUtil.createAccessToken(socialId, user.getRole(), user.getSocialType());
        String newRefreshToken = jwtUtil.createRefreshToken(socialId);

        refreshTokenService.save(socialId, newRefreshToken);

        return new RefreshResDto(newAccessToken, newRefreshToken);
    }

    @Transactional
    public MessageResDto logout(CustomUserDetails user) {
        String socialId = user.getUsername();
        refreshTokenRepository.deleteById(socialId);
        return new MessageResDto("로그아웃 되었습니다.");
    }

    @Transactional
    public TokenValidateResDto tokenValidate() {
        return new TokenValidateResDto(true);
    }

    @Transactional
    public SwitchAccountResDto switchAccount(@Valid SwitchAccountReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user); // 현재 유저
        if(!currentUser.getRole().equals(Role.GUEST)) {
            throw new CustomException(ErrorCode.NO_GUEST_ACCESS);
        }

        String accessToken = "";
        String refreshToken = "";
        switch (reqDto.getSocialType()) {
            case KAKAO -> {
                // kakao 회원 정보 받아오기
                KakaoProfileDto kakaoProfileDto = kakaoService.getKakaoProfile(reqDto.getSocialCode());

                String socialId = SocialType.KAKAO.toString().toLowerCase() + "_" + kakaoProfileDto.getId();

                currentUser.switchAccount(kakaoProfileDto.getKakao_account().getEmail(), Role.USER, SocialType.KAKAO, socialId);
                accessToken = jwtUtil.createAccessToken(socialId, Role.USER, SocialType.KAKAO);
                refreshToken = jwtUtil.createRefreshToken(socialId);
            }
            case APPLE -> {

            }
        }

        return new SwitchAccountResDto(reqDto.getSocialType(), accessToken, refreshToken);
    }
    // 유틸 메서드

    private OnboardingDataDto getOnboardingData(User user, boolean isNicknameSet, boolean onboardingCompleted) {
        if(onboardingCompleted && !isNicknameSet) { // 해당 사용자가 온보딩 완료, 닉네임 설정 미완료시 온보딩 데이터 조회해서 반환
            return hobbyRepository.getOnboardingDate(user);
        }
        return null;
    }

    private static boolean hasNickname(User user) {
        return StringUtils.hasText(user.getNickname());
    }
}
