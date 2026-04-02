package com.example.ForDay.domain.record.service.v1;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.service.TodayRecordRedisService;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.notification.repository.NotificationRepository;
import com.example.ForDay.domain.notification.service.NotificationService;
import com.example.ForDay.domain.reaction.entity.ActivityRecordReactionCount;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionCountRepository;
import com.example.ForDay.domain.recent.service.RecentRedisService;
import com.example.ForDay.domain.record.dto.*;
import com.example.ForDay.domain.record.dto.request.ReportActivityRecordReqDto;
import com.example.ForDay.domain.record.dto.request.UpdateActivityRecordReqDto;
import com.example.ForDay.domain.record.dto.request.UpdateRecordVisibilityReqDto;
import com.example.ForDay.domain.record.dto.response.*;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.reaction.entity.ActivityRecordReaction;
import com.example.ForDay.domain.record.entity.ActivityRecordReport;
import com.example.ForDay.domain.record.entity.ActivityRecordScrap;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordReportRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordScrapRepository;
import com.example.ForDay.domain.record.service.RecordRedisService;
import com.example.ForDay.domain.record.service.StickerRedisService;
import com.example.ForDay.domain.record.service.RedisReactionService;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.record.type.StoryFilterType;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.domain.record.utils.ActivityRecordUtil;
import com.example.ForDay.domain.activity.utils.ActivityUtil;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.infra.s3.service.S3Service;
import com.example.ForDay.infra.s3.util.S3Util;
import io.jsonwebtoken.lang.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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
    private static final String REACTION_DONE_KEY_FORMAT = "reaction:done:%d:%s";

    private final ActivityRecordRepository activityRecordRepository;
    private final UserUtil userUtil;
    private final ActivityRecordReactionRepository recordReactionRepository;
    private final S3Service s3Service;
    private final ActivityRecordUtil activityRecordUtil;
    private final ActivityUtil activityUtil;
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
    private final ActivityRecordReactionCountRepository recordReactionCountRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final StickerRedisService stickerRedisService;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final RecordRedisService recordRedisService;

    // 이제 사용 x
    @Transactional(readOnly = true)
    public GetRecordDetailResDto getRecordDetail(Long recordId, CustomUserDetails user, Long notificationId) {
        RecordDetailQueryDto detail = activityRecordRepository.findDetailDtoById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        if (detail.recordDeleted()) throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);

        String currentUserId = userUtil.getCurrentUser(user).getId();
        boolean isRecordOwner = activityRecordUtil.isRecordOwner(currentUserId, detail.writerId());
        log.info("[getRecordDetail] 권한 확인 - WriterId: {}, IsOwner: {}", detail.writerId(), isRecordOwner);

        if (!isRecordOwner) {
            log.debug("[getRecordDetail] 비소유자 접근 - 공개 범위 검증 시작 (Visibility: {})", detail.visibility());
            activityRecordUtil.validateAccess(currentUserId, detail.writerId(), detail.writerDeleted(), detail.visibility());
        }

        List<ReactionSummary> summaries = recordReactionRepository.findReactionSummariesByRecordId(recordId);

        boolean scraped = activityRecordScrapRepository.existsByScrap(detail.recordId(), currentUserId);

        log.info("[getRecordDetail] 조회 성공 - RecordId: {}, Writer: {}, Reactions: {}, Scraped: {}",
                recordId, detail.writerId(), summaries.size(), scraped);

        String profileImageUrl = s3Util.toProfileMainResizedUrl(detail.writerProfileImageUrl());

        if(isRecordOwner) notificationService.markAsReadIfUnread(notificationId);

        return GetRecordDetailResDto.of(detail, isRecordOwner, scraped, GetRecordDetailResDto.NewReactionDto.of(summaries, isRecordOwner), GetRecordDetailResDto.UserReactionDto.of(summaries, currentUserId), profileImageUrl);
    }

    @Transactional
    public GetRecordReactionUsersResDto getRecordReactionUsers(Long recordId, RecordReactionType type, CustomUserDetails user, String lastUserId, Integer size) {
        // 엔티티 전체 대신 권한 확인용 DTO만 조회 (Fetch Join 제거 효과)
        RecordDetailQueryDto recordDetail = activityRecordRepository.findDetailDtoById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        if (recordDetail.recordDeleted()) {
            throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);
        }

        String currentUserId = userUtil.getCurrentUser(user).getId();
        activityRecordUtil.validateAccess(currentUserId, recordDetail.writerId(), recordDetail.writerDeleted(), recordDetail.visibility());

        boolean isRecordOwner = activityRecordUtil.isRecordOwner(currentUserId, recordDetail.writerId());
        // 리포지토리에서 DTO(ReactionUserInfo)로 직접 조회하여 N+1 및 오버페칭 방지
        List<GetRecordReactionUsersResDto.ReactionUserInfo> reactionUsers = recordReactionRepository.findReactionUsersDtoByType(recordId, type, lastUserId, size, isRecordOwner);


        // 게시글 주인인 경우에만 벌크 업데이트 실행
        if (isRecordOwner) {
            recordReactionRepository.markAsReadByRecordIdAndType(recordId, type);
        }

        return GetRecordReactionUsersResDto.of(type, reactionUsers, size);
    }

    @Transactional
    public ReactToRecordResDto reactToRecord(Long recordId, RecordReactionType type, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        ReportActivityRecordDto record = activityRecordUtil.getValidRecord(recordId);
        if(!isRecordOwner(currentUser, record)) activityRecordUtil.validateAccess(currentUser.getId(), record.getWriterId(), record.isWriterDeleted(), record.getVisibility());
        validateDuplicateReaction(recordId, currentUser.getId(), type);

        ActivityRecordReaction reaction = ActivityRecordReaction.of(activityRecordRepository.getReferenceById(recordId), userRepository.getReferenceById(currentUser.getId()), type);
        recordReactionRepository.save(reaction);

        // 반응 수 증가
        int result = recordReactionCountRepository.increaseCount(recordId, type.toString());
        if (result == 0) {
            recordReactionCountRepository.save(ActivityRecordReactionCount.init(recordId, type));
        }
        // 리액션 증가에 따른 랭킹 점수 업데이트
        redisReactionService.incrementRankingScore(record.getRecordId());

        if(!isRecordOwner(currentUser, record)) {
            notificationService.processReactionNotification(currentUser, userRepository.getReferenceById(record.getWriterId()), type, record.getRecordId(), record.getImageUrl());
        }

        return ReactToRecordResDto.of(type, recordId);
    }

    @Transactional
    public UpdateRecordVisibilityResDto updateRecordVisibility(Long recordId, UpdateRecordVisibilityReqDto reqDto, CustomUserDetails user) {
        ActivityRecord activityRecord = activityRecordUtil.getRecord(recordId);
        verifyRecordOwner(activityRecord, userUtil.getCurrentUser(user));

        RecordVisibility previous = activityRecord.getVisibility();
        RecordVisibility next = reqDto.getVisibility();

        if (previous == next) {
            return UpdateRecordVisibilityResDto.alreadyVisibility(previous, next);
        }

        activityRecord.updateVisibility(next);
        return UpdateRecordVisibilityResDto.updateVisibility(previous, next);
    }

    @Transactional
    public CancelReactToRecordResDto cancelReactToRecord(Long recordId, RecordReactionType type, CustomUserDetails user) {
        String userId = user.getUserId();

        int deletedCount = recordReactionRepository.deleteByRecordIdAndUserIdAndType(recordId, userId, type);
        if (deletedCount == 0) {
            throw new CustomException(ErrorCode.REACTION_NOT_FOUND);
        }

        recordReactionCountRepository.decreaseCount(recordId, type.name());

        String key = REACTION_DONE_KEY_FORMAT.formatted(recordId, userId);
        redisTemplate.opsForSet().remove(key, type.name());

        // Redis 점수 차감
        redisReactionService.decrementRankingScore(recordId);

        log.info("[cancelReactToRecord] 리액션 취소 완료 - RecordId: {}, UserId: {}", recordId, userId);
        return CancelReactToRecordResDto.of(type, recordId);
    }


    @Transactional
    public UpdateActivityRecordResDto updateActivityRecord(Long recordId, UpdateActivityRecordReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        ActivityRecord record = activityRecordUtil.getRecordByUserId(recordId, currentUser);
        Activity activity = activityUtil.getActivityByUserId(reqDto.getActivityId(), currentUser.getId());

        handleImageUpdate(record.getImageUrl(), reqDto.getImageUrl(), record.getId());
        record.updateRecord(activity, reqDto);

        stickerRedisService.evictRecordCache(record.getHobby().getId(), currentUser.getId());
        recordRedisService.evictRecordCache(record.getId());
        return UpdateActivityRecordResDto.of(activity, record);
    }

    // 리팩토링 필요
    @Transactional
    public DeleteActivityRecordResDto deleteActivityRecord(Long recordId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        ActivityRecord record = activityRecordUtil.getRecordByUserId(recordId, currentUser);
        // 이미 삭제된 경우 예외 처리
        if (record.isDeleted()) {
            throw new CustomException(ErrorCode.ALREADY_DELETED_RECORD);
        }

        reactionRepository.deleteByActivityRecord(record);
        reportRepository.deleteByReportedRecord(record);
        scrapRepository.deleteByActivityRecord(record);

        String deleteImageUrl = record.getImageUrl();

        if (isToday(record)) {
            record.getActivity().deleteRecord();
            record.getHobby().deleteRecord();
            todayRecordRedisService.deleteTodayRecordKey(currentUser.getId(), record.getHobby().getId());
            activityRecordRepository.delete(record);
            notificationRepository.updateImageUrlByRecordId(record.getId(), null);
        } else {
            record.deleteRecord();
        }

        registerDeleteImageAfterCommit(deleteImageUrl);
        stickerRedisService.evictRecordCache(record.getHobby().getId(), currentUser.getId());
        recordRedisService.evictRecordCache(record.getId());
        return DeleteActivityRecordResDto.of(record.getId(), deleteImageUrl);
    }

    @Transactional
    public AddActivityRecordScrapResDto addActivityRecordScrap(Long recordId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        getAccessibleRecordWithUser(recordId, currentUser);
        validateDuplicateScrap(recordId, currentUser.getId());

        ActivityRecordScrap scrap = ActivityRecordScrap.of(activityRecordRepository.getReferenceById(recordId), currentUser);
        activityRecordScrapRepository.save(scrap);

        return AddActivityRecordScrapResDto.from(recordId);
    }

    @Transactional
    public DeleteActivityRecordScrapResDto deleteActivityRecordScrap(Long recordId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        Optional<ActivityRecordScrap> scrap = activityRecordScrapRepository.findByActivityRecordIdAndUserId(recordId, currentUser.getId());

        if (scrap.isEmpty()) {
            return DeleteActivityRecordScrapResDto.notExistScrap(recordId);
        }
        activityRecordScrapRepository.delete(scrap.get());

        return DeleteActivityRecordScrapResDto.deleteScrap(recordId);
    }

    @Transactional
    public ReportActivityRecordResDto reportActivityRecord(Long recordId, ReportActivityRecordReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        log.info("[reportActivityRecord] 신고 요청 - recordId={}, reporter={}", recordId, currentUser.getId());

        // 기록 조회 + 접근 권한 검증
        ReportActivityRecordDto record = getAccessibleReportRecord(recordId, currentUser);

        // 중복 신고 방지
        validateDuplicateReport(record.getRecordId(), currentUser.getId());

        // 신고 저장
        saveReport(record, currentUser, reqDto.getReason());

        log.info("[reportActivityRecord] 신고 완료 - recordId={}", recordId);

        return ReportActivityRecordResDto.from(record);
    }


    @Transactional(readOnly = true)
    public GetActivityRecordByStoryResDto getActivityRecordByStory(Long hobbyId, Long lastRecordId, Integer size, String keyword, CustomUserDetails user, StoryFilterType storyFilterType) {
        User currentUser = userUtil.getCurrentUser(user);
        log.info("[getActivityRecordByStory] 조회 시작 - User: {}, Filter: {}, Keyword: {}", currentUser.getId(), storyFilterType, keyword);
        // 검색어 저장
        saveRecentKeywordIfPresent(currentUser.getId(), keyword);

        // 상단 탭 정보 조회 (첫 페이지 조회 시에만)
        List<GetActivityRecordByStoryResDto.StoryTabInfo> tabInfos = getStoryTabInfos(currentUser.getId(), hobbyId, lastRecordId);

        // 취미 상세 정보 조회 (검색 조건용)
        HobbyInfo hobbyInfo = getTargetHobbyInfo(hobbyId);

        // 데이터 조회 및 페이징 처리
        List<GetActivityRecordByStoryResDto.RecordDto> recordDtos = activityRecordRepository.getActivityRecordByStory(
                hobbyInfo.id(), lastRecordId, size + 1, keyword, currentUser.getId(), storyFilterType, hobbyInfo.name()
        );

        recordDtos.forEach(dto -> dto.convertImageUrls(s3Util));

        return GetActivityRecordByStoryResDto.of(notificationService.unreadNotificationExists(currentUser), tabInfos, recordDtos, size);
    }

    private static boolean isToday(ActivityRecord activityRecord) {
        return activityRecord.getCreatedAt().toLocalDate().equals(LocalDate.now());
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

    private void verifyRecordOwner(ActivityRecord record, User user) {
        if (!Objects.equals(record.getUser(), user)) {
            throw new CustomException(ErrorCode.NOT_ACTIVITY_RECORD_OWNER);
        }
    }

    private void handleImageUpdate(String oldImageUrl, String newImageUrl, Long recordId) {
        if (!isImageChanged(oldImageUrl, newImageUrl)) {
            return;
        }

        s3Util.validateS3Image(newImageUrl);

        if (hasOldImage(oldImageUrl)) {
            registerImageDeletionAfterCommit(oldImageUrl);
        }

        notificationRepository.updateImageUrlByRecordId(recordId, newImageUrl);
    }

    private boolean isImageChanged(String oldUrl, String newUrl) {
        return StringUtils.hasText(newUrl) && !newUrl.equals(oldUrl);
    }

    private boolean hasOldImage(String oldImageUrl) {
        return StringUtils.hasText(oldImageUrl);
    }

    private void registerImageDeletionAfterCommit(String oldImageUrl) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    String originalKey = s3Service.extractKeyFromFileUrl(oldImageUrl);

                    String thumbUrl = s3Util.toFeedThumbResizedUrl(oldImageUrl);
                    String thumbKey = s3Service.extractKeyFromFileUrl(thumbUrl);

                    s3Service.deleteByKey(originalKey);
                    s3Service.deleteByKey(thumbKey);

                    log.info("[S3-Cleanup] 기존 이미지 삭제 완료: {}", oldImageUrl);

                } catch (Exception e) {
                    log.error("[S3-Cleanup] 삭제 실패: {}", oldImageUrl, e);
                }
            }
        });
    }

    private void validateDuplicateReaction(Long recordId, String userId, RecordReactionType type) {
        if (recordReactionRepository.existsByRecordIdAndUserIdAndType(recordId, userId, type)) {
            throw new CustomException(ErrorCode.DUPLICATE_REACTION);
        }
    }

    private ActivityRecordWithUserDto getAccessibleRecordWithUser(Long recordId, User user) {
        ActivityRecordWithUserDto record = activityRecordRepository.getActivityRecordWithUser(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        activityRecordUtil.validateAccess(user.getId(), record.getWriterId(), record.isWriterDeleted(), record.getVisibility());

        return record;
    }

    private void validateDuplicateScrap(Long recordId, String userId) {
        if (activityRecordScrapRepository.existsByScrap(recordId, userId)) {
            throw new CustomException(ErrorCode.DUPLICATE_SCRAP);
        }
    }

    private void validateDuplicateReport(Long recordId, String userId) {
        if (activityRecordReportRepository.existsByReportedRecordIdAndReporterId(recordId, userId)) {
            throw new CustomException(ErrorCode.ALREADY_RECORD_REPORTED);
        }
    }

    private void saveReport(ReportActivityRecordDto record, User reporter, String reason) {
        ActivityRecord recordProxy = activityRecordRepository.getReferenceById(record.getRecordId());
        User reportedUserProxy = userRepository.getReferenceById(record.getWriterId());
        ActivityRecordReport report = ActivityRecordReport.of(reporter, reportedUserProxy, recordProxy, reason);
        activityRecordReportRepository.save(report);
    }

    private ReportActivityRecordDto getAccessibleReportRecord(Long recordId, User user) {
        ReportActivityRecordDto record = activityRecordRepository.getReportActivityRecord(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        activityRecordUtil.validateAccess(user.getId(), record.getWriterId(), record.isWriterDeleted(), record.getVisibility());
        return record;
    }


    private void registerDeleteImageAfterCommit(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    String imageKey = s3Service.extractKeyFromFileUrl(imageUrl);
                    String feedThumbResizedUrl = s3Util.toFeedThumbResizedUrl(imageUrl);
                    String feedThumbResizedKey = s3Service.extractKeyFromFileUrl(feedThumbResizedUrl);

                    s3Service.deleteByKey(imageKey);
                    s3Service.deleteByKey(feedThumbResizedKey);

                } catch (Exception e) {
                    log.error("S3 파일 삭제 실패 (DB는 정상 삭제됨): {}", imageUrl, e);
                }
            }
        });
    }

    private static boolean isRecordOwner(User currentUser, ReportActivityRecordDto record) {
        return currentUser.getId().equals(record.getWriterId());
    }
}