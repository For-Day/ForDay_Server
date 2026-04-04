package com.example.ForDay.domain.user.service;

import com.example.ForDay.domain.friend.entity.FriendRelation;
import com.example.ForDay.domain.friend.repository.FriendRelationRepository;
import com.example.ForDay.domain.friend.type.FriendRelationStatus;
import com.example.ForDay.domain.hobby.repository.HobbyCardRepository;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.notification.service.NotificationService;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordScrapRepository;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.user.dto.TargetUserInfo;
import com.example.ForDay.domain.user.dto.request.SetUserProfileImageReqDto;
import com.example.ForDay.domain.user.dto.response.*;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.domain.user.type.SocialType;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.domain.record.utils.ActivityRecordUtil;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.infra.s3.service.S3Service;
import com.example.ForDay.infra.s3.util.S3DeleteUtil;
import com.example.ForDay.infra.s3.util.S3Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9]+$");
    private static final int MAX_NICKNAME_LENGTH = 10;

    private final UserRepository userRepository;
    private final UserUtil userUtil;
    private final S3Service s3Service;
    private final HobbyRepository hobbyRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final HobbyCardRepository hobbyCardRepository;
    private final FriendRelationRepository friendRelationRepository;
    private final ActivityRecordScrapRepository activityRecordScrapRepository;
    private final S3Util s3Util;
    private final ActivityRecordUtil activityRecordUtil;
    private final NotificationService notificationService;
    private final S3DeleteUtil s3DeleteUtil;

    @Transactional
    public User createOauth(String socialId, String email, SocialType socialType) {
        return userRepository.save(User.createOauth(socialId, email, socialType));
    }

    @Transactional(readOnly = true)
    public NicknameCheckResDto nicknameCheck(String nickname) {
        // 형식 검증 (길이, 허용 문자)
        Optional<String> validationMessage = validateNicknameFormat(nickname);
        if (validationMessage.isPresent()) {
            return new NicknameCheckResDto(nickname, false, validationMessage.get());
        }

        // 중복 검증 (DB 조회)
        if (isExistsByNickname(nickname)) {
            return NicknameCheckResDto.alreadyUsedNickname(nickname);
        }

        // 사용 가능 응답
        return NicknameCheckResDto.canUseNickname(nickname);
    }

    @Transactional
    public NicknameRegisterResDto nicknameRegister(String nickname, CustomUserDetails user) {
        if (isExistsByNickname(nickname)) {
            throw new CustomException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        User currentUser = userUtil.getCurrentUser(user);
        currentUser.changeNickname(nickname);
        userRepository.save(currentUser);

        return NicknameRegisterResDto.from(currentUser.getNickname());
    }

    @Transactional(readOnly = true)
    public UserInfoResDto getUserInfo(CustomUserDetails user, String userId) {
        User targetUser;
        String targetId;

        if (userId != null) {
            // 다른 사용자 정보 조회시 (차단 관계, 탈퇴한 회원인지 고려)
            User currentUser = userUtil.getCurrentUser(user);
            targetId = userId;
            targetUser = userRepository.findById(targetId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            List<FriendRelation> relations = activityRecordUtil.getRelations(currentUser.getId(), targetId);
            activityRecordUtil.checkBlockedAndDeletedUser(relations, targetUser.isDeleted());
        } else {
            targetUser = userUtil.getCurrentUser(user);
            targetId = targetUser.getId();
        }

        int totalStickerCount = hobbyRepository.sumCurrentStickerNumByUserId(targetId).orElse(0);
        return UserInfoResDto.of(targetUser, totalStickerCount, s3Util, userId == null ? notificationService.unreadNotificationExists(targetUser) : false
        );
    }

    @Transactional
    public SetUserProfileImageResDto setUserProfileImage(SetUserProfileImageReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        String newImageUrl = StringUtils.hasText(reqDto.getProfileImageUrl()) ? reqDto.getProfileImageUrl() : null;
        String oldImageUrl = currentUser.getProfileImageUrl();

        if (isSameImageCheck(oldImageUrl, newImageUrl)) {
            return new SetUserProfileImageResDto(s3Util.toProfileMainResizedUrl(oldImageUrl), "이미 동일한 프로필 이미지로 설정되어 있습니다.");
        }

        s3Util.validateS3Image(newImageUrl);
        currentUser.updateProfileImage(newImageUrl);
        userRepository.save(currentUser);
        s3DeleteUtil.registerS3DeletionAfterCommit(oldImageUrl);

        log.info("[PROFILE] Image changed for user: {} ({} -> {})", currentUser.getId(), oldImageUrl, newImageUrl);

        return new SetUserProfileImageResDto(s3Util.toProfileMainResizedUrl(newImageUrl), "프로필 이미지가 성공적으로 변경되었습니다.");
    }

    @Transactional(readOnly = true)
    public GetHobbyInProgressResDto getHobbyInProgress(CustomUserDetails user, String userId) {
        User currentUser = userUtil.getCurrentUser(user);
        // 조회 대상 유저 확정 및 검증
        User targetUser = resolveTargetUser(currentUser, userId);
        // 취미 리스트 조회
        List<GetHobbyInProgressResDto.HobbyDto> hobbyList = hobbyRepository.findUserTabHobbyList(targetUser);

        // 썸네일 URL 가공 (커버 사이즈용)
        processHobbyThumbnailUrls(hobbyList);

        return GetHobbyInProgressResDto.of(targetUser, hobbyList);
    }

    @Cacheable(
            value = "userFeed",
            key = "#user.user.id + ':' + T(java.util.Objects).hashCode(#hobbyIds) + ':' + (#lastRecordId ?: 0) + ':' + #feedSize",
            condition = "#userId == null",
            unless = "#result == null"
    )
    @Transactional(readOnly = true)
    public GetUserFeedListResDto getUserFeedList(List<Long> hobbyIds, Long lastRecordId, Integer feedSize, CustomUserDetails user, String userId) {
        User currentUser = userUtil.getCurrentUser(user);
        String currentUserId = currentUser.getId();

        // 조회 대상 유저 및 권한 확정
        TargetUserInfo targetInfo = resolveTargetUserInfo(currentUserId, userId, currentUser);
        // 전체 개수 조회 (첫 페이지 진입 시에만)
        Long totalFeedCount = (lastRecordId == null)
                ? activityRecordRepository.countRecordByHobbyIds(hobbyIds, targetInfo.user().getId())
                : null;

        // 피드 목록 조회 (Slice 페이징을 위해 feedSize + 1)
        List<GetUserFeedListResDto.FeedDto> feedList = activityRecordRepository.findUserFeedList(
                hobbyIds, lastRecordId, feedSize + 1, targetInfo.user().getId(), targetInfo.visibilities(), currentUserId
        );
        // 썸네일 URL 리사이징 처리
        processFeedThumbnailUrls(feedList);

        return GetUserFeedListResDto.of(feedList, totalFeedCount, feedSize);
    }

    @Transactional(readOnly = true)
    public GetUserHobbyCardListResDto getUserHobbyCardList(Long lastHobbyCardId, Integer size, CustomUserDetails user, String userId) {
        String currentUserId = user.getUserId();
        String targetUserId = (userId == null) ? currentUserId : userId; // 조회하고자하는 유저

        if (userId != null) {
            User targetUser = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            List<FriendRelation> relations = activityRecordUtil.getRelations(currentUserId, targetUserId);
            activityRecordUtil.checkBlockedAndDeletedUser(relations, targetUser.isDeleted());
        }
        List<GetUserHobbyCardListResDto.HobbyCardDto> cardDtoList = hobbyCardRepository.findUserHobbyCardList(lastHobbyCardId, size, targetUserId);

        return GetUserHobbyCardListResDto.of(cardDtoList, size);
    }

    @Transactional(readOnly = true)
    public GetUserScrapListResDto getUserScrapList(Long lastScrapId, Integer size, CustomUserDetails user, String userId) {
        User currentUser = userUtil.getCurrentUser(user);
        String currentUserId = currentUser.getId();

        // 조회 대상 결정 (본인 또는 타인)
        String targetUserId = (userId == null) ? currentUserId : userId;
        boolean isMyScrap = targetUserId.equals(currentUserId);

        // 노출 권한(Visibility) 리스트 결정
        List<RecordVisibility> visibilities = resolveVisibilities(currentUserId, targetUserId, isMyScrap);

        // 데이터 조회 (Slice 방식 페이징을 위해 size + 1 조회)
        List<GetUserScrapListResDto.ScrapDto> scrapDtos = activityRecordScrapRepository.getScrapList(
                lastScrapId, size + 1, targetUserId, currentUserId, visibilities
        );

        // 이미지 URL 가공 및 결과 생성
        processThumbnailUrls(scrapDtos);

        long totalCount = (lastScrapId == null) ? activityRecordScrapRepository.countByUserId(targetUserId) : 0;

        return GetUserScrapListResDto.of(scrapDtos, totalCount, size);
    }

    private static boolean isSameImageCheck(String oldImageUrl, String newImageUrl) {
        return Objects.equals(oldImageUrl, newImageUrl);
    }

    private boolean isExistsByNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    private Optional<String> validateNicknameFormat(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return Optional.of("닉네임을 입력해주세요.");
        }

        if (nickname.length() > MAX_NICKNAME_LENGTH) {
            return Optional.of("닉네임은 " + MAX_NICKNAME_LENGTH + "자 이내로 입력해주세요.");
        }

        if (!NICKNAME_PATTERN.matcher(nickname).matches()) {
            return Optional.of("한글, 영어, 숫자만 사용할 수 있습니다.");
        }

        return Optional.empty();
    }

    private List<RecordVisibility> resolveVisibilities(String currentUserId, String targetUserId, boolean isMyScrap) {
        if (isMyScrap) {
            // 내 스크랩은 모든 권한(PUBLIC, FRIEND, PRIVATE) 조회 가능
            return List.of(RecordVisibility.PUBLIC, RecordVisibility.FRIEND, RecordVisibility.PRIVATE);
        }

        // 타인 조회 시 기본은 PUBLIC
        List<RecordVisibility> visibilities = new ArrayList<>(List.of(RecordVisibility.PUBLIC));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 차단 및 탈퇴 상태 체크
        List<FriendRelation> relations = activityRecordUtil.getRelations(currentUserId, targetUserId);
        activityRecordUtil.checkBlockedAndDeletedUser(relations, targetUser.isDeleted());

        // 팔로우 관계라면 FRIEND 권한 추가
        if (friendRelationRepository.existsByFriendship(currentUserId, targetUserId, FriendRelationStatus.FOLLOW)) {
            visibilities.add(RecordVisibility.FRIEND);
        }

        return visibilities;
    }

    private void processThumbnailUrls(List<GetUserScrapListResDto.ScrapDto> scrapDtos) {
        scrapDtos.stream()
                .filter(dto -> StringUtils.hasText(dto.getThumbnailImageUrl()))
                .forEach(dto -> dto.setThumbnailImageUrl(s3Util.toFeedThumbResizedUrl(dto.getThumbnailImageUrl())));
    }

    private TargetUserInfo resolveTargetUserInfo(String currentUserId, String targetUserId, User currentUser) {
        if (targetUserId == null || targetUserId.equals(currentUserId)) {
            // 내 피드 조회 시 모든 권한 반환
            return new TargetUserInfo(currentUser, List.of(RecordVisibility.PUBLIC, RecordVisibility.FRIEND, RecordVisibility.PRIVATE));
        }

        // 남의 피드 조회 시 검증 및 관계 확인
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        List<FriendRelation> relations = activityRecordUtil.getRelations(currentUserId, targetUserId);
        activityRecordUtil.checkBlockedAndDeletedUser(relations, targetUser.isDeleted());

        List<RecordVisibility> visibilities = new ArrayList<>(List.of(RecordVisibility.PUBLIC));
        if (friendRelationRepository.existsByFriendship(currentUserId, targetUser.getId(), FriendRelationStatus.FOLLOW)) {
            visibilities.add(RecordVisibility.FRIEND);
        }

        return new TargetUserInfo(targetUser, visibilities);
    }

    private void processFeedThumbnailUrls(List<GetUserFeedListResDto.FeedDto> feedList) {
        feedList.forEach(dto ->
                dto.setThumbnailImageUrl(s3Util.toFeedThumbResizedUrl(dto.getThumbnailImageUrl()))
        );
    }

    private void processHobbyThumbnailUrls(List<GetHobbyInProgressResDto.HobbyDto> hobbyList) {
        hobbyList.stream()
                .filter(dto -> StringUtils.hasText(dto.getThumbnailImageUrl()))
                .forEach(dto -> dto.setThumbnailImageUrl(s3Util.toCoverMainResizedUrl(dto.getThumbnailImageUrl())));
    }

    private User resolveTargetUser(User currentUser, String userId) {
        if (userId == null || Objects.equals(currentUser.getId(), userId)) {
            return currentUser;
        }

        User targetUser = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 차단 및 탈퇴 상태 체크
        List<FriendRelation> relations = activityRecordUtil.getRelations(currentUser.getId(), targetUser.getId());
        activityRecordUtil.checkBlockedAndDeletedUser(relations, targetUser.isDeleted());

        return targetUser;
    }
}
