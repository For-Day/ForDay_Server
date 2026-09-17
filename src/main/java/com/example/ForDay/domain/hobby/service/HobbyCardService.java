package com.example.ForDay.domain.hobby.service;

import com.example.ForDay.domain.activity.dto.response.FastAPIHobbyCardResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.entity.HobbyCard;
import com.example.ForDay.domain.hobby.repository.HobbyCardRepository;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.ai.service.AiHobbyCardService;
import com.example.ForDay.global.port.ImageUrlPort;
import com.example.ForDay.global.port.ImageLifecyclePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static com.example.ForDay.global.common.constants.FileStorageConstants.TEMP_COVER_PATH;
import static com.example.ForDay.global.common.constants.FileStorageConstants.TEMP_HOBBY_CARD_PATH;

@Service
@RequiredArgsConstructor
@Slf4j
public class HobbyCardService {

    private static final String DEFAULT_HOBBY_IMAGE_URL = "https://your-bucket.s3.../default-hobby-image.png";

    private final ActivityRecordRepository activityRecordRepository;
    private final HobbyCardRepository hobbyCardRepository;
    private final ImageUrlPort imageUrlPort;
    private final ImageLifecyclePort imageLifecyclePort;
    private final AiHobbyCardService aiHobbyCardService;

    public HobbyCard createHobbyCard(User user, Hobby hobby) {
        FastAPIHobbyCardResDto response = aiHobbyCardService.requestHobbyCardContentAI(hobby);
        String hobbyCardImageUrl = resolveHobbyCardImageUrl(hobby);

        HobbyCard hobbyCard = HobbyCard.of(user, hobby, response.getContent(), hobbyCardImageUrl);
        hobbyCardRepository.save(hobbyCard);

        log.info("[HobbyCard] 생성 완료 - 카드ID: {}, 사용자: {}", hobbyCard.getId(), user.getId());
        return hobbyCard;
    }

    private String resolveHobbyCardImageUrl(Hobby hobby) {
        String coverImageUrl = resolveCoverImageUrl(hobby);
        if (!StringUtils.hasText(coverImageUrl)) {
            return null;
        }

        try {
            String coverImageKey = imageUrlPort.extractKeyFromFileUrl(coverImageUrl);
            String hobbyCardImageKey = coverImageKey.replace(TEMP_COVER_PATH, TEMP_HOBBY_CARD_PATH);
            imageLifecyclePort.copy(coverImageKey, hobbyCardImageKey);

            String hobbyCardImageUrl = imageUrlPort.createFileUrl(hobbyCardImageKey);
            log.info("[HobbyCard] S3 이미지 복사 완료 - {} -> {}", coverImageKey, hobbyCardImageKey);
            return hobbyCardImageUrl;

        } catch (Exception e) {
            log.warn("[HobbyCard] S3 이미지 처리 중 오류 발생 (프로세스는 계속됨) - {}", e.getMessage());
            return null;
        }
    }

    private String resolveCoverImageUrl(Hobby hobby) {
        if (hobby.getCoverImageUrl() != null) {
            return hobby.getCoverImageUrl();
        }
        return activityRecordRepository.findLatestImageRecord(hobby.getId())
                .map(ActivityRecord::getImageUrl)
                .orElse(DEFAULT_HOBBY_IMAGE_URL);
    }
}