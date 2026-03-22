package com.example.ForDay.domain.hobby.service;

import com.example.ForDay.domain.activity.dto.response.FastAPIHobbyCardResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.entity.HobbyCard;
import com.example.ForDay.domain.hobby.repository.HobbyCardRepository;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.ai.service.AIService;
import com.example.ForDay.infra.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class HobbyCardService {

    private static final String DEFAULT_HOBBY_IMAGE_URL = "https://your-bucket.s3.../default-hobby-image.png";
    private static final String COVER_IMAGE_PATH = "cover_image/temp/";
    private static final String HOBBY_CARD_IMAGE_PATH = "hobby_card/temp/";

    private final ActivityRecordRepository activityRecordRepository;
    private final HobbyCardRepository hobbyCardRepository;
    private final S3Service s3Service;
    private final AIService aiService;

    public HobbyCard createHobbyCard(User user, Hobby hobby) {
        FastAPIHobbyCardResDto response = aiService.requestHobbyCardContentAI(hobby);
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
            String coverImageKey = s3Service.extractKeyFromFileUrl(coverImageUrl);
            String hobbyCardImageKey = coverImageKey.replace(COVER_IMAGE_PATH, HOBBY_CARD_IMAGE_PATH);
            s3Service.copyObject(coverImageKey, hobbyCardImageKey);

            String hobbyCardImageUrl = s3Service.createFileUrl(hobbyCardImageKey);
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