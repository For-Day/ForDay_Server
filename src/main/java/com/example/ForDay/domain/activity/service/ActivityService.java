package com.example.ForDay.domain.activity.service;

import com.example.ForDay.domain.activity.dto.request.UpdateActivityReqDto;
import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.hobby.entity.HobbyCard;
import com.example.ForDay.domain.hobby.repository.HobbyCardRepository;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.hobby.dto.request.RecordActivityReqDto;
import com.example.ForDay.domain.hobby.dto.response.RecordActivityResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.common.response.dto.MessageResDto;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.infra.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final HobbyCardRepository hobbyCardRepository;

    @Transactional
    public RecordActivityResDto recordActivity(
            Long activityId,
            RecordActivityReqDto reqDto,
            CustomUserDetails user
    ) {

        User currentUser = userUtil.getCurrentUser(user);
        log.info("[RecordActivity] 시작 - UserId: {}, ActivityId: {}", currentUser.getId(), activityId);

        Activity activity = getActivityByUserId(activityId, currentUser.getId());
        Hobby hobby = activity.getHobby();

        checkHobbyInProgressStatus(hobby); // 진행 중인 취미에 대해서만 활동 기록 가능

        // 기간 설정 66일 이고 이미 스티커를 다 채운 상황이면 기록 불가
        if (isCheckStickerFull(hobby)) throw new CustomException(ErrorCode.STICKER_COMPLETION_REACHED);

        String redisKey = todayRecordRedisService.createRecordKey(currentUser.getId(), hobby.getId());
        if (todayRecordRedisService.hasKey(redisKey)) { // 해당 취미에 대해 오늘 기록한 활동이 있는지 확인
            log.warn("[RecordActivity] 중복 기록 시도 - UserId: {}, HobbyId: {}",
                    currentUser.getId(), hobby.getId());
            throw new CustomException(ErrorCode.ALREADY_RECORDED_TODAY);
        }

        if (StringUtils.hasText(reqDto.getImageUrl())) {  // 이미지를 등록하고자 한다면 해당 이미지가 s3상에 잘 업로드 되었는지 확인
            String s3Key = s3Service.extractKeyFromFileUrl(reqDto.getImageUrl()); // 이미지url에서 key를 추출
            if (!s3Service.existsByKey(s3Key)) { // 해당 key를 가진 객체가 존재하는지 확인
                log.error("[RecordActivity] S3 이미지 부재 - Key: {}", s3Key);
                throw new CustomException(ErrorCode.S3_IMAGE_NOT_FOUND); // 존재하지 않으면 예외 발생
            }
        }

        ActivityRecord activityRecord = ActivityRecord.builder()
                .hobby(hobby)
                .activity(activity)
                .user(currentUser)
                .sticker(reqDto.getSticker())
                .memo(reqDto.getMemo())
                .visibility(reqDto.getVisibility())
                .imageUrl(reqDto.getImageUrl())
                .build();

        activity.record(); // 해당 취미와 활동에 대해 스티커 + 1
        currentUser.obtainSticker(); // 해당 유저가 모은 스티커 + 1
        activityRecordRepository.save(activityRecord);

        todayRecordRedisService.setDataExpire(redisKey, "recorded");

        // 취미 카드 생성 로직 (목표일 여부와 관계없이 취미를 66개 모으면 취미 카드 생성)
        if (Objects.equals(hobby.getCurrentStickerNum(), STICKER_COMPLETE_COUNT)) {
            createHobbyCard(hobby, currentUser);
        }

        boolean extensionCheckRequired = isCheckStickerFull(hobby);

        return new RecordActivityResDto(
                "오늘의 활동 기록이 정상적으로 작성되었습니다",
                hobby.getId(),
                activityRecord.getId(),
                activity.getContent(),
                activityRecord.getImageUrl(),
                reqDto.getSticker(),
                activityRecord.getMemo(),
                extensionCheckRequired
        );
    }

    @Transactional
    public RecordActivityResDto testRecordActivity(
            Long activityId,
            RecordActivityReqDto reqDto,
            CustomUserDetails user
    ) {
        User currentUser = userUtil.getCurrentUser(user);

        Activity activity = getActivityByUserId(activityId, currentUser.getId());
        Hobby hobby = activity.getHobby();

        if (isCheckStickerFull(hobby)) throw new CustomException(ErrorCode.STICKER_COMPLETION_REACHED);
        checkHobbyInProgressStatus(hobby); // 진행 중인 취미에 대해서만 활동 기록 가능

        if (StringUtils.hasText(reqDto.getImageUrl())) {  // 이미지를 등록하고자 한다면 해당 이미지가 s3상에 잘 업로드 되었는지 확인
            String s3Key = s3Service.extractKeyFromFileUrl(reqDto.getImageUrl()); // 이미지url에서 key를 추출
            if (!s3Service.existsByKey(s3Key)) { // 해당 key를 가진 객체가 존재하는지 확인
                throw new CustomException(ErrorCode.S3_IMAGE_NOT_FOUND); // 존재하지 않으면 예외 발생
            }
        }

        ActivityRecord activityRecord = ActivityRecord.builder()
                .hobby(hobby)
                .activity(activity)
                .user(currentUser)
                .sticker(reqDto.getSticker())
                .memo(reqDto.getMemo())
                .visibility(reqDto.getVisibility())
                .imageUrl(reqDto.getImageUrl())
                .build();

        activity.record();
        currentUser.obtainSticker();
        activityRecordRepository.save(activityRecord);


        // 취미 카드 생성 로직 (목표일 여부와 관계없이 취미를 66개 모으면 취미 카드 생성)
        if (Objects.equals(hobby.getCurrentStickerNum(), STICKER_COMPLETE_COUNT)) {
            createHobbyCard(hobby, currentUser);
        }
        boolean extensionCheckRequired = isCheckStickerFull(hobby);

        return new RecordActivityResDto(
                "오늘의 활동 기록이 정상적으로 작성되었습니다",
                hobby.getId(),
                activityRecord.getId(),
                activity.getContent(),
                activityRecord.getImageUrl(),
                reqDto.getSticker(),
                activityRecord.getMemo(),
                extensionCheckRequired
        );
    }

    private void createHobbyCard(Hobby hobby, User currentUser) {
        // fast api 서버와 통신하여 취미 카드 content 생성하기
        String hobbyCardContent = "취미 카드 내용";

        HobbyCard hobbyCard = HobbyCard.builder()
                .user(currentUser)
                .hobby(hobby)
                .content(hobbyCardContent)
                .imageUrl(hobby.getCoverImageUrl())
                .build();
        hobbyCardRepository.save(hobbyCard);

        currentUser.obtainHobbyCard();
    }

    private static boolean isCheckStickerFull(Hobby hobby) {
        return Objects.equals(hobby.getCurrentStickerNum(), STICKER_COMPLETE_COUNT) && Objects.equals(hobby.getGoalDays(), STICKER_COMPLETE_COUNT);
    }

    @Transactional
    public MessageResDto updateActivity(
            Long activityId,
            UpdateActivityReqDto reqDto,
            CustomUserDetails user
    ) {
        log.info("[ActivityService] 활동 수정 요청 - activityId={}, content={}",
                activityId, reqDto.getContent());
        User currentUser = userUtil.getCurrentUser(user);
        Activity activity = getActivityByUserId(activityId, currentUser.getId());

        // 진행 중인 취미가 아니면 활동 수정 불가
        checkHobbyInProgressStatus(activity.getHobby());

        String beforeContent = activity.getContent();
        activity.updateContent(reqDto.getContent());

        log.info("[ActivityService] 활동 수정 완료 - activityId={}, userId={}, before='{}', after='{}'",
                activityId,
                currentUser.getId(),
                beforeContent,
                reqDto.getContent()
        );

        return new MessageResDto("활동이 정상적으로 수정되었습니다.");
    }

    @Transactional
    public MessageResDto deleteActivity(Long activityId, CustomUserDetails user) {
        log.info("[ActivityService] 활동 삭제 요청 - activityId={}", activityId);
        User currentUser = userUtil.getCurrentUser(user);
        Activity activity = getActivityByUserId(activityId, currentUser.getId());

        // 삭제 가능 여부 (해당 활동으로 획득한 스티커가 없을 때)
        if (!activity.isDeletable()) {
            log.warn("[ActivityService] 활동 삭제 불가 (deletable=false) - activityId={}, userId={}",
                    activityId, currentUser.getId());
            throw new CustomException(ErrorCode.ACTIVITY_NOT_DELETABLE);
        }

        // 진행 중인 취미가 아니면 활동 삭제 불가
        checkHobbyInProgressStatus(activity.getHobby());
        activityRepository.delete(activity);

        log.info("[ActivityService] 활동 삭제 완료 - activityId={}, userId={}",
                activityId, currentUser.getId()
        );

        return new MessageResDto("활동이 정상적으로 삭제되었습니다.");
    }

    // 유틸 클래스
    private Activity getActivityByUserId(Long activityId, String userId) {
        return activityRepository.findByIdAndUserId(activityId, userId).orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));
    }

    private void checkHobbyInProgressStatus(Hobby hobby) {
        if (!hobby.getStatus().equals(HobbyStatus.IN_PROGRESS)) {
            throw new CustomException(ErrorCode.INVALID_HOBBY_STATUS);
        }
    }
}
