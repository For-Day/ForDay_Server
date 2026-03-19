package com.example.ForDay.domain.record.service.v1;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.activity.service.TodayRecordRedisService;
import com.example.ForDay.domain.friend.entity.FriendRelation;
import com.example.ForDay.domain.friend.repository.FriendRelationRepository;
import com.example.ForDay.domain.friend.type.FriendRelationStatus;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.recent.service.RecentRedisService;
import com.example.ForDay.domain.record.dto.*;
import com.example.ForDay.domain.record.dto.request.ReportActivityRecordReqDto;
import com.example.ForDay.domain.record.dto.request.UpdateActivityRecordReqDto;
import com.example.ForDay.domain.record.dto.request.UpdateRecordVisibilityReqDto;
import com.example.ForDay.domain.record.dto.response.*;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.entity.ActivityRecordReaction;
import com.example.ForDay.domain.record.entity.ActivityRecordReport;
import com.example.ForDay.domain.record.entity.ActivityRecordScrap;
import com.example.ForDay.domain.record.repository.ActivityRecordReactionRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordReportRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordScrapRepository;
import com.example.ForDay.domain.record.service.RedisReactionService;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.record.type.StoryFilterType;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.TimeUtil;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.infra.s3.service.S3Service;
import com.example.ForDay.infra.s3.util.S3Util;
import io.jsonwebtoken.lang.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityRecordService {
    private final ActivityRecordRepository activityRecordRepository;
    private final UserUtil userUtil;
    private final FriendRelationRepository friendRelationRepository;
    private final ActivityRecordReactionRepository recordReactionRepository;
    private final S3Service s3Service;
    private final ActivityRepository activityRepository;
    private final ActivityRecordScrapRepository activityRecordScrapRepository;
    private final ActivityRecordReportRepository activityRecordReportRepository;
    private final HobbyRepository hobbyRepository;
    private final RecentRedisService recentRedisService;
    private final S3Util s3Util;
    private final ActivityRecordReactionRepository reactionRepository;
    private final ActivityRecordReportRepository reportRepository;
    private final ActivityRecordScrapRepository scrapRepository;
    private final TodayRecordRedisService todayRecordRedisService;
    private final RedisReactionService redisReactionService;
    private final UserRepository userRepository;

    // 이제 사용 x
    @Transactional(readOnly = true)
    public GetRecordDetailResDto getRecordDetail(Long recordId, CustomUserDetails user) {
        RecordDetailQueryDto detail = activityRecordRepository.findDetailDtoById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        if (detail.recordDeleted()) {
            throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);
        }

        String currentUserId = userUtil.getCurrentUser(user).getId();
        boolean isRecordOwner = Objects.equals(currentUserId, detail.writerId());
        log.info("[getRecordDetail] 권한 확인 - WriterId: {}, IsOwner: {}", detail.writerId(), isRecordOwner);

        // 차단 여부와 탈퇴 회원 여부 확인
        checkBlockedAndDeletedUser(currentUserId, detail.writerId(), detail.writerDeleted());

        if (!isRecordOwner) {
            log.debug("[getRecordDetail] 비소유자 접근 - 공개 범위 검증 시작 (Visibility: {})", detail.visibility());
            validateRecordAuthority(detail.visibility(), detail.writerId(), currentUserId);
        }

        List<ReactionSummary> summaries = recordReactionRepository.findReactionSummariesByRecordId(recordId);
        GetRecordDetailResDto.UserReactionDto userReaction = createUserReactionDto(summaries, currentUserId);
        GetRecordDetailResDto.NewReactionDto newReaction = createNewReactionDto(summaries, isRecordOwner);

        boolean scraped = activityRecordScrapRepository.existsByScrap(detail.recordId(), currentUserId);

        log.info("[getRecordDetail] 조회 성공 - RecordId: {}, Writer: {}, Reactions: {}, Scraped: {}",
                recordId, detail.writerId(), summaries.size(), scraped);

        return buildGetRecordDetailResDtoFromDto(detail, isRecordOwner, newReaction, userReaction, scraped);
    }

    @Transactional
    public GetRecordReactionUsersResDto getRecordReactionUsers(
            Long recordId, RecordReactionType type, CustomUserDetails user, String lastUserId, Integer size
    ) {
        // 엔티티 전체 대신 권한 확인용 DTO만 조회 (Fetch Join 제거 효과)
        RecordDetailQueryDto recordDetail = activityRecordRepository.findDetailDtoById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        if (recordDetail.recordDeleted()) {
            throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);
        }

        String currentUserId = userUtil.getCurrentUser(user).getId();

        List<FriendRelation> relations = friendRelationRepository.findAllRelationsBetween(currentUserId, recordDetail.writerId());
        checkBlockedAndDeletedUser(relations, currentUserId, recordDetail.writerId(), recordDetail.writerDeleted());
        validateRecordAuthority(relations, recordDetail.visibility(), recordDetail.writerId(), currentUserId);

        boolean isRecordOwner = Objects.equals(currentUserId, recordDetail.writerId());
        // 리포지토리에서 DTO(ReactionUserInfo)로 직접 조회하여 N+1 및 오버페칭 방지
        List<GetRecordReactionUsersResDto.ReactionUserInfo> reactionUsers =
                recordReactionRepository.findReactionUsersDtoByType(recordId, type, lastUserId, size, isRecordOwner);

        // 다음 페이지 여부 확인
        boolean hasNext = reactionUsers.size() > size;
        if (hasNext) reactionUsers.remove(size.intValue());

        // 게시글 주인인 경우에만 벌크 업데이트 실행
        if (isRecordOwner) {
            recordReactionRepository.markAsReadByRecordIdAndType(recordId, type);
        }

        String nextLastUserId = reactionUsers.isEmpty() ? null : reactionUsers.get(reactionUsers.size() - 1).getUserId();

        return new GetRecordReactionUsersResDto(type, reactionUsers, hasNext, nextLastUserId);
    }

    @Transactional
    public ReactToRecordResDto reactToRecord(
            Long recordId,
            RecordReactionType type,
            CustomUserDetails user
    ) {
        // 현재 로그인한 사용자 조회
        User currentUser = userUtil.getCurrentUser(user);
        // 기록 조회 + 삭제 여부 검증
        ReportActivityRecordDto record = getValidRecord(recordId);
        // 차단 여부, 친구 관계, 공개 범위 등 접근 권한 검증
        validateAccess(record, currentUser);
        // 동일 유저의 동일 타입 리액션 중복 여부 체크
        validateDuplicateReaction(recordId, currentUser.getId(), type);
        // 리액션 엔티티 생성 및 저장
        saveReaction(recordId, currentUser.getId(), type);
        // 리액션 증가에 따른 랭킹 점수 업데이트 (Redis)
        updateRanking(record.getRecordId());
        // 최종 응답 반환
        return new ReactToRecordResDto(
                "반응이 정상적으로 등록되었습니다.",
                type,
                recordId
        );
    }

    @Transactional
    public UpdateRecordVisibilityResDto updateRecordVisibility(Long recordId, UpdateRecordVisibilityReqDto reqDto, CustomUserDetails user) {
        ActivityRecord activityRecord = getActivityRecord(recordId);
        verifyRecordOwner(activityRecord, userUtil.getCurrentUser(user));

        RecordVisibility previous = activityRecord.getVisibility();
        RecordVisibility next = reqDto.getVisibility();

        if (previous == next) {
            return new UpdateRecordVisibilityResDto("이미 설정된 공개 범위입니다.", previous, next);
        }
        activityRecord.updateVisibility(next);
        return new UpdateRecordVisibilityResDto("공개 범위가 정상적으로 변경되었습니다.", previous, next);
    }

    @Transactional
    public CancelReactToRecordResDto cancelReactToRecord(Long recordId, RecordReactionType type, CustomUserDetails user) {
        String userId = user.getUserId();
        int deletedCount = recordReactionRepository.deleteByRecordIdAndUserIdAndType(recordId, userId, type);
        if (deletedCount == 0) {
            throw new CustomException(ErrorCode.REACTION_NOT_FOUND);
        }

        // Redis 점수 차감
        redisReactionService.decrementRankingScore(recordId);

        log.info("[cancelReactToRecord] 리액션 취소 완료 - RecordId: {}, UserId: {}", recordId, userId);
        return new CancelReactToRecordResDto("리액션이 정상적으로 취소되었습니다.", type, recordId);
    }

    @Transactional
    public UpdateActivityRecordResDto updateActivityRecord(
            Long recordId,
            UpdateActivityRecordReqDto reqDto,
            CustomUserDetails user
    ) {
        User currentUser = userUtil.getCurrentUser(user);

        ActivityRecord record = getActivityRecord(recordId, currentUser);
        Activity activity = getActivity(reqDto.getActivityId(), currentUser);

        handleImageUpdate(record.getImageUrl(), reqDto.getImageUrl());

        record.updateRecord(
                activity,
                reqDto.getSticker(),
                reqDto.getMemo(),
                reqDto.getVisibility(),
                reqDto.getImageUrl()
        );

        return new UpdateActivityRecordResDto(
                "활동 기록이 정상적으로 수정되었습니다.",
                activity.getId(),
                activity.getContent(),
                record.getSticker(),
                record.getMemo(),
                record.getImageUrl(),
                record.getVisibility()
        );
    }

    // 리팩토링 필요
    @Transactional
    public DeleteActivityRecordResDto deleteActivityRecord(Long recordId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        String currentUserId = currentUser.getId();
        ActivityRecord activityRecord = activityRecordRepository.findByIdAndUserId(recordId, currentUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        // 이미 삭제된 경우 예외 처리
        if (activityRecord.isDeleted()) {
            throw new CustomException(ErrorCode.ALREADY_DELETED_RECORD);
        }

        reactionRepository.deleteByActivityRecord(activityRecord);
        reportRepository.deleteByReportedRecord(activityRecord);
        scrapRepository.deleteByActivityRecord(activityRecord);

        String deleteImageUrl = activityRecord.getImageUrl();

        boolean isToday = activityRecord.getCreatedAt().toLocalDate().equals(LocalDate.now());

        if (isToday) {
            activityRecord.getActivity().deleteRecord();
            activityRecord.getHobby().deleteRecord();
            todayRecordRedisService.deleteTodayRecordKey(currentUserId, activityRecord.getHobby().getId());
            activityRecordRepository.delete(activityRecord);
        } else {
            activityRecord.deleteRecord();
        }

        if (StringUtils.hasText(deleteImageUrl)) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        String deletedImageKey = s3Service.extractKeyFromFileUrl(deleteImageUrl);
                        String feedThumbResizedUrl = s3Util.toFeedThumbResizedUrl(deleteImageUrl);
                        String feedThumbResizedKey = s3Service.extractKeyFromFileUrl(feedThumbResizedUrl);

                        s3Service.deleteByKey(deletedImageKey);
                        s3Service.deleteByKey(feedThumbResizedKey);

                    } catch (Exception e) {
                        log.error("S3 파일 삭제 실패 (DB는 정상 삭제됨): {}", deleteImageUrl, e);
                    }
                }
            });
        }

        return new DeleteActivityRecordResDto("활동 기록이 삭제되었어요.", activityRecord.getId(), deleteImageUrl);
    }

    @Transactional
    public AddActivityRecordScrapResDto addActivityRecordScrap(
            Long recordId,
            CustomUserDetails user
    ) {
        User currentUser = userUtil.getCurrentUser(user);

        ActivityRecordWithUserDto record = getAccessibleRecordWithUser(recordId, currentUser);
        validateDuplicateScrap(recordId, currentUser.getId());
        saveScrap(recordId, currentUser);

        return new AddActivityRecordScrapResDto("스크랩을 완료했어요.", recordId, true);
    }

    @Transactional
    public DeleteActivityRecordScrapResDto deleteActivityRecordScrap(
            Long recordId,
            CustomUserDetails user
    ) {
        User currentUser = userUtil.getCurrentUser(user);

        ActivityRecordWithUserDto record = getAccessibleRecordWithUser(recordId, currentUser);
        Optional<ActivityRecordScrap> scrap =
                findScrap(recordId, currentUser.getId());

        if (scrap.isEmpty()) {
            return new DeleteActivityRecordScrapResDto(
                    "스크랩이 존재하지 않거나 이미 삭제되었습니다.",
                    recordId,
                    false
            );
        }
        deleteScrap(scrap.get());

        return new DeleteActivityRecordScrapResDto(
                "스크랩 취소가 완료되었습니다.",
                recordId,
                false
        );
    }

    @Transactional
    public ReportActivityRecordResDto reportActivityRecord(
            Long recordId,
            ReportActivityRecordReqDto reqDto,
            CustomUserDetails user
    ) {
        User currentUser = userUtil.getCurrentUser(user);

        log.info("[reportActivityRecord] 신고 요청 - recordId={}, reporter={}",
                recordId, currentUser.getId());

        // 기록 조회 + 접근 권한 검증
        ReportActivityRecordDto record = getAccessibleReportRecord(recordId, currentUser);

        // 중복 신고 방지
        validateDuplicateReport(record.getRecordId(), currentUser.getId());

        // 신고 저장
        saveReport(record, currentUser, reqDto.getReason());

        log.info("[reportActivityRecord] 신고 완료 - recordId={}", recordId);

        return new ReportActivityRecordResDto(
                record.getRecordId(),
                record.getWriterId(),
                record.getWriterNickname(),
                "기록이 정상적으로 신고되었습니다."
        );
    }


    @Transactional(readOnly = true)
    public GetActivityRecordByStoryResDto getActivityRecordByStory(
            Long hobbyId, Long lastRecordId, Integer size, String keyword,
            CustomUserDetails user, StoryFilterType storyFilterType) {

        User currentUser = userUtil.getCurrentUser(user);
        log.info("[getActivityRecordByStory] 조회 시작 - User: {}, Filter: {}, Keyword: {}",
                currentUser.getId(), storyFilterType, keyword);

        // 검색어 저장
        saveRecentKeywordIfPresent(currentUser.getId(), keyword);

        // 상단 탭 정보 조회 (첫 페이지 조회 시에만)
        List<GetActivityRecordByStoryResDto.StoryTabInfo> tabInfos = getStoryTabInfos(currentUser.getId(), hobbyId, lastRecordId);

        // 취미 상세 정보 조회 (검색 조건용)
        HobbyInfo hInfo = getTargetHobbyInfo(hobbyId);

        // 데이터 조회 및 페이징 처리
        List<GetActivityRecordByStoryResDto.RecordDto> recordDtos = activityRecordRepository.getActivityRecordByStory(
                hInfo.id(), lastRecordId, size + 1, keyword, currentUser.getId(), storyFilterType, hInfo.name()
        );

        boolean hasNext = recordDtos.size() > size;
        if (hasNext) recordDtos.remove(size.intValue());

        // 이미지 URL 변환
        convertImageUrls(recordDtos);

        Long lastId = recordDtos.isEmpty() ? null : recordDtos.get(recordDtos.size() - 1).getRecordId();

        return new GetActivityRecordByStoryResDto(tabInfos, lastId, recordDtos, hasNext);
    }

    private void saveRecentKeywordIfPresent(String userId, String keyword) {
        if (Strings.hasText(keyword)) {
            recentRedisService.createRecentKeyword(userId, keyword);
        }
    }

    private List<GetActivityRecordByStoryResDto.StoryTabInfo> getStoryTabInfos(String userId, Long hobbyId, Long lastRecordId) {
        if (lastRecordId != null) return null;

        return hobbyRepository.findAllByUserIdAndStatusOrderByIdDesc(userId, HobbyStatus.IN_PROGRESS)
                .stream()
                .map(h -> GetActivityRecordByStoryResDto.StoryTabInfo.from(h, h.getId().equals(hobbyId)))
                .toList();
    }

    private HobbyInfo getTargetHobbyInfo(Long hobbyId) {
        if (hobbyId == null) return new HobbyInfo(null, null);

        Hobby targetHobby = hobbyRepository.findById(hobbyId)
                .orElseThrow(() -> new CustomException(ErrorCode.HOBBY_NOT_FOUND));
        return new HobbyInfo(targetHobby.getHobbyInfoId(), targetHobby.getHobbyName());
    }

    private void convertImageUrls(List<GetActivityRecordByStoryResDto.RecordDto> dtos) {
        dtos.forEach(dto -> {
            if (dto.getThumbnailUrl() != null) {
                dto.setThumbnailUrl(s3Util.toFeedThumbResizedUrl(dto.getThumbnailUrl()));
            }
            if (dto.getUserInfo() != null && dto.getUserInfo().getProfileImageUrl() != null) {
                dto.getUserInfo().setProfileImageUrl(
                        s3Util.toProfileListResizedUrl(dto.getUserInfo().getProfileImageUrl())
                );
            }
        });
    }


    private record HobbyInfo(Long id, String name) {
    }

    private void validateRecordAuthority(RecordVisibility visibility, String writerId, String currentUserId) {
        if (writerId.equals(currentUserId)) return;

        switch (visibility) {
            case FRIEND -> {
                if (!checkFriendship(writerId, currentUserId)) throw new CustomException(ErrorCode.FRIEND_ONLY_ACCESS);
            }
            case PRIVATE -> throw new CustomException(ErrorCode.PRIVATE_RECORD);
            default -> {
            } // PUBLIC
        }
    }

    private boolean checkFriendship(String writerId, String currentUserId) {
        return friendRelationRepository.existsByFriendship(
                currentUserId, writerId, FriendRelationStatus.FOLLOW);
    }

    private void verifyRecordOwner(ActivityRecord record, User user) {
        if (!Objects.equals(record.getUser(), user)) {
            throw new CustomException(ErrorCode.NOT_ACTIVITY_RECORD_OWNER);
        }
    }

    private ActivityRecord getActivityRecord(Long id) {
        return activityRecordRepository.findById(id).orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));
    }

    private GetRecordDetailResDto.UserReactionDto createUserReactionDto(List<ReactionSummary> summaries, String userId) {
        List<RecordReactionType> myTypes = summaries.stream()
                .filter(s -> s.reactedUserId().equals(userId))
                .map(ReactionSummary::type).toList();
        return new GetRecordDetailResDto.UserReactionDto(
                myTypes.contains(RecordReactionType.AWESOME),
                myTypes.contains(RecordReactionType.GREAT),
                myTypes.contains(RecordReactionType.AMAZING),
                myTypes.contains(RecordReactionType.FIGHTING)
        );
    }

    private GetRecordDetailResDto.NewReactionDto createNewReactionDto(List<ReactionSummary> summaries, boolean isOwner) {
        if (!isOwner) return new GetRecordDetailResDto.NewReactionDto(false, false, false, false);
        List<RecordReactionType> unreadTypes = summaries.stream()
                .filter(s -> !s.readWriter())
                .map(ReactionSummary::type).toList();
        return new GetRecordDetailResDto.NewReactionDto(
                unreadTypes.contains(RecordReactionType.AWESOME),
                unreadTypes.contains(RecordReactionType.GREAT),
                unreadTypes.contains(RecordReactionType.AMAZING),
                unreadTypes.contains(RecordReactionType.FIGHTING)
        );
    }

    private GetRecordDetailResDto buildGetRecordDetailResDtoFromDto(RecordDetailQueryDto detail,
                                                                    boolean isOwner,
                                                                    GetRecordDetailResDto.NewReactionDto newR,
                                                                    GetRecordDetailResDto.UserReactionDto userR, boolean scraped) {
        return GetRecordDetailResDto.builder()
                .hobbyId(detail.hobbyId())
                .hobbyName(detail.hobbyName())
                .activityId(detail.activityId())
                .activityContent(detail.activityContent())
                .activityRecordId(detail.recordId())
                .imageUrl(detail.imageUrl())
                .sticker(detail.sticker())
                .createdAt(TimeUtil.formatLocalDateTime(detail.createdAt()))
                .memo(detail.memo())
                .recordOwner(isOwner)
                .scraped(scraped)
                .userInfo(GetRecordDetailResDto.UserInfoDto.builder()
                        .userId(detail.writerId())
                        .nickname(detail.writerNickname())
                        .profileImageUrl(s3Util.toProfileMainResizedUrl(detail.writerProfileImageUrl()))
                        .build())
                .visibility(detail.visibility())
                .newReaction(newR)
                .userReaction(userR)
                .build();
    }

    private void checkBlockedAndDeletedUser(String currentUserId, String targetId, boolean deleted) {
        // 한쪽이라도 차단 관계가 있는지 확인
        if (friendRelationRepository.existsByFriendship(currentUserId, targetId, FriendRelationStatus.BLOCK) || friendRelationRepository.existsByFriendship(targetId, currentUserId, FriendRelationStatus.BLOCK)) {
            throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);
        }

        // 타겟유저가 탈퇴한 회원인 경우
        if (deleted) throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);
    }

    private void checkBlockedAndDeletedUser(List<FriendRelation> relations, String me, String target, boolean deleted) {
        // 리스트에서 차단(BLOCK)이 하나라도 있는지 확인
        boolean isBlocked = relations.stream()
                .anyMatch(f -> f.getRelationStatus() == FriendRelationStatus.BLOCK);

        if (isBlocked || deleted) {
            throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);
        }
    }

    private void validateRecordAuthority(List<FriendRelation> relations, RecordVisibility visibility, String writerId, String me) {
        if (writerId.equals(me)) return;

        if (visibility == RecordVisibility.FRIEND) {
            boolean isFollowing = relations.stream()
                    .anyMatch(f -> f.getRequester().getId().equals(me) &&
                            f.getTargetUser().getId().equals(writerId) &&
                            f.getRelationStatus() == FriendRelationStatus.FOLLOW);

            if (!isFollowing) throw new CustomException(ErrorCode.FRIEND_ONLY_ACCESS);
        } else if (visibility == RecordVisibility.PRIVATE) {
            throw new CustomException(ErrorCode.PRIVATE_RECORD);
        }
    }

    private ActivityRecord getActivityRecord(Long recordId, User user) {
        return activityRecordRepository.findByIdAndUserId(recordId, user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));
    }

    private Activity getActivity(Long activityId, User user) {
        return activityRepository.findByIdAndUserId(activityId, user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));
    }

    private void handleImageUpdate(String oldImageUrl, String newImageUrl) {

        if (!isImageChanged(oldImageUrl, newImageUrl)) {
            return;
        }

        validateNewImageExists(newImageUrl);

        if (hasOldImage(oldImageUrl)) {
            registerImageDeletionAfterCommit(oldImageUrl);
        }
    }

    private boolean isImageChanged(String oldUrl, String newUrl) {
        return StringUtils.hasText(newUrl) && !newUrl.equals(oldUrl);
    }

    private void validateNewImageExists(String imageUrl) {
        String key = s3Service.extractKeyFromFileUrl(imageUrl);

        if (!s3Service.existsByKey(key)) {
            throw new CustomException(ErrorCode.S3_IMAGE_NOT_FOUND);
        }
    }

    private boolean hasOldImage(String oldImageUrl) {
        return StringUtils.hasText(oldImageUrl);
    }

    private void registerImageDeletionAfterCommit(String oldImageUrl) {

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteImageWithThumbnail(oldImageUrl);
            }
        });
    }

    private void deleteImageWithThumbnail(String imageUrl) {
        try {
            String originalKey = s3Service.extractKeyFromFileUrl(imageUrl);

            String thumbUrl = s3Util.toFeedThumbResizedUrl(imageUrl);
            String thumbKey = s3Service.extractKeyFromFileUrl(thumbUrl);

            s3Service.deleteByKey(originalKey);
            s3Service.deleteByKey(thumbKey);

            log.info("[S3-Cleanup] 기존 이미지 삭제 완료: {}", imageUrl);

        } catch (Exception e) {
            log.error("[S3-Cleanup] 삭제 실패: {}", imageUrl, e);
        }
    }

    private ReportActivityRecordDto getValidRecord(Long recordId) {
        ReportActivityRecordDto record = activityRecordRepository.getReportActivityRecord(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        if (record.isRecordDeleted()) {
            throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);
        }

        return record;
    }

    private void validateAccess(ReportActivityRecordDto record, User user) {
        String currentUserId = user.getId();

        List<FriendRelation> relations =
                friendRelationRepository.findAllRelationsBetween(currentUserId, record.getWriterId());

        checkBlockedAndDeletedUser(
                relations,
                currentUserId,
                record.getWriterId(),
                record.isWriterDeleted()
        );

        validateRecordAuthority(
                relations,
                record.getVisibility(),
                record.getWriterId(),
                currentUserId
        );
    }

    private void validateDuplicateReaction(Long recordId, String userId, RecordReactionType type) {

        boolean exists = recordReactionRepository
                .existsByRecordIdAndUserIdAndType(recordId, userId, type);

        if (exists) {
            throw new CustomException(ErrorCode.DUPLICATE_REACTION);
        }
    }

    private void saveReaction(Long recordId, String userId, RecordReactionType type) {

        ActivityRecordReaction reaction = ActivityRecordReaction.builder()
                .activityRecord(activityRecordRepository.getReferenceById(recordId))
                .reactedUser(userRepository.getReferenceById(userId))
                .reactionType(type)
                .readWriter(false)
                .build();

        recordReactionRepository.save(reaction);
    }

    private void updateRanking(Long recordId) {
        redisReactionService.incrementRankingScore(recordId);
    }

    private ActivityRecordWithUserDto getAccessibleRecordWithUser(Long recordId, User user) {

        ActivityRecordWithUserDto record =
                activityRecordRepository.getActivityRecordWithUser(recordId)
                        .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        String currentUserId = user.getId();

        List<FriendRelation> relations =
                friendRelationRepository.findAllRelationsBetween(currentUserId, record.getWriterId());

        checkBlockedAndDeletedUser(
                relations,
                currentUserId,
                record.getWriterId(),
                record.isWriterDeleted()
        );

        validateRecordAuthority(
                relations,
                record.getVisibility(),
                record.getWriterId(),
                currentUserId
        );

        return record;
    }

    private void validateDuplicateScrap(Long recordId, String userId) {
        if (activityRecordScrapRepository.existsByScrap(recordId, userId)) {
            throw new CustomException(ErrorCode.DUPLICATE_SCRAP);
        }
    }

    private Optional<ActivityRecordScrap> findScrap(Long recordId, String userId) {
        return activityRecordScrapRepository
                .findByActivityRecordIdAndUserId(recordId, userId);
    }

    private void saveScrap(Long recordId, User user) {

        ActivityRecord recordProxy =
                activityRecordRepository.getReferenceById(recordId);

        ActivityRecordScrap scrap = ActivityRecordScrap.builder()
                .activityRecord(recordProxy)
                .user(user)
                .build();

        activityRecordScrapRepository.save(scrap);
    }

    private void deleteScrap(ActivityRecordScrap scrap) {
        activityRecordScrapRepository.delete(scrap);
    }

    private void validateDuplicateReport(Long recordId, String userId) {

        boolean exists = activityRecordReportRepository
                .existsByReportedRecordIdAndReporterId(recordId, userId);

        if (exists) {
            throw new CustomException(ErrorCode.ALREADY_RECORD_REPORTED);
        }
    }

    private void saveReport(
            ReportActivityRecordDto record,
            User reporter,
            String reason
    ) {

        ActivityRecord recordProxy =
                activityRecordRepository.getReferenceById(record.getRecordId());

        User reportedUserProxy =
                userRepository.getReferenceById(record.getWriterId());

        ActivityRecordReport report = ActivityRecordReport.builder()
                .reporter(reporter)
                .reportedUser(reportedUserProxy)
                .reportedRecord(recordProxy)
                .reason(reason)
                .build();

        activityRecordReportRepository.save(report);
    }

    private ReportActivityRecordDto getAccessibleReportRecord(Long recordId, User user) {

        ReportActivityRecordDto record =
                activityRecordRepository.getReportActivityRecord(recordId)
                        .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        String currentUserId = user.getId();

        List<FriendRelation> relations =
                friendRelationRepository.findAllRelationsBetween(currentUserId, record.getWriterId());

        checkBlockedAndDeletedUser(
                relations,
                currentUserId,
                record.getWriterId(),
                record.isWriterDeleted()
        );

        validateRecordAuthority(
                relations,
                record.getVisibility(),
                record.getWriterId(),
                currentUserId
        );

        return record;
    }
}