package com.example.ForDay.domain.activity.service;

import com.example.ForDay.domain.activity.dto.request.UpdateActivityReqDto;
import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.entity.ActivityRecord;
import com.example.ForDay.domain.activity.repository.ActivityRecordRepository;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.hobby.dto.request.RecordActivityReqDto;
import com.example.ForDay.domain.hobby.dto.response.GetActivityListResDto;
import com.example.ForDay.domain.hobby.dto.response.RecordActivityResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.common.response.dto.MessageResDto;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.RedisUtil;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.infra.s3.S3Service;
import jakarta.validation.Valid;
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
    private final UserUtil userUtil;
    private final ActivityRepository activityRepository;
    private final S3Service s3Service;
    private final ActivityRecordRepository activityRecordRepository;
    private final RedisUtil redisUtil;

    @Transactional
    public RecordActivityResDto recordActivity(
            Long activityId,
            RecordActivityReqDto reqDto,
            CustomUserDetails user
    ) {
        Activity activity = getActivity(activityId);
        User currentUser = userUtil.getCurrentUser(user);

        log.info("[RecordActivity] 시작 - UserId: {}, ActivityId: {}", currentUser.getId(), activityId);

        verifyActivityOwner(activity, currentUser);

        String redisKey = redisUtil.createRecordKey(currentUser.getId(), activity.getHobby().getId());

        if (redisUtil.hasKey(redisKey)) {
            log.warn("[RecordActivity] 중복 기록 시도 - UserId: {}, HobbyId: {}",
                    currentUser.getId(), activity.getHobby().getId());
            throw new CustomException(ErrorCode.ALREADY_RECORDED_TODAY);
        }

        if (StringUtils.hasText(reqDto.getImageUrl())) {
            String s3Key = s3Service.extractKeyFromFileUrl(reqDto.getImageUrl());
            if (!s3Service.existsByKey(s3Key)) {
                log.error("[RecordActivity] S3 이미지 부재 - Key: {}", s3Key);
                throw new CustomException(ErrorCode.S3_IMAGE_NOT_FOUND);
            }
        }

        ActivityRecord activityRecord = ActivityRecord.builder()
                .activity(activity)
                .user(currentUser)
                .sticker(reqDto.getSticker())
                .memo(reqDto.getMemo())
                .visibility(reqDto.getVisibility())
                .imageUrl(reqDto.getImageUrl())
                .build();

        activity.record();

        activityRecordRepository.save(activityRecord);

        redisUtil.setDataExpire(redisKey, "recorded", 86400);

        return new RecordActivityResDto(
                "오늘의 활동 기록이 정상적으로 작성되었습니다",
                activityRecord.getId(),
                activity.getContent(),
                activityRecord.getImageUrl(),
                reqDto.getSticker()
        );
    }


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

    public MessageResDto updateActivity(Long activityId, UpdateActivityReqDto reqDto, CustomUserDetails user) {
        Activity activity = getActivity(activityId);
        User currentUser = userUtil.getCurrentUser(user);
        verifyActivityOwner(activity, currentUser);

        activity.updateContent(reqDto.getContent());
        return new MessageResDto("활동이 정상적으로 수정되었습니다.");
    }
}
