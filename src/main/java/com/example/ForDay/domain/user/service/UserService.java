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
import java.util.stream.Stream;

@Slf4j
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
    private final S3Util s3Util;
    private final ActivityRecordReportRepository reportRepository;

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
        String newImageUrl = reqDto.getProfileImageUrl();

        String currentUrl = StringUtils.hasText(currentUser.getProfileImageUrl()) ? currentUser.getProfileImageUrl() : null;
        String targetUrl = StringUtils.hasText(newImageUrl) ? newImageUrl : null;

        if (Objects.equals(currentUrl, targetUrl)) {
            return new SetUserProfileImageResDto(currentUrl, "이미 동일한 프로필 이미지로 설정되어 있습니다.");
        }

        validateNewImage(targetUrl);

        String oldImageUrl = currentUser.getProfileImageUrl();
        currentUser.updateProfileImage(newImageUrl);
        userRepository.save(currentUser);

        if (StringUtils.hasText(oldImageUrl)) {
            registerS3Deletion(oldImageUrl);
        }

        return new SetUserProfileImageResDto(
                s3Util.toProfileMainResizedUrl(newImageUrl),
                "프로필 이미지가 성공적으로 변경되었습니다."
        );
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
        hobbyList.forEach(hobbyDto -> {
            String originalUrl = hobbyDto.getThumbnailImageUrl();
            if (StringUtils.hasText(originalUrl)) {
                // s3Util을 사용하여 /temp/ -> /resized/thumb/ 경로로 변환
                String thumbUrl = s3Util.toCoverMainResizedUrl(originalUrl);
                hobbyDto.setThumbnailImageUrl(thumbUrl);
            }
        });

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

            if (friendRelationRepository.existsByFriendship(
                    currentUserId, targetUser.getId(), FriendRelationStatus.FOLLOW)) {
                visibilities.add(RecordVisibility.FRIEND);
            }
        }
        String targetUserId = targetUser.getId();
        List<Long> reportedRecordIds = reportRepository.findReportedRecordIdsByReporterId(currentUserId);

        Long totalFeedCount = null;
        if(lastRecordId == null) {
            totalFeedCount = activityRecordRepository.countRecordByHobbyIds(hobbyIds, targetUserId);
        }

        List<GetUserFeedListResDto.FeedDto> feedList = activityRecordRepository.findUserFeedList(hobbyIds, lastRecordId, feedSize, targetUserId, visibilities, reportedRecordIds, currentUserId);

        boolean hasNext = false;
        if (feedList.size() > feedSize) {
            hasNext = true;
            feedList.remove(feedSize.intValue());
        }

        // resized url 호출하도록 수정
        feedList.forEach(feedDto -> {
            feedDto.setThumbnailImageUrl(
                    s3Util.toFeedThumbResizedUrl(feedDto.getThumbnailImageUrl())
            );
        });

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
            checkBlockedAndDeletedUser(relations, currentUserId, targetUserId, targetUser.isDeleted());
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
        String targetUserId = (userId == null) ? currentUser.getId() : userId;

        if (userId != null) {
            User targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            List<FriendRelation> relations = friendRelationRepository.findAllRelationsBetween(currentUserId, targetUserId);
            checkBlockedAndDeletedUser(relations, currentUserId, targetUserId, targetUser.isDeleted());
        }

        List<GetUserScrapListResDto.ScrapDto> scrapDtos;
        if (userId == null) {
            scrapDtos = activityRecordScrapRepository.getMyScrapList(lastScrapId, size, currentUser.getId());
        } else {
            List<String> myFriendIds = friendRelationRepository.findAllFriendIdsByUserId(currentUser.getId());
            List<String> blockFriendIds = friendRelationRepository.findAllBlockedIdsByUserId(currentUser.getId());
            List<Long> reportedRecordIds = reportRepository.findReportedRecordIdsByReporterId(currentUser.getId());
            scrapDtos = activityRecordScrapRepository.getOtherScrapList(
                    lastScrapId, size, targetUserId, currentUser.getId(), myFriendIds, blockFriendIds, reportedRecordIds
            );
        }

        boolean hasNext = false;
        if (scrapDtos.size() > size) {
            hasNext = true;
            scrapDtos.remove(size.intValue());
        }

        long scrapCount = (lastScrapId == null) ? activityRecordScrapRepository.countByUserId(targetUserId) : 0; // 혹은 count 쿼리 별도 실행

        // 썸네일용 이미지 url 반환
        scrapDtos.forEach(scrapDto -> {
            if (StringUtils.hasText(scrapDto.getThumbnailImageUrl())) {
                scrapDto.setThumbnailImageUrl(s3Util.toFeedThumbResizedUrl(scrapDto.getThumbnailImageUrl()));
            }
        });

        Long lastId = scrapDtos.isEmpty() ? null : scrapDtos.get(scrapDtos.size() - 1).getScrapId();
        return new GetUserScrapListResDto(scrapCount, lastId, scrapDtos, hasNext);
    }

    private void checkBlockedAndDeletedUser(String currentUserId, String targetId, boolean deleted) {
        // 한쪽이라도 차단 관계가 있는지 확인
        if(friendRelationRepository.existsByFriendship(currentUserId, targetId, FriendRelationStatus.BLOCK) || friendRelationRepository.existsByFriendship(targetId, currentUserId, FriendRelationStatus.BLOCK)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 타겟유저가 탈퇴한 회원인 경우
        if(deleted) throw new CustomException(ErrorCode.USER_NOT_FOUND);
    }

    private void checkBlockedAndDeletedUser(List<FriendRelation> relations, String me, String target, boolean deleted) {
        // 리스트에서 차단(BLOCK)이 하나라도 있는지 확인
        boolean isBlocked = relations.stream()
                .anyMatch(f -> f.getRelationStatus() == FriendRelationStatus.BLOCK);

        if (isBlocked || deleted) {
            throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);
        }
    }
}
