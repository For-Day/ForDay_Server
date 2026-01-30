package com.example.ForDay.domain.user.service;

import com.example.ForDay.domain.friend.repository.FriendRelationRepository;
import com.example.ForDay.domain.friend.type.FriendRelationStatus;
import com.example.ForDay.domain.hobby.repository.HobbyCardRepository;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordScrapRepository;
import com.example.ForDay.domain.record.service.ActivityRecordService;
import com.example.ForDay.domain.record.type.RecordVisibility;
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
import com.example.ForDay.infra.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserUtil userUtil;
    private final S3Service s3Service;
    private final HobbyRepository hobbyRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final HobbyCardRepository hobbyCardRepository;
    private final FriendRelationRepository friendRelationRepository;
    private final ActivityRecordScrapRepository activityRecordScrapRepository;

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
    public UserInfoResDto getUserInfo(CustomUserDetails user, String userId) {
        User targetUser;
        String targetId;
        if(userId != null) {
            // 다른 사용자 정보 조회시 (차단 관계, 탈퇴한 회원인지 고려)
            User currentUser = userUtil.getCurrentUser(user);
            targetId = userId;
            targetUser = userRepository.findById(targetId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            checkBlockedAndDeletedUser(currentUser.getId(), targetId, targetUser.isDeleted());
        } else {
            targetUser = userUtil.getCurrentUser(user);
            targetId = targetUser.getId();
        }

        int totalStickerCount = hobbyRepository.sumCurrentStickerNumByUserId(targetId).orElse(0);
        return new UserInfoResDto(toProfileMainResizedUrl(targetUser.getProfileImageUrl()), // 프로필 조회용 url로 수정
                targetUser.getNickname(),
                totalStickerCount);
    }

    @Transactional
    public SetUserProfileImageResDto setUserProfileImage(SetUserProfileImageReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        String newImageUrl = reqDto.getProfileImageUrl(); // 새로 설정하는 원본 url
        String resizedImageUrl = toProfileMainResizedUrl(newImageUrl); // 새로 설정하는 리사이즈 url

        // 원본이 그대로 저장되므로 db에도 원본이 url이 이미 있는지 확인
        if (Objects.equals(currentUser.getProfileImageUrl(), newImageUrl)) {
            return new SetUserProfileImageResDto(currentUser.getProfileImageUrl(), "이미 동일한 프로필 이미지로 설정되어 있습니다.");
        }

        // 새로 업데이트 하는 경우 기존 url 삭제
        String oldImageUrl = currentUser.getProfileImageUrl();
        if (oldImageUrl != null && !oldImageUrl.isBlank()) {
            String oldKey = s3Service.extractKeyFromFileUrl(oldImageUrl);
            String oldMainResizedUrl = toProfileMainResizedUrl(oldImageUrl);
            String oldMainResizedKey = s3Service.extractKeyFromFileUrl(oldMainResizedUrl);

            //String oldListResizedUrl = toProfileListResizedUrl(oldImageUrl);
            //String oldListResizedKey = s3Service.extractKeyFromFileUrl(oldListResizedUrl);

            if (s3Service.existsByKey(oldKey)) { // 원래 원본 이미지 url 삭제
                s3Service.deleteByKey(oldKey);
            }
            if(s3Service.existsByKey(oldMainResizedKey)) { // 원래 리사이즈 이미지 url 삭제
                s3Service.deleteByKey(oldMainResizedKey);
            }
            /*if(s3Service.existsByKey(oldListResizedKey)) {
                s3Service.deleteByKey(oldListResizedKey);
            }*/
        }

        String newKey = s3Service.extractKeyFromFileUrl(newImageUrl); // 새로 설정하는 원본 key
        String resizedKey = s3Service.extractKeyFromFileUrl(resizedImageUrl); // 새로 설정하는 resize key
        if (!s3Service.existsByKey(newKey) && !s3Service.existsByKey(resizedKey)) {
            throw new CustomException(ErrorCode.S3_IMAGE_NOT_FOUND);
        }

        currentUser.updateProfileImage(newImageUrl); // 원본 url을 db에 저장 사용 목적에 따라 url을 바꿔서 사용
        userRepository.save(currentUser);
        return new SetUserProfileImageResDto(s3Service.createFileUrl(resizedKey), "프로필 이미지가 성공적으로 변경되었습니다.");
    }

    @Transactional(readOnly = true)
    public GetHobbyInProgressResDto getHobbyInProgress(CustomUserDetails user, String userId) {
        User targetUser;

        if(userId != null) {
            targetUser = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            checkBlockedAndDeletedUser(userUtil.getCurrentUser(user).getId(), targetUser.getId(), targetUser.isDeleted());
        } else {
           targetUser = userUtil.getCurrentUser(user);
        }

        List<GetHobbyInProgressResDto.HobbyDto> hobbyList = hobbyRepository.findUserTabHobbyList(targetUser);

        int inProgressHobbyCount = (int) hobbyList.stream()
                .filter(h -> h.getStatus() == HobbyStatus.IN_PROGRESS)
                .count();

        // 커버 사이즈용 이미지 url 반환하도록 나중에 수정하기

        int hobbyCardCount = targetUser.getHobbyCardCount();
        return new GetHobbyInProgressResDto(inProgressHobbyCount, hobbyCardCount, hobbyList);
    }

    @Transactional(readOnly = true)
    public GetUserFeedListResDto getUserFeedList(List<Long> hobbyIds, Long lastRecordId, Integer feedSize, CustomUserDetails user, String userId) {
        User targetUser;

        List<RecordVisibility> visibilities = new ArrayList<>();
        String currentUserId = userUtil.getCurrentUser(user).getId();

        if (userId == null || userId.equals(currentUserId)) {
            // 내 피드 조회: 모든 권한 오픈
            targetUser = userUtil.getCurrentUser(user);
            visibilities.addAll(List.of(RecordVisibility.PUBLIC, RecordVisibility.FRIEND, RecordVisibility.PRIVATE));
        } else {
            // 남의 피드 조회
            targetUser = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            // 차단 및 탈퇴 체크
            checkBlockedAndDeletedUser(currentUserId, targetUser.getId(), targetUser.isDeleted());

            visibilities.add(RecordVisibility.PUBLIC);

            if (friendRelationRepository.existsByRequesterIdAndTargetUserIdAndRelationStatus(
                    currentUserId, targetUser.getId(), FriendRelationStatus.FOLLOW)) {
                visibilities.add(RecordVisibility.FRIEND);
            }
        }
        String targetUserId = targetUser.getId();

        Long totalFeedCount = null;
        if(lastRecordId == null) {
            totalFeedCount = activityRecordRepository.countRecordByHobbyIds(hobbyIds, targetUserId);
        }

        List<GetUserFeedListResDto.FeedDto> feedList = activityRecordRepository.findUserFeedList(hobbyIds, lastRecordId, feedSize, targetUserId, visibilities);

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
    public GetUserHobbyCardListResDto getUserHobbyCardList(Long lastHobbyCardId, Integer size, CustomUserDetails user, String userId) {
        User targetUser;

        if(userId != null) {
            targetUser = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            checkBlockedAndDeletedUser(userUtil.getCurrentUser(user).getId(), targetUser.getId(), targetUser.isDeleted());
        } else {
            targetUser = userUtil.getCurrentUser(user);
        }

        List<GetUserHobbyCardListResDto.HobbyCardDto> cardDtoList = hobbyCardRepository.findUserHobbyCardList(lastHobbyCardId, size, targetUser);

        boolean hasNext = false;
        if (cardDtoList.size() > size) {
            hasNext = true;
            cardDtoList.remove(size.intValue());
        }

        Long lastId = cardDtoList.isEmpty() ? null : cardDtoList.get(cardDtoList.size() - 1).getHobbyCardId();

        return new GetUserHobbyCardListResDto(lastId, cardDtoList, hasNext);
    }

    @Transactional(readOnly = true)
    public GetUserScrapListResDto getUserScrapList(Long lastScrapId, Integer size, CustomUserDetails user, String userId) {
        User targetUser;

        if(userId == null) {
            // 자신의 스크랩 목록 조회
            targetUser = userUtil.getCurrentUser(user);

            long scrapCount = 0;
            if(lastScrapId == null) {
                scrapCount  = activityRecordScrapRepository.countByUserId(targetUser.getId());
            }
            List<GetUserScrapListResDto.ScrapDto> scrapDtos = activityRecordScrapRepository.getMyScrapList(lastScrapId, size, targetUser.getId());

            boolean hasNext = false;
            if (scrapDtos.size() > size) {
                hasNext = true;
                scrapDtos.remove(size.intValue());
            }

            Long lastId = scrapDtos.isEmpty() ? null : scrapDtos.get(scrapDtos.size() - 1).getRecordId();

            return new GetUserScrapListResDto(scrapCount, lastId, scrapDtos, hasNext);
        } else {
            // 다른 사람의 피드 조회시
            targetUser = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            User currentUser = userUtil.getCurrentUser(user);

            // 차단 및 탈퇴 확인
            checkBlockedAndDeletedUser(currentUser.getId(), targetUser.getId(), targetUser.isDeleted());

            // 현재 유저의 친구 id 목록 조회??
        }
    }

    private void checkBlockedAndDeletedUser(String currentUserId, String targetId, boolean deleted) {
        // 한쪽이라도 차단 관계가 있는지 확인
        if(friendRelationRepository.existsByRequesterIdAndTargetUserIdAndRelationStatus(currentUserId, targetId, FriendRelationStatus.BLOCK) || friendRelationRepository.existsByRequesterIdAndTargetUserIdAndRelationStatus(targetId, currentUserId, FriendRelationStatus.BLOCK)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 타겟유저가 탈퇴한 회원인 경우
        if(deleted) throw new CustomException(ErrorCode.USER_NOT_FOUND);
    }

    private static String toProfileMainResizedUrl(String originalUrl) {
        if (originalUrl == null || !originalUrl.contains("/temp/")) {
            return originalUrl;
        }
        return originalUrl.replace("/temp/", "/resized/main/");
    }

    private static String toProfileListResizedUrl(String originalUrl) {
        if (originalUrl == null || !originalUrl.contains("/temp/")) {
            return originalUrl;
        }
        return originalUrl.replace("/temp/", "/resized/list/");
    }

    private static String toFeedThumbResizedUrl(String originalUrl) {
        if (originalUrl == null || !originalUrl.contains("/temp/")) {
            return originalUrl;
        }
        return originalUrl.replace("/temp/", "/resized/thumb/");
    }

    private static String toCoverMainResizedUrl(String originalUrl) {
        if (originalUrl == null || !originalUrl.contains("/temp/")) {
            return originalUrl;
        }
        return originalUrl.replace("/temp/", "/resized/thumb/");
    }
}
