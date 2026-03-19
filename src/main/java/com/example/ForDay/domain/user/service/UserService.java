package com.example.ForDay.domain.user.service;

import com.example.ForDay.domain.friend.entity.FriendRelation;
import com.example.ForDay.domain.friend.repository.FriendRelationRepository;
import com.example.ForDay.domain.friend.type.FriendRelationStatus;
import com.example.ForDay.domain.hobby.repository.HobbyCardRepository;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.record.repository.ActivityRecordReportRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordScrapRepository;
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
import com.example.ForDay.infra.s3.util.S3Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Transactional
    public User createOauth(String socialId, String email, SocialType socialType) {
        return userRepository.save(User.builder()
                .role(Role.USER)
                .email(email)
                .socialType(socialType)
                .socialId(socialId)
                .build());
    }

    @Transactional(readOnly = true)
    public NicknameCheckResDto nicknameCheck(String nickname) {
        // 형식 검증 (길이, 허용 문자)
        Optional<String> validationMessage = validateNicknameFormat(nickname);
        if (validationMessage.isPresent()) {
            return new NicknameCheckResDto(nickname, false, validationMessage.get());
        }

        // 중복 검증 (DB 조회)
        if (userRepository.existsByNickname(nickname)) {
            return new NicknameCheckResDto(nickname, false, "이미 사용 중인 닉네임입니다.");
        }

        // 사용 가능 응답
        return new NicknameCheckResDto(nickname, true, "사용 가능한 쿼리입니다.");
    }

    @Transactional
    public NicknameRegisterResDto nicknameRegister(String nickname, CustomUserDetails user) {
        boolean exists = userRepository.existsByNickname(nickname);
        if (exists) {
            throw new CustomException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        User currentUser = userUtil.getCurrentUser(user);
        currentUser.changeNickname(nickname);
        userRepository.save(currentUser);

        return new NicknameRegisterResDto("사용자 이름이 성공적으로 등록되었습니다.", currentUser.getNickname());
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
        return new UserInfoResDto(s3Util.toProfileMainResizedUrl(targetUser.getProfileImageUrl()), // 프로필 조회용 url로 수정
                targetUser.getNickname(),
                totalStickerCount);
    }

    @Transactional
    public SetUserProfileImageResDto setUserProfileImage(SetUserProfileImageReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        String newImageUrl = StringUtils.hasText(reqDto.getProfileImageUrl()) ? reqDto.getProfileImageUrl() : null;
        String oldImageUrl = currentUser.getProfileImageUrl();

        // 동일 이미지 여부 체크
        if (Objects.equals(oldImageUrl, newImageUrl)) {
            return new SetUserProfileImageResDto(s3Util.toProfileMainResizedUrl(oldImageUrl),"이미 동일한 프로필 이미지로 설정되어 있습니다."
            );
        }

        // 새로운 이미지가 있는 경우에만 S3 존재 여부 검증
        validateNewImage(newImageUrl);

        // 상태 업데이트
        currentUser.updateProfileImage(newImageUrl);
        userRepository.save(currentUser);

        // 이전 이미지가 실제 S3 객체였다면 삭제 등록
        if (StringUtils.hasText(oldImageUrl)) {
            registerS3Deletion(oldImageUrl);
        }

        log.info("[PROFILE] Image changed for user: {} ({} -> {})",
                currentUser.getId(), oldImageUrl, newImageUrl);

        return new SetUserProfileImageResDto(
                s3Util.toProfileMainResizedUrl(newImageUrl),
                "프로필 이미지가 성공적으로 변경되었습니다."
        );
    }

    @Transactional(readOnly = true)
    public GetHobbyInProgressResDto getHobbyInProgress(CustomUserDetails user, String userId) {
        User currentUser = userUtil.getCurrentUser(user);

        // 조회 대상 유저 확정 및 검증
        User targetUser = resolveTargetUser(currentUser, userId);

        // 취미 리스트 조회
        List<GetHobbyInProgressResDto.HobbyDto> hobbyList = hobbyRepository.findUserTabHobbyList(targetUser);

        // 진행 중인 취미 개수 카운트
        int inProgressCount = countInProgressHobbies(hobbyList);

        // 썸네일 URL 가공 (커버 사이즈용)
        processHobbyThumbnailUrls(hobbyList);

        return new GetHobbyInProgressResDto(
                inProgressCount,
                targetUser.getHobbyCardCount(),
                hobbyList
        );
    }

    private User resolveTargetUser(User currentUser, String userId) {
        if (userId == null || userId.equals(currentUser.getId())) {
            return currentUser;
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 차단 및 탈퇴 상태 체크
        checkBlockedAndDeletedUser(currentUser.getId(), targetUser.getId(), targetUser.isDeleted());

        return targetUser;
    }

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

        // 페이징 결과 처리
        boolean hasNext = feedList.size() > feedSize;
        if (hasNext) {
            feedList.remove(feedSize.intValue());
        }

        // 썸네일 URL 리사이징 처리
        processFeedThumbnailUrls(feedList);

        Long lastId = feedList.isEmpty() ? null : feedList.get(feedList.size() - 1).getRecordId();

        return new GetUserFeedListResDto(totalFeedCount, lastId, feedList, hasNext);
    }

    @Transactional(readOnly = true)
    public GetUserHobbyCardListResDto getUserHobbyCardList(Long lastHobbyCardId, Integer size, CustomUserDetails user, String userId) {
        String currentUserId = user.getUserId();
        String targetUserId = (userId == null) ? currentUserId : userId; // 조회하고자하는 유저

        if(userId != null) {
            User targetUser = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            List<FriendRelation> relations = friendRelationRepository.findAllRelationsBetween(currentUserId, targetUserId);
            checkBlockedAndDeletedUser(relations, targetUser.isDeleted());
        }
        List<GetUserHobbyCardListResDto.HobbyCardDto> cardDtoList = hobbyCardRepository.findUserHobbyCardList(lastHobbyCardId, size, targetUserId);

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

        // 페이징 결과 처리 (HasNext 판단 및 데이터 절삭)
        boolean hasNext = scrapDtos.size() > size;
        if (hasNext) {
            scrapDtos.remove(size.intValue());
        }

        // 이미지 URL 가공 및 결과 생성
        processThumbnailUrls(scrapDtos);

        long totalCount = (lastScrapId == null) ? activityRecordScrapRepository.countByUserId(targetUserId) : 0;
        Long nextLastId = scrapDtos.isEmpty() ? null : scrapDtos.get(scrapDtos.size() - 1).getScrapId();

        return new GetUserScrapListResDto(totalCount, nextLastId, scrapDtos, hasNext);
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
        checkBlockedAndDeletedUser(currentUserId, targetUser.getId(), targetUser.isDeleted());

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

    private void checkBlockedAndDeletedUser(String currentUserId, String targetId, boolean deleted) {
        // 한쪽이라도 차단 관계가 있는지 확인
        if(friendRelationRepository.existsByFriendship(currentUserId, targetId, FriendRelationStatus.BLOCK) || friendRelationRepository.existsByFriendship(targetId, currentUserId, FriendRelationStatus.BLOCK)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 타겟유저가 탈퇴한 회원인 경우
        if(deleted) throw new CustomException(ErrorCode.USER_NOT_FOUND);
    }

    private void checkBlockedAndDeletedUser(List<FriendRelation> relations, boolean deleted) {
        // 리스트에서 차단(BLOCK)이 하나라도 있는지 확인
        boolean isBlocked = relations.stream()
                .anyMatch(f -> f.getRelationStatus() == FriendRelationStatus.BLOCK);

        if (isBlocked || deleted) {
            throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);
        }
    }

    private void validateNewImage(String targetUrl) {
        if (targetUrl != null) {
            String newKey = s3Service.extractKeyFromFileUrl(targetUrl);
            if (!s3Service.existsByKey(newKey)) {
                throw new CustomException(ErrorCode.S3_IMAGE_NOT_FOUND);
            }
        }
    }

    private void registerS3Deletion(String oldImageUrl) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("[S3-Cleanup] 이전 이미지 삭제 시작: {}", oldImageUrl);
                try {
                    // 리스트 형태로 만들어서 한 번에 삭제 로직 실행
                    List<String> keysToDelete = Stream.of(
                            oldImageUrl,
                            s3Util.toProfileMainResizedUrl(oldImageUrl),
                            s3Util.toProfileListResizedUrl(oldImageUrl)
                    ).map(s3Service::extractKeyFromFileUrl).toList();

                    keysToDelete.forEach(s3Service::deleteByKey);
                } catch (Exception e) {
                    log.error("[S3-Cleanup] 이전 프로필 이미지 삭제 실패: {}", oldImageUrl, e);
                }
            }
        });
    }

    private TargetUserInfo resolveTargetUserInfo(String currentUserId, String targetUserId, User currentUser) {
        if (targetUserId == null || targetUserId.equals(currentUserId)) {
            // 내 피드 조회 시 모든 권한 반환
            return new TargetUserInfo(currentUser, List.of(RecordVisibility.PUBLIC, RecordVisibility.FRIEND, RecordVisibility.PRIVATE));
        }

        // 남의 피드 조회 시 검증 및 관계 확인
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        checkBlockedAndDeletedUser(currentUserId, targetUser.getId(), targetUser.isDeleted());

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

    private record TargetUserInfo(User user, List<RecordVisibility> visibilities) {}

    private int countInProgressHobbies(List<GetHobbyInProgressResDto.HobbyDto> hobbyList) {
        return (int) hobbyList.stream()
                .filter(h -> h.getStatus() == HobbyStatus.IN_PROGRESS)
                .count();
    }

    private void processHobbyThumbnailUrls(List<GetHobbyInProgressResDto.HobbyDto> hobbyList) {
        hobbyList.stream()
                .filter(dto -> StringUtils.hasText(dto.getThumbnailImageUrl()))
                .forEach(dto -> dto.setThumbnailImageUrl(s3Util.toCoverMainResizedUrl(dto.getThumbnailImageUrl())));
    }
}
