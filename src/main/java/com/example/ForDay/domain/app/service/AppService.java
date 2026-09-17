package com.example.ForDay.domain.app.service;

import com.example.ForDay.domain.app.dto.request.DeleteS3ImageReqDto;
import com.example.ForDay.domain.app.dto.request.GeneratePresignedReqDto;
import com.example.ForDay.domain.app.dto.response.AppMetaDataResDto;
import com.example.ForDay.domain.app.dto.response.GeneratePresignedUrlResDto;
import com.example.ForDay.domain.app.dto.response.VersionPolicyResDto;
import com.example.ForDay.domain.app.entity.AppVersion;
import com.example.ForDay.domain.app.repository.AppVersionRepository;
import com.example.ForDay.domain.app.type.Platform;
import com.example.ForDay.domain.app.type.UpdateType;
import com.example.ForDay.domain.app.utils.AppVersionUtil;
import com.example.ForDay.domain.hobby.repository.HobbyInfoRepository;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.common.response.dto.MessageResDto;
import com.example.ForDay.global.common.response.message.AppSuccessCode;
import com.example.ForDay.global.port.ImageLifecyclePort;
import com.example.ForDay.global.port.ImageUploadPort;
import com.example.ForDay.global.port.UploadTarget;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppService {
    private final HobbyInfoRepository hobbyInfoRepository;
    private final AppVersionRepository appVersionRepository;
    private final ImageUploadPort imageUploadPort;
    private final ImageLifecyclePort imageLifecyclePort;

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
                    UploadTarget target = imageUploadPort.issueUploadUrl(
                            img.getUsage(),
                            img.getOriginalFilename(),
                            img.getContentType()
                    );

                    log.info("[generatePresignedUrls] URL 생성 완료 - Usage: {}", img.getUsage());
                    return GeneratePresignedUrlResDto.of(target.uploadUrl(), target.fileUrl(), img.getOrder());
                })
                .toList();
    }

    @Transactional
    public MessageResDto deleteS3Image(DeleteS3ImageReqDto reqDto) {
        String imageUrl = reqDto.getImageUrl();

        if (!StringUtils.hasText(imageUrl)) {
            log.warn("[deleteS3Image] 삭제 실패 - 빈 이미지 URL 입력됨");
            throw new CustomException(ErrorCode.INVALID_IMAGE_URL);
        }

        log.info("[deleteS3Image] S3 이미지 삭제 예약 - URL: {}", imageUrl);
        imageLifecyclePort.deleteAfterCommit(imageUrl);

        return new MessageResDto(AppSuccessCode.DELETE_S3_IMAGE_SUCCESS.getMessage());
    }

    @Transactional(readOnly = true)
    public VersionPolicyResDto getPolicy(Platform platform, String appVersion, int build) {
        // 해당 플랫폼의 최신 정책 조회 (엔티티)
        AppVersion policy = appVersionRepository.findFirstByPlatformOrderByCreatedAtDesc(platform)
                .orElseThrow(() -> new CustomException(ErrorCode.PLATFORM_NOT_FOUND));

        // 버전 비교를 위한 객체 생성
        AppVersionUtil current = AppVersionUtil.of(appVersion, build);
        AppVersionUtil minSupported = AppVersionUtil.of(policy.getMinSupportedVersion(), policy.getMinSupportedBuild());
        AppVersionUtil latest = AppVersionUtil.of(policy.getLatestVersion(), policy.getLatestBuild());

        // 우선순위에 따른 업데이트 타입 및 메시지 결정
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
