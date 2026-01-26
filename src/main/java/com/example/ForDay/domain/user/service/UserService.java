package com.example.ForDay.domain.user.service;

import com.example.ForDay.domain.hobby.repository.HobbyCardRepository;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.record.entity.UserRecordCount;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.repository.UserRecordCountRepository;
import com.example.ForDay.domain.user.dto.request.SetUserProfileImageReqDto;
import com.example.ForDay.domain.user.dto.response.*;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.infra.s3.S3Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserUtil userUtil;
    private final S3Service s3Service;
    private final HobbyRepository hobbyRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final UserRecordCountRepository userRecordCountRepository;
    private final HobbyCardRepository hobbyCardRepository;

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
        if (exists) {
            throw new CustomException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        User currentUser = userRepository.findBySocialId(user.getUsername());
        currentUser.changeNickname(nickname);

        return new NicknameRegisterResDto("사용자 이름이 성공적으로 등록되었습니다.", nickname);
    }

    @Transactional(readOnly = true)
    public UserInfoResDto getUserInfo(CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        return new UserInfoResDto(toProfileMainResizedUrl(currentUser.getProfileImageUrl()),
                currentUser.getNickname(),
                currentUser.getTotalCollectedStickerCount() == null ? 0 : currentUser.getTotalCollectedStickerCount());
    }

    @Transactional
    public SetUserProfileImageResDto setUserProfileImage(SetUserProfileImageReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        String newImageUrl = reqDto.getProfileImageUrl();
        String resizedImageUrl = toProfileMainResizedUrl(newImageUrl);

        if (Objects.equals(currentUser.getProfileImageUrl(), newImageUrl)) {
            return new SetUserProfileImageResDto(currentUser.getProfileImageUrl(), "이미 동일한 프로필 이미지로 설정되어 있습니다.");
        }

        String originalKey = s3Service.extractKeyFromFileUrl(newImageUrl);
        String resizedKey  = s3Service.extractKeyFromFileUrl(resizedImageUrl);

        if (!s3Service.existsByKey(originalKey) && !s3Service.existsByKey(resizedKey)) {
            throw new CustomException(ErrorCode.S3_IMAGE_NOT_FOUND);
        }

        currentUser.updateProfileImage(newImageUrl); // 원본 url을 db에 저장 사용 목적에 따라 url을 바꿔서 사용
        return new SetUserProfileImageResDto(currentUser.getProfileImageUrl(), "프로필 이미지가 성공적으로 변경되었습니다.");
    }

    @Transactional(readOnly = true)
    public GetHobbyInProgressResDto getHobbyInProgress(CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        List<GetHobbyInProgressResDto.HobbyDto> hobbyList = hobbyRepository.findUserTabHobbyList(currentUser);

        int inProgressHobbyCount = (int) hobbyList.stream()
                .filter(h -> h.getStatus() == HobbyStatus.IN_PROGRESS)
                .count();

        int hobbyCardCount = currentUser.getHobbyCardCount();
        return new GetHobbyInProgressResDto(inProgressHobbyCount, hobbyCardCount, hobbyList);
    }

    @Transactional(readOnly = true)
    public GetUserFeedListResDto getUserFeedList(Long hobbyId, Long lastRecordId, Integer feedSize, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        String userId = currentUser.getId();

        int totalFeedCount = userRecordCountRepository.findRecordCountByUserId(userId).orElse(0);

        List<GetUserFeedListResDto.FeedDto> feedList = activityRecordRepository.findUserFeedList(hobbyId, lastRecordId, feedSize, userId);

        boolean hasNext = false;
        if (feedList.size() > feedSize) {
            hasNext = true;
            feedList.remove(feedSize.intValue());
        }

        // resized url 호출하도록 수정
        feedList.forEach(feedDto -> {
            feedDto.setThumbnailImageUrl(
                    toFeedThumbResizedUrl(feedDto.getThumbnailImageUrl())
            );
        });

        Long lastId = feedList.isEmpty() ? null : feedList.get(feedList.size() - 1).getRecordId();
        return new GetUserFeedListResDto(totalFeedCount, lastId, feedList, hasNext);
    }

    @Transactional(readOnly = true)
    public GetUserHobbyCardListResDto getUserHobbyCardList(Long lastHobbyCardId, Integer size, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        List<GetUserHobbyCardListResDto.HobbyCardDto> cardDtoList = hobbyCardRepository.findUserHobbyCardList(lastHobbyCardId, size, currentUser);

        boolean hasNext = false;
        if (cardDtoList.size() > size) {
            hasNext = true;
            cardDtoList.remove(size.intValue());
        }

        Long lastId = cardDtoList.isEmpty() ? null : cardDtoList.get(cardDtoList.size() - 1).getHobbyCardId();

        return new GetUserHobbyCardListResDto(lastId, cardDtoList, hasNext);
    }

    private static String toProfileMainResizedUrl(String originalUrl) {
        if (originalUrl == null || !originalUrl.contains("/temp/")) {
            return originalUrl;
        }
        return originalUrl.replace("/temp/", "/resized/main/");
    }

    private static String toFeedThumbResizedUrl(String originalUrl) {
        if (originalUrl == null || !originalUrl.contains("/temp/")) {
            return originalUrl;
        }
        return originalUrl.replace("/temp/", "/resized/thumb/");
    }
}
