package com.example.ForDay.domain.activity.service;

import com.example.ForDay.domain.activity.dto.ActivityRecordCollectInfo;
import com.example.ForDay.domain.activity.dto.request.FastAPIHobbyCardReqDto;
import com.example.ForDay.domain.activity.dto.request.UpdateActivityReqDto;
import com.example.ForDay.domain.activity.dto.response.FastAPIHobbyCardResDto;
import com.example.ForDay.domain.activity.dto.response.GetAiRecommendItemsResDto;
import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.entity.ActivityRecommendItem;
import com.example.ForDay.domain.activity.repository.ActivityRecommendItemRepository;
import com.example.ForDay.domain.activity.type.AIItemType;
import com.example.ForDay.domain.friend.repository.FriendRelationRepository;
import com.example.ForDay.domain.friend.type.FriendRelationStatus;
import com.example.ForDay.domain.hobby.dto.response.CollectActivityResDto;
import com.example.ForDay.domain.hobby.entity.HobbyCard;
import com.example.ForDay.domain.hobby.repository.HobbyCardRepository;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.service.HobbyCardService;
import com.example.ForDay.domain.hobby.service.UserSummaryAIService;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.hobby.dto.request.RecordActivityReqDto;
import com.example.ForDay.domain.hobby.dto.response.RecordActivityResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.ai.service.AIService;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.common.response.dto.MessageResDto;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.ActivityUtil;
import com.example.ForDay.global.util.HobbyUtil;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.infra.s3.service.S3Service;
import com.example.ForDay.infra.s3.util.S3Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {
    private static final Integer STICKER_COMPLETE_COUNT = 66;
    private final UserUtil userUtil;
    private final ActivityRepository activityRepository;
    private final S3Service s3Service;
    private final ActivityRecordRepository activityRecordRepository;
    private final TodayRecordRedisService todayRecordRedisService;
    private final FriendRelationRepository friendRelationRepository;
    private final ActivityRecommendItemRepository recommendItemRepository;
    private final UserSummaryAIService userSummaryAIService;
    private final HobbyUtil hobbyUtil;
    private final HobbyCardService hobbyCardService;
    private final ActivityUtil activityUtil;
    private final S3Util s3Util;

    @Transactional
    public RecordActivityResDto recordActivity(Long activityId, RecordActivityReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        log.info("[RecordActivity] 시작 - UserId: {}, ActivityId: {}", currentUser.getId(), activityId);

        Activity activity = activityRepository.findByIdAndUserIdWithHobby(activityId, currentUser.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));
        Hobby hobby = activity.getHobby();

        hobby.validateCanRecord(); // 취미 진행 상태 및 스티커 완료 여부 확인
        todayRecordRedisService.validateNotRecordedToday(currentUser.getId(), hobby.getId()); // 오늘 기록 여부 확인
        s3Util.validateS3Image(reqDto.getImageUrl()); // S3 이미지 존재 여부 확인

        ActivityRecord activityRecord = ActivityRecord.of(hobby, activity, currentUser, reqDto);
        activity.record(); // 해당 취미와 활동에 대해 스티커 + 1
        currentUser.obtainSticker(); // 해당 유저가 모은 스티커 + 1
        activityRecordRepository.save(activityRecord);

        log.info("[RecordActivity] 기록 저장 성공 - RecordId: {}, 현재 스티커 수: {}",activityRecord.getId(), hobby.getCurrentStickerNum());

        // 취미 카드 생성 로직 (목표일 여부와 관계없이 취미를 66개 모으면 취미 카드 생성)
        if (hobby.isStickerFull()) {
            log.info("[RecordActivity] 취미 완주 달성! 취미 카드 생성을 시작합니다. HobbyId: {}", hobby.getId());
            createHobbyCard(hobby, currentUser);
        }
        todayRecordRedisService.markAsRecorded(currentUser.getId(), hobby.getId()); // 오늘 기록 여부 표시

        return RecordActivityResDto.of( hobby, activityRecord, activity, reqDto.getSticker(), isCheckStickerFull(hobby));
    }

    @Transactional
    public RecordActivityResDto testRecordActivity(
            Long activityId,
            RecordActivityReqDto reqDto,
            CustomUserDetails user
    ) {
        User currentUser = userUtil.getCurrentUser(user);

        Activity activity = activityRepository.findByIdAndUserIdWithHobby(activityId, currentUser.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));
        Hobby hobby = activity.getHobby();

        if (isCheckStickerFull(hobby)) throw new CustomException(ErrorCode.STICKER_COMPLETION_REACHED);
        hobby.validateInProgress();
        s3Util.validateS3Image(reqDto.getImageUrl()); // S3 이미지 존재 여부 확인

        ActivityRecord activityRecord = ActivityRecord.of(hobby, activity, currentUser, reqDto);

        activity.record();
        currentUser.obtainSticker();
        activityRecordRepository.save(activityRecord);

        // 취미 카드 생성 로직 (목표일 여부와 관계없이 취미를 66개 모으면 취미 카드 생성)
        if (Objects.equals(hobby.getCurrentStickerNum(), STICKER_COMPLETE_COUNT)) {
            createHobbyCard(hobby, currentUser);
        }

        return RecordActivityResDto.of( hobby, activityRecord, activity, reqDto.getSticker(), isCheckStickerFull(hobby));
    }

    @Transactional
    public MessageResDto updateActivity(Long activityId, UpdateActivityReqDto reqDto, CustomUserDetails user) {
        log.info("[ActivityService] 활동 수정 요청 - activityId={}, content={}", activityId, reqDto.getContent());
        User currentUser = userUtil.getCurrentUser(user);
        Activity activity = activityUtil.getActivityByUserId(activityId, currentUser.getId());

        // 진행 중인 취미가 아니면 활동 수정 불가
        activity.getHobby().validateInProgress();

        String beforeContent = activity.getContent();
        activity.updateContent(reqDto.getContent());

        log.info("[ActivityService] 활동 수정 완료 - activityId={}, userId={}, before='{}', after='{}'",activityId, currentUser.getId(), beforeContent, reqDto.getContent());
        return new MessageResDto("활동이 정상적으로 수정되었습니다.");
    }

    @Transactional
    public MessageResDto deleteActivity(Long activityId, CustomUserDetails user) {
        log.info("[ActivityService] 활동 삭제 요청 - activityId={}", activityId);
        User currentUser = userUtil.getCurrentUser(user);
        Activity activity = activityUtil.getActivityByUserId(activityId, currentUser.getId());

        // 삭제 가능 여부 (해당 활동으로 획득한 스티커가 없을 때)
        activity.validateDeletable();

        // 진행 중인 취미가 아니면 활동 삭제 불가
        activity.getHobby().validateInProgress();
        activityRepository.delete(activity);

        log.info("[ActivityService] 활동 삭제 완료 - activityId={}, userId={}",activityId, currentUser.getId());
        return new MessageResDto("활동이 삭제되었어요.");
    }

    @Transactional
    public CollectActivityResDto collectActivity(Long hobbyId, Long activityId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        log.info("[Activity Collect] 시작 - 사용자: {}, 취미ID: {}, 활동ID: {}", currentUser.getId(), hobbyId, activityId);

        Hobby hobby = hobbyUtil.getHobbyByUserId(hobbyId, currentUser);

        ActivityRecordCollectInfo originActivity = activityRepository.getCollectActivityInfo(activityId)
                .orElseThrow(() -> {
                    log.warn("[Activity Collect] 실패 - 원본 활동을 찾을 수 없음. 활동ID: {}", activityId);
                    return new CustomException(ErrorCode.ACTIVITY_NOT_FOUND);
                });

        hobby.validateInProgress();
        validateTargetUser(currentUser.getId(), originActivity);
        log.info("[Activity Collect] 검증 완료 - 활동 소유자ID: {}", originActivity.getUserId());

        Activity newActivity = activityRepository.save(Activity.from(currentUser, hobby, originActivity));

        log.info("[Activity Collect] 완료 - 생성된 활동ID: {}, 저장된 취미: {}", newActivity.getId(), hobby.getHobbyName());
        return CollectActivityResDto.of(hobby, newActivity);
    }

    @Transactional(readOnly = true)
    public GetAiRecommendItemsResDto getAiRecommendItems(Long hobbyId, CustomUserDetails user, AIItemType type) {
        User currentUser = userUtil.getCurrentUser(user);
        log.info("[AI Recommend] 아이템 조회 시작 - HobbyId: {}, Type: {}", hobbyId, type);

        // 취미 조회 및 검증
        Hobby hobby = hobbyUtil.getHobbyByUserId(hobbyId, currentUser);

        // 오늘 생성된 추천 아이템 조회
        List<ActivityRecommendItem> items = recommendItemRepository.findAllByHobbyIdAndDate(
                hobby.getId(),
                LocalDate.now().atStartOfDay(),
                LocalDate.now().atTime(LocalTime.MAX),
                type
        );

        if (items.isEmpty()) {
            return new GetAiRecommendItemsResDto();
        }

        // 사용자 요약 문구 생성
        String userSummaryText = determineUserSummary(currentUser, hobby);

        // DTO 변환 및 반환
        return GetAiRecommendItemsResDto.of(hobby, items, userSummaryText);
    }

    private String determineUserSummary(User user, Hobby hobby) {
        String summary = userSummaryAIService.determine(user, hobby);

        if (summary.isEmpty()) {
            return "이전에 추천 받은 활동들이에요.";
        }

        return summary + " 이전에 추천 받은 활동들이에요.";
    }

    private void createHobbyCard(Hobby hobby, User currentUser) {
        log.info("[HobbyCard] 생성 프로세스 시작 - 사용자: {}, 취미: {}", currentUser.getId(), hobby.getId());

        try {
            hobbyCardService.createHobbyCard(currentUser, hobby);
            currentUser.obtainHobbyCard();

        } catch (Exception e) {
            todayRecordRedisService.deleteTodayRecordKey(currentUser.getId(), hobby.getId());
            log.error("[AI-HOBBY-CARD][ERROR] FastAPI 호출 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    private static boolean isCheckStickerFull(Hobby hobby) {
        if (hobby.getCurrentStickerNum() == null || hobby.getGoalDays() == null) {
            return false;
        }
        return Objects.equals(hobby.getCurrentStickerNum().intValue(), STICKER_COMPLETE_COUNT)
                && Objects.equals(hobby.getGoalDays().intValue(), STICKER_COMPLETE_COUNT);
    }

    private void validateTargetUser(String currentUserId, ActivityRecordCollectInfo target) {
        // 탈퇴한 회원인지 먼저 확인
        if (target.isUserDeleted()) {
            throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);
        }

        // 차단 관계 확인 (양방향)
        boolean isBlocked = friendRelationRepository.existsByFriendship(currentUserId, target.getUserId(), FriendRelationStatus.BLOCK)
                || friendRelationRepository.existsByFriendship(target.getUserId(), currentUserId, FriendRelationStatus.BLOCK);

        if (isBlocked) {
            throw new CustomException(ErrorCode.ACTIVITY_NOT_FOUND);
        }
    }
}
