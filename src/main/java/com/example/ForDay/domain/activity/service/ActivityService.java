package com.example.ForDay.domain.activity.service;

import com.example.ForDay.domain.activity.dto.request.UpdateActivityReqDto;
import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.entity.ActivityRecord;
import com.example.ForDay.domain.activity.repository.ActivityRecordRepository;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.hobby.dto.request.RecordActivityReqDto;
import com.example.ForDay.domain.hobby.dto.response.RecordActivityResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.common.response.dto.MessageResDto;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.RedisUtil;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.infra.s3.S3Service;
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
    private final RedisUtil redisUtil;
    private final HobbyRepository hobbyRepository;

    @Transactional
    public RecordActivityResDto recordActivity(
            Long activityId,
            RecordActivityReqDto reqDto,
            CustomUserDetails user
    ) {
        Activity activity = getActivity(activityId);
        User currentUser = userUtil.getCurrentUser(user);

        log.info("[RecordActivity] 시작 - UserId: {}, ActivityId: {}", currentUser.getId(), activityId);

        verifyActivityOwner(activity, currentUser); // 활동 소유자인지 검증
        Hobby hobby = activity.getHobby();

        if(isCheckStickerFull(hobby)) throw new CustomException(ErrorCode.STICKER_COMPLETION_REACHED);

        checkHobbyInProgressStatus(hobby); // 진행 중인 취미에 대해서만 활동 기록 가능

        String redisKey = redisUtil.createRecordKey(currentUser.getId(), hobby.getId());
        if (redisUtil.hasKey(redisKey)) { // 해당 취미에 대해 오늘 기록한 활동이 있는지 확인
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

        activity.record();
        currentUser.obtainSticker();
        activityRecordRepository.save(activityRecord);

        redisUtil.setDataExpire(redisKey, "recorded", 86400);

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
        Activity activity = getActivity(activityId);
        User currentUser = userUtil.getCurrentUser(user);

        verifyActivityOwner(activity, currentUser); // 활동 소유자인지 검증
        Hobby hobby = activity.getHobby();

        if(isCheckStickerFull(hobby)) throw new CustomException(ErrorCode.STICKER_COMPLETION_REACHED);
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

        Activity activity = getActivity(activityId);
        User currentUser = userUtil.getCurrentUser(user);

        verifyActivityOwner(activity, currentUser);

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

        Activity activity = getActivity(activityId);
        User currentUser = userUtil.getCurrentUser(user);
        verifyActivityOwner(activity, currentUser);

        // 삭제 가능 여부
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

    private Activity getActivity(Long activityId) {
        return activityRepository.findById(activityId).orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));
    }

    private void verifyActivityOwner(Activity activity, User currentUser) {
        if (!Objects.equals(activity.getUser(), currentUser)) {
            log.warn("[RecordActivity] 소유권 검증 실패 - ActivityOwnerId: {}, CurrentUserId: {}",
                    activity.getUser().getId(), currentUser.getId());
            throw new CustomException(ErrorCode.NOT_ACTIVITY_OWNER);
        }
    }

    private void checkHobbyInProgressStatus(Hobby hobby) {
        if(!hobby.getStatus().equals(HobbyStatus.IN_PROGRESS)) {
            throw new CustomException(ErrorCode.INVALID_HOBBY_STATUS);
        }
    }
}
