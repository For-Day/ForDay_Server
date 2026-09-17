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
import com.example.ForDay.domain.record.utils.ActivityRecordUtil;
import com.example.ForDay.domain.user.dto.TargetUserInfo;
import com.example.ForDay.domain.user.dto.request.SetUserProfileImageReqDto;
import com.example.ForDay.domain.user.dto.response.*;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.domain.user.type.SocialType;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.common.response.message.UserSuccessCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.global.util.ImageUrlConverter;
import com.example.ForDay.global.port.ImageLifecyclePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9]+$");
    private static final int MAX_NICKNAME_LENGTH = 10;

    private final UserRepository userRepository;
    private final UserUtil userUtil;
    private final HobbyRepository hobbyRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final HobbyCardRepository hobbyCardRepository;
    private final FriendRelationRepository friendRelationRepository;
    private final ActivityRecordScrapRepository activityRecordScrapRepository;
    private final ImageUrlConverter imageUrlConverter;
    private final ImageLifecyclePort imageLifecyclePort;
    private final ActivityRecordUtil activityRecordUtil;
    private final NotificationService notificationService;

    @Transactional
    public User createOauth(String socialId, String email, SocialType socialType) {
        return userRepository.save(User.createOauth(socialId, email, socialType));
    }

    @Transactional(readOnly = true)
    public NicknameCheckResDto nicknameCheck(String nickname, User user) {
        Optional<String> validationMessage = validateNicknameFormat(nickname);
        if (validationMessage.isPresent()) {
            return new NicknameCheckResDto(nickname, false, validationMessage.get());
        }

        if (isExistsByNickname(nickname, user)) {
            return NicknameCheckResDto.alreadyUsedNickname(nickname);
        }

        return NicknameCheckResDto.canUseNickname(nickname);
    }

    @Transactional
    public NicknameRegisterResDto nicknameRegister(String nickname, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        if (isExistsByNickname(nickname, currentUser)) {
            throw new CustomException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        currentUser.changeNickname(nickname);
        userRepository.save(currentUser);

        return NicknameRegisterResDto.from(currentUser.getNickname());
    }

    @Transactional(readOnly = true)
    public UserInfoResDto getUserInfo(CustomUserDetails user, String userId) {
        User targetUser;
        String targetId;

        if (userId != null) {
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
        return UserInfoResDto.of(targetUser, totalStickerCount, imageUrlConverter, userId == null ? notificationService.unreadNotificationExists(targetUser) : false
        );
    }

    @Transactional
    public SetUserProfileImageResDto setUserProfileImage(SetUserProfileImageReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        String newImageUrl = StringUtils.hasText(reqDto.getProfileImageUrl()) ? reqDto.getProfileImageUrl() : null;
        String oldImageUrl = currentUser.getProfileImageUrl();

        if (isSameImageCheck(oldImageUrl, newImageUrl)) {
            return new SetUserProfileImageResDto(imageUrlConverter.toProfileMainResizedUrl(oldImageUrl), UserSuccessCode.ALREADY_SAME_PROFILE_IMAGE.getMessage());
        }

        imageLifecyclePort.validateExists(newImageUrl);
        currentUser.updateProfileImage(newImageUrl);
        userRepository.save(currentUser);
        imageLifecyclePort.deleteAfterCommit(oldImageUrl);

        log.info("[PROFILE] Image changed for user: {} ({} -> {})", currentUser.getId(), oldImageUrl, newImageUrl);

        return new SetUserProfileImageResDto(imageUrlConverter.toProfileMainResizedUrl(newImageUrl), UserSuccessCode.UPDATE_PROFILE_IMAGE_SUCCESS.getMessage());
    }

    @Transactional(readOnly = true)
    public GetHobbyInProgressResDto getHobbyInProgress(CustomUserDetails user, String userId) {
        User currentUser = userUtil.getCurrentUser(user);
        User targetUser = resolveTargetUser(currentUser, userId);
        List<GetHobbyInProgressResDto.HobbyDto> hobbyList = hobbyRepository.findUserTabHobbyList(targetUser);
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

        TargetUserInfo targetInfo = resolveTargetUserInfo(currentUserId, userId, currentUser);

        Long totalFeedCount = (lastRecordId == null)
                ? activityRecordRepository.countRecordByHobbyIds(hobbyIds, targetInfo.user().getId())
                : null;

        List<GetUserFeedListResDto.FeedDto> feedList = activityRecordRepository.findUserFeedList(
                hobbyIds, lastRecordId, feedSize + 1, targetInfo.user().getId(), targetInfo.visibilities(), currentUserId
        );
        processFeedThumbnailUrls(feedList);

        return GetUserFeedListResDto.of(feedList, totalFeedCount, feedSize);
    }

    @Transactional(readOnly = true)
    public GetUserHobbyCardListResDto getUserHobbyCardList(Long lastHobbyCardId, Integer size, CustomUserDetails user, String userId) {
        String currentUserId = user.getUserId();
        String targetUserId = (userId == null) ? currentUserId : userId;

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
        String targetUserId = (userId == null) ? currentUserId : userId;
        boolean isMyScrap = targetUserId.equals(currentUserId);

        List<RecordVisibility> visibilities = resolveVisibilities(currentUserId, targetUserId, isMyScrap);
        List<GetUserScrapListResDto.ScrapDto> scrapDtos = activityRecordScrapRepository.getScrapList(
                lastScrapId, size + 1, targetUserId, currentUserId, visibilities
        );
        processThumbnailUrls(scrapDtos);

        long totalCount = (lastScrapId == null) ? activityRecordScrapRepository.countByUserId(targetUserId) : 0;
        return GetUserScrapListResDto.of(scrapDtos, totalCount, size);
    }

    private static boolean isSameImageCheck(String oldImageUrl, String newImageUrl) {
        return Objects.equals(oldImageUrl, newImageUrl);
    }

    private boolean isExistsByNickname(String nickname, User currentUser) {
        return userRepository.existsByNicknameAndIdNot(nickname, currentUser.getId());
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
            return List.of(RecordVisibility.PUBLIC, RecordVisibility.FRIEND, RecordVisibility.PRIVATE);
        }

        List<RecordVisibility> visibilities = new ArrayList<>(List.of(RecordVisibility.PUBLIC));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<FriendRelation> relations = activityRecordUtil.getRelations(currentUserId, targetUserId);
        activityRecordUtil.checkBlockedAndDeletedUser(relations, targetUser.isDeleted());

        if (friendRelationRepository.existsByFriendship(currentUserId, targetUserId, FriendRelationStatus.FOLLOW)) {
            visibilities.add(RecordVisibility.FRIEND);
        }

        return visibilities;
    }

    private void processThumbnailUrls(List<GetUserScrapListResDto.ScrapDto> scrapDtos) {
        scrapDtos.stream()
                .filter(dto -> StringUtils.hasText(dto.getThumbnailImageUrl()))
                .forEach(dto -> dto.setThumbnailImageUrl(imageUrlConverter.toFeedThumbResizedUrl(dto.getThumbnailImageUrl())));
    }

    private TargetUserInfo resolveTargetUserInfo(String currentUserId, String targetUserId, User currentUser) {
        if (targetUserId == null || targetUserId.equals(currentUserId)) {
            return new TargetUserInfo(currentUser, List.of(RecordVisibility.PUBLIC, RecordVisibility.FRIEND, RecordVisibility.PRIVATE));
        }

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
                dto.setThumbnailImageUrl(imageUrlConverter.toFeedThumbResizedUrl(dto.getThumbnailImageUrl()))
        );
    }

    private void processHobbyThumbnailUrls(List<GetHobbyInProgressResDto.HobbyDto> hobbyList) {
        hobbyList.stream()
                .filter(dto -> StringUtils.hasText(dto.getThumbnailImageUrl()))
                .forEach(dto -> dto.setThumbnailImageUrl(imageUrlConverter.toCoverMainResizedUrl(dto.getThumbnailImageUrl())));
    }

    private User resolveTargetUser(User currentUser, String userId) {
        if (userId == null || Objects.equals(currentUser.getId(), userId)) {
            return currentUser;
        }

        User targetUser = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<FriendRelation> relations = activityRecordUtil.getRelations(currentUser.getId(), targetUser.getId());
        activityRecordUtil.checkBlockedAndDeletedUser(relations, targetUser.isDeleted());

        return targetUser;
    }
}
