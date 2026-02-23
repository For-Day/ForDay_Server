package com.example.ForDay.domain.app.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.example.ForDay.domain.app.dto.request.DeleteS3ImageReqDto;
import com.example.ForDay.domain.app.dto.request.GeneratePresignedReqDto;
import com.example.ForDay.domain.app.dto.response.AppMetaDataResDto;
import com.example.ForDay.domain.app.dto.response.GeneratePresignedUrlResDto;
import com.example.ForDay.domain.app.dto.response.VersionPolicyResDto;
import com.example.ForDay.domain.app.entity.AppVersion;
import com.example.ForDay.domain.app.repository.AppVersionRepository;
import com.example.ForDay.domain.app.type.Platform;
import com.example.ForDay.domain.app.type.UpdateType;
import com.example.ForDay.domain.hobby.repository.HobbyInfoRepository;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.common.response.dto.MessageResDto;
import com.example.ForDay.infra.s3.property.S3Properties;
import com.example.ForDay.infra.s3.service.S3Service;
import com.example.ForDay.infra.s3.util.S3Util;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppService {
    private final HobbyInfoRepository hobbyInfoRepository;
    private final S3Service s3Service;
    private final AmazonS3 amazonS3;
    private final S3Properties s3Properties;
    private final S3Util s3Util;
    private final AppVersionRepository appVersionRepository;

    @Transactional(readOnly = true)
    public AppMetaDataResDto getMetaData() {
        log.info("[getMetaData] 앱 메타데이터 조회 시작");
        List<AppMetaDataResDto.HobbyInfoDto> hobbyCardDtos =
                hobbyInfoRepository.findAll()
                        .stream()
                        .map(hobbyCard -> new AppMetaDataResDto.HobbyInfoDto(
                                hobbyCard.getId(),
                                hobbyCard.getHobbyName(),
                                hobbyCard.getHobbyDescription(),
                                hobbyCard.getImageCode()
                        ))
                        .toList();
        log.info("[getMetaData] 조회 완료 - 취미 정보 개수: {}개", hobbyCardDtos.size());

        return new AppMetaDataResDto(
                "1.0.0",
                hobbyCardDtos
        );
    }

    @Transactional
    public List<GeneratePresignedUrlResDto> generatePresignedUrls(@Valid GeneratePresignedReqDto reqDto) {
        log.info("[generatePresignedUrls] Presigned URL 발행 시작 - 요청 개수: {}개", reqDto.getImages().size());

        return reqDto.getImages().stream()
                .map(img -> {
                    // 이미지에 대한 key 값 생성
                    String key = s3Service.generateKey(
                            img.getUsage(),
                            img.getOriginalFilename()
                    );

                    // key값을 사용하여 s3에 presigned url 만들어달라고 s3에 요청
                    GeneratePresignedUrlRequest request =
                            s3Service.createPresignedPutRequest(
                                    s3Properties.getBucket(),
                                    key,
                                    img.getContentType()
                            );

                    String uploadUrl =
                            amazonS3.generatePresignedUrl(request).toString();

                    // 실제 접근 가능한 이미지 url
                    String fileUrl = s3Service.createFileUrl(key);

                    log.info("[generatePresignedUrls] URL 생성 완료 - Usage: {}, Key: {}", img.getUsage(), key);

                    return new GeneratePresignedUrlResDto(
                            uploadUrl,
                            fileUrl,
                            img.getOrder()
                    );
                })
                .toList();
    }

    @Transactional
    public MessageResDto deleteS3Image(DeleteS3ImageReqDto reqDto) {
        String imageUrl = reqDto.getImageUrl();
        log.info("[deleteS3Image] S3 이미지 삭제 프로세스 시작 - URL: {}", imageUrl);
        if (!StringUtils.hasText(imageUrl)) {
            log.warn("[deleteS3Image] 삭제 실패 - 빈 이미지 URL 입력됨");
            throw new CustomException(ErrorCode.INVALID_IMAGE_URL);
        }

        String originalKey = s3Service.extractKeyFromFileUrl(imageUrl);
        List<String> keysToDelete = new ArrayList<>();
        keysToDelete.add(originalKey);

        try {
            if (originalKey.contains("activity_record")) {
                keysToDelete.add(s3Service.extractKeyFromFileUrl(s3Util.toFeedThumbResizedUrl(imageUrl)));
            } else if (originalKey.contains("profile_image")) {
                keysToDelete.add(s3Service.extractKeyFromFileUrl(s3Util.toProfileMainResizedUrl(imageUrl)));
                keysToDelete.add(s3Service.extractKeyFromFileUrl(s3Util.toProfileListResizedUrl(imageUrl)));
            } else if (originalKey.contains("cover_image")) {
                keysToDelete.add(s3Service.extractKeyFromFileUrl(s3Util.toCoverMainResizedUrl(imageUrl)));
            } else {
                log.warn("[deleteS3Image] 삭제 실패 - 알 수 없는 이미지 경로: {}", originalKey);
                throw new CustomException(ErrorCode.INVALID_IMAGE_SOURCE);
            }
        } catch (Exception e) {
            log.error("[deleteS3Image] 리사이징 키 추출 중 예외 발생: {}", e.getMessage());
            throw new CustomException(ErrorCode.INVALID_IMAGE_URL);
        }

        log.info("[deleteS3Image] 삭제 대상 키 리스트: {}", keysToDelete);

        try {
            for (String key : keysToDelete) {
                if (key != null) {
                    s3Service.deleteByKey(key);
                    log.info("[deleteS3Image] S3 객체 삭제됨: {}", key);
                }
            }
        } catch (Exception e) {
            log.error("S3 이미지 삭제 중 오류 발생: {}", imageUrl, e);
            throw new CustomException(ErrorCode.S3_DELETE_ERROR);
        }

        log.info("[deleteS3Image] 모든 관련 이미지 삭제 완료 - Original Key: {}", originalKey);
        return new MessageResDto("이미지가 성공적으로 삭제되었습니다.");
    }

    @Transactional(readOnly = true)
    public VersionPolicyResDto getPolicy(Platform platform, String appVersion, int build) {
        // 1. 해당 플랫폼의 최신 정책 조회 (엔티티)
        AppVersion policy = appVersionRepository.findFirstByPlatformOrderByCreatedAtDesc(platform)
                .orElseThrow(() -> new CustomException(ErrorCode.PLATFORM_NOT_FOUND));

        // 2. 버전 비교를 위한 객체 생성
        AppVersionUtil current = AppVersionUtil.of(appVersion, build);
        AppVersionUtil minSupported = AppVersionUtil.of(policy.getMinSupportedVersion(), policy.getMinSupportedBuild());
        AppVersionUtil latest = AppVersionUtil.of(policy.getLatestVersion(), policy.getLatestBuild());

        // 3. 우선순위에 따른 업데이트 타입 및 메시지 결정
        UpdateType updateType;
        String message;

        if (policy.getBlockEnabled()) {
            updateType = UpdateType.BLOCK;
            message = policy.getBlockMessage();
        } else if (current.compareTo(minSupported) < 0) {
            updateType = UpdateType.FORCE;
            message = policy.getForceMessage();
        } else if (current.compareTo(latest) < 0) {
            updateType = UpdateType.RECOMMEND;
            message = policy.getRecommendMessage();
        } else {
            updateType = UpdateType.NONE;
            message = "";
        }

        // 4. DTO 변환 및 반환
        return new VersionPolicyResDto(
                policy.getPolicyVersion(),
                platform,
                new VersionPolicyResDto.Current(current.versionString(), current.build()),
                new VersionPolicyResDto.Threshold(minSupported.versionString(), minSupported.build()),
                new VersionPolicyResDto.Threshold(latest.versionString(), latest.build()),
                updateType,
                policy.getStoreUrl(),
                message
        );
    }
}
