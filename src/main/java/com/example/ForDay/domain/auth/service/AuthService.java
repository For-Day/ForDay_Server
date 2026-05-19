package com.example.ForDay.domain.auth.service;

import com.example.ForDay.domain.auth.dto.LoginInternalResult;
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
import com.example.ForDay.global.common.response.message.AuthSuccessCode;
import com.example.ForDay.global.firebase.repository.FcmTokenRepository;
import com.example.ForDay.global.firebase.service.FcmTokenService;
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
    private final FcmTokenService fcmTokenService;
    private final FcmTokenRepository fcmTokenRepository;

    @Transactional
    public LoginResDto kakaoLogin(KakaoLoginReqDto reqDto) {
        log.info("[LOGIN] kakao login process start");

        // 카카오 accessToken을 활용하여 카카오 사용자 정보 얻기
        KakaoProfileDto kakaoProfileDto = kakaoService.getKakaoProfile(reqDto.getKakaoAccessToken());
        String socialId = createSocialId(SocialType.KAKAO, String.valueOf(kakaoProfileDto.getId()));

        log.info("[LOGIN] Kakao userId={}", kakaoProfileDto.getId());

        User user = userRepository.findBySocialId(socialId);
        boolean isNewUser = (user == null);
        if (isNewUser) {
            // 회원가입이 되어 있지 않다면 회원가입
            log.info("[LOGIN] New Kakao user registered. kakaoId={}", kakaoProfileDto.getId());
            user = userService.createOauth(socialId, kakaoProfileDto.getKakao_account().getEmail(), SocialType.KAKAO);
        }

        log.info("[LOGIN] Kakao login success userId={}", user.getId());

        String fcmToken = fcmTokenService.registerFcmToken(user, reqDto.getFcmToken(), reqDto.getDeviceId(), reqDto.getDeviceType());
        LoginInternalResult result = processCommonLogin(user, SocialType.KAKAO);

        return LoginResDto.of(result, user, isNewUser, SocialType.KAKAO, fcmToken);
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
        String socialId = createSocialId(SocialType.APPLE, claims.getSubject());
        String email = claims.containsKey("email") ? claims.get("email", String.class) : null;

        User user = userRepository.findBySocialId(socialId);
        boolean isNewUser = (user == null);
        if (isNewUser) {
            // 처음 회원가입 하는 유저
            log.info("[LOGIN] New Apple user registered. appleId={}", socialId);
            user = userService.createOauth(socialId, email, SocialType.APPLE);
        }

        log.info("[LOGIN] Apple login success userId={}", user.getId());

        String fcmToken = fcmTokenService.registerFcmToken(user, reqDto.getFcmToken(), reqDto.getDeviceId(), reqDto.getDeviceType());
        LoginInternalResult result = processCommonLogin(user, SocialType.APPLE);

        return LoginResDto.of(result, user, isNewUser, SocialType.APPLE, fcmToken);
    }

    @Transactional
    public GuestLoginResDto guestLogin(GuestLoginReqDto reqDto) {
        User user;
        String guestUserId = reqDto.getGuestUserId();
        boolean isNewUser = false;

        if(StringUtils.hasText(guestUserId) && guestUserId.startsWith("withdrawn")) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 처음 가입하는 게스트 로그인일 때
        if (!StringUtils.hasText(guestUserId)) {
            String socialId = "guest_" + UUID.randomUUID(); // 게스트용 socialId 생성

            user = userRepository.save(User.builder()
                    .role(Role.GUEST)
                    .socialType(SocialType.GUEST)
                    .socialId(socialId)
                    .build());
            isNewUser = true;

            log.info("[GUEST] New guest created id={}", user.getId());

        } else {
            user = userRepository.findBySocialId(guestUserId);
            if (user == null) throw new CustomException(ErrorCode.USER_NOT_FOUND);

            if (user.getRole() != Role.GUEST) {
                throw new CustomException(ErrorCode.INVALID_USER_ROLE);
            }
        }

        user.updateLastActivity(); // 게스트 마지막 활동 일시 업데이트
        log.info("[GUEST] Last activity updated userId={}", user.getId());

        LoginInternalResult result = processCommonLogin(user, SocialType.GUEST);

        return GuestLoginResDto.of(result, user, isNewUser);
    }

    @Transactional
    public RefreshResDto refresh(@Valid RefreshReqDto reqDto) {
        String refreshToken = reqDto.getRefreshToken();
        log.info("[refresh] 토큰 재발급 프로세스 시작");

        // 리프레시 토큰 유효성 검사
        if (!jwtUtil.validate(refreshToken)) {
            log.warn("[refresh] 유효하지 않은 리프레시 토큰으로 접근 시도");
            throw new CustomException(ErrorCode.LOGIN_EXPIRED);
        }

        String socialId = jwtUtil.getUsername(refreshToken);
        log.info("[refresh] 요청자 SocialId: {}", socialId);

        // 저장된 refreshToken 조회
        String storedToken = refreshTokenService.get(socialId);

        if (storedToken == null) {
            log.warn("[refresh] 저장된 토큰이 없음 - 만료되었거나 로그아웃된 사용자: {}", socialId);
            throw new CustomException(ErrorCode.LOGIN_EXPIRED);
        }

        if (!storedToken.equals(refreshToken)) {
            log.error("[refresh] 토큰 불일치! 탈취 가능성 있음 - SocialId: {}", socialId);
            throw new CustomException(ErrorCode.LOGIN_EXPIRED);
        }

        // 토큰 재발급
        User user = userRepository.findBySocialId(socialId);
        if (user == null) {
            log.error("[refresh] 토큰은 유효하나 사용자를 찾을 수 없음 - SocialId: {}", socialId);
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        String newAccessToken = jwtUtil.createAccessToken(socialId, user.getRole(), user.getSocialType());
        String newRefreshToken = jwtUtil.createRefreshToken(socialId);

        refreshTokenService.save(socialId, newRefreshToken);

        log.info("[refresh] 토큰 재발급 완료 - SocialId: {}", socialId);
        return RefreshResDto.of(newAccessToken, newRefreshToken);
    }

    @Transactional
    public MessageResDto logout(CustomUserDetails user, String deviceId) {
        User currentUser = userUtil.getCurrentUser(user);
        log.info("[logout] 로그아웃 요청 - SocialId: {}", currentUser.getSocialId());

        refreshTokenRepository.deleteById(currentUser.getSocialId());

        if (deviceId != null && !deviceId.isBlank()) {
            fcmTokenRepository.deleteByUserAndDeviceId(currentUser, deviceId);
            log.info("[logout] 특정 FCM 토큰 삭제 완료 - deviceId: {}", deviceId);
        }

        log.info("[logout] 로그아웃 처리 완료(RT 삭제됨) - SocialId: {}", currentUser.getSocialId());
        return new MessageResDto(AuthSuccessCode.LOGOUT_SUCCESS.getMessage());
    }

    @Transactional
    public SwitchAccountResDto switchAccount(SwitchAccountReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        log.info("Switch account attempt - userId: {}, currentRole: {}, requestSocialType: {}", currentUser.getId(), currentUser.getRole(), reqDto.getSocialType());

        if(!currentUser.getRole().equals(Role.GUEST)) {
            log.warn("Switch account denied - userId: {} is not GUEST (role: {})", currentUser.getId(), currentUser.getRole());
            throw new CustomException(ErrorCode.NO_GUEST_ACCESS);
        }

        if(reqDto.getSocialType() == SocialType.GUEST) {
            log.warn("Invalid switch request - userId: {} tried to switch to GUEST", currentUser.getId());
            throw new CustomException(ErrorCode.INVALID_REQUEST_TYPE);
        }

        String socialId = "";
        String email = null;

        switch (reqDto.getSocialType()) {
            case KAKAO -> {
                log.info("Calling Kakao API for switch - userId: {}", currentUser.getId());
                KakaoProfileDto kakaoProfileDto = kakaoService.getKakaoProfile(reqDto.getSocialCode());
                socialId = createSocialId(SocialType.KAKAO, String.valueOf(kakaoProfileDto.getId()));
                email = kakaoProfileDto.getKakao_account().getEmail();
            }
            case APPLE -> {
                log.info("Calling Apple API for switch - userId: {}", currentUser.getId());
                AppleTokenResDto appleTokenResDto = appleService.getAppleToken(reqDto.getSocialCode());
                Claims claims = appleService.verifyAndParseAppleIdToken(appleTokenResDto);
                socialId = createSocialId(SocialType.APPLE, claims.getSubject());
                email = claims.containsKey("email") ? claims.get("email", String.class) : null;
            }
        }

        User targetUser = userRepository.findBySocialId(socialId);
        String accessToken = "";
        String refreshToken = "";

        if (targetUser != null) {
            log.info("Switch account - Social account already exists. Logging in as existing user. socialId: {}, existingUserId: {}", socialId, targetUser.getId());

            accessToken = jwtUtil.createAccessToken(socialId, targetUser.getRole(), reqDto.getSocialType());
            refreshToken = jwtUtil.createRefreshToken(socialId);

            String fcmToken = fcmTokenService.registerFcmToken(targetUser, reqDto.getFcmToken(), reqDto.getDeviceId(), reqDto.getDeviceType());

            return SwitchAccountResDto.of(reqDto.getSocialType(), accessToken, refreshToken, fcmToken);

        } else {
            log.info("Switch account - Creating new social link for GUEST userId: {}, socialId: {}", currentUser.getId(), socialId);

            currentUser.switchAccount(email, Role.USER, reqDto.getSocialType(), socialId);
            userRepository.save(currentUser);

            accessToken = jwtUtil.createAccessToken(socialId, Role.USER, reqDto.getSocialType());
            refreshToken = jwtUtil.createRefreshToken(socialId);

            String fcmToken = fcmTokenService.registerFcmToken(currentUser, reqDto.getFcmToken(), reqDto.getDeviceId(), reqDto.getDeviceType());

            return SwitchAccountResDto.of(reqDto.getSocialType(), accessToken, refreshToken, fcmToken);
        }
    }

    @Transactional
    public TokenValidateResDto tokenValidate() {
        log.info("[tokenValidate] 액세스 토큰 유효성 확인 성공");
        return new TokenValidateResDto(true);
    }

    @Transactional
    public UserWithDrawResDto userWithDraw(CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        currentUser.withdraw();
        userRepository.save(currentUser);
        fcmTokenService.deleteUserFcmToken(fcmTokenService.findUserFcmToken(currentUser.getId()));

        return UserWithDrawResDto.of(currentUser.getDeletedAt());
    }

    private OnboardingDataDto getOnboardingData(User user, boolean isNicknameSet, boolean onboardingCompleted) {
        if(onboardingCompleted && !isNicknameSet) { // 해당 사용자가 온보딩 완료, 닉네임 설정 미완료시 온보딩 데이터 조회해서 반환
            return hobbyRepository.getOnboardingDate(user);
        }
        return null;
    }

    private String createSocialId(SocialType type, String id) {
        return type.toString().toLowerCase() + "_" + id;
    }

    private LoginInternalResult processCommonLogin(User user, SocialType socialType) {
        String accessToken = jwtUtil.createAccessToken(user.getSocialId(), user.getRole(), socialType);
        String refreshToken = jwtUtil.createRefreshToken(user.getSocialId());
        refreshTokenService.save(user.getSocialId(), refreshToken);
        OnboardingDataDto dataDto = getOnboardingData(user, user.isNicknameSet(), user.isOnboardingCompleted());

        return new LoginInternalResult(accessToken, refreshToken, user.isOnboardingCompleted(), user.isNicknameSet(), dataDto);
    }

}
