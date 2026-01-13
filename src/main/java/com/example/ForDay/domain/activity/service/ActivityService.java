package com.example.ForDay.domain.activity.service;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.entity.ActivityRecord;
import com.example.ForDay.domain.activity.entity.ActivityRecordImage;
import com.example.ForDay.domain.activity.repository.ActivityRecordRepository;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.hobby.dto.request.RecordActivityReqDto;
import com.example.ForDay.domain.hobby.dto.response.RecordActivityResDto;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.RedisUtil;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.infra.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ActivityService {
    private final UserUtil userUtil;
    private final ActivityRepository activityRepository;
    private final S3Service s3Service;
    private final ActivityRecordRepository activityRecordRepository;
    private final RedisUtil redisUtil;

    public RecordActivityResDto recordActivity(Long activityId, RecordActivityReqDto reqDto, CustomUserDetails user) {
        Activity activity = getActivity(activityId);
        User currentUser = userUtil.getCurrentUser(user);

        // redis 키 생성
        String redisKey = redisUtil.createRecordKey(currentUser.getId(), activity.getHobby().getId());

        // Redis를 통한 중복 체크
        if (redisUtil.hasKey(redisKey)) {
            throw new CustomException(ErrorCode.ALREADY_RECORDED_TODAY);
        }

        verifyActivityOwner(activity, currentUser);

        ActivityRecord activityRecord = ActivityRecord.builder()
                .activity(activity)
                .user(currentUser)
                .sticker(reqDto.getSticker())
                .memo(reqDto.getMemo())
                .visibility(reqDto.getVisibility())
                .build();

        activity.record();

        // 해당 이미지가 s3에 업로드 되었는지 확인
        if (reqDto.getImages() != null) {
            reqDto.getImages().forEach(imgDto -> {
                // URL에서 Key 추출 후 S3 존재 여부 확인
                String s3Key = s3Service.extractKeyFromFileUrl(imgDto.getImageUrl());
                if (!s3Service.existsByKey(s3Key)) {
                    throw new CustomException(ErrorCode.S3_IMAGE_NOT_FOUND);
                }

                ActivityRecordImage image = ActivityRecordImage.builder()
                        .imageUrl(imgDto.getImageUrl())
                        .sortOrder(imgDto.getOrder())
                        .build();

                activityRecord.addImage(image); // 연관관계 편의 메서드 사용
            });
        }

        activityRecordRepository.save(activityRecord);
        redisUtil.setDataExpire(redisKey, "recorded", 86400);

        String thumbnail = (reqDto.getImages() != null && !reqDto.getImages().isEmpty())
                ? reqDto.getImages().get(0).getImageUrl() : null;

        return new RecordActivityResDto(
                "오늘의 활동 기록이 정상적으로 작성되었습니다",
                activityRecord.getId(),
                activity.getContent(),
                thumbnail,
                reqDto.getSticker()
        );
    }

    private Activity getActivity(Long activityId) {
        return activityRepository.findById(activityId).orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));
    }

    private void verifyActivityOwner(Activity activity, User currentUser) {
        if (!Objects.equals(activity.getUser(), currentUser)) {
            throw new CustomException(ErrorCode.NOT_ACTIVITY_OWNER);
        }
    }
}
