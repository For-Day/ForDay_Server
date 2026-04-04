package com.example.ForDay.domain.record.service.v1;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.utils.ActivityUtil;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.notification.repository.NotificationRepository;
import com.example.ForDay.domain.notification.service.NotificationService;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionRepository;
import com.example.ForDay.domain.recent.service.RecentRedisService;
import com.example.ForDay.domain.record.dto.*;
import com.example.ForDay.domain.record.dto.request.ReportActivityRecordReqDto;
import com.example.ForDay.domain.record.dto.request.UpdateActivityRecordReqDto;
import com.example.ForDay.domain.record.dto.request.UpdateRecordVisibilityReqDto;
import com.example.ForDay.domain.record.dto.response.*;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.entity.ActivityRecordReport;
import com.example.ForDay.domain.record.entity.ActivityRecordScrap;
import com.example.ForDay.domain.record.repository.ActivityRecordReportRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordScrapRepository;
import com.example.ForDay.domain.record.service.RecordRedisService;
import com.example.ForDay.domain.record.service.StickerRedisService;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.record.type.StoryFilterType;
import com.example.ForDay.domain.record.utils.ActivityRecordUtil;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.global.ai.service.TodayRecordRedisService;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
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
    private final ActivityRecordReactionRepository recordReactionRepository;
    private final S3Service s3Service;
    private final ActivityRecordUtil activityRecordUtil;
    private final ActivityUtil activityUtil;
    private final ActivityRecordScrapRepository activityRecordScrapRepository;
    private final HobbyRepository hobbyRepository;
    private final RecentRedisService recentRedisService;
    private final S3Util s3Util;
    private final ActivityRecordReactionRepository reactionRepository;
    private final ActivityRecordReportRepository reportRepository;
    private final ActivityRecordScrapRepository scrapRepository;
    private final TodayRecordRedisService todayRecordRedisService;
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

    @Transactional(readOnly = true)
    public GetActivityRecordByStoryResDto getActivityRecordByStory(Long hobbyId, Long lastRecordId, Integer size, String keyword, CustomUserDetails user, StoryFilterType storyFilterType) {
        User currentUser = userUtil.getCurrentUser(user);
        log.info("[getActivityRecordByStory] 조회 시작 - User: {}, Filter: {}, Keyword: {}", currentUser.getId(), storyFilterType, keyword);

        saveRecentKeywordIfPresent(currentUser.getId(), keyword);
        List<GetActivityRecordByStoryResDto.StoryTabInfo> tabInfos = getStoryTabInfos(currentUser.getId(), hobbyId, lastRecordId);
        HobbyInfo hobbyInfo = getTargetHobbyInfo(hobbyId);

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
}