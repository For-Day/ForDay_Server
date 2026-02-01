package com.example.ForDay.domain.app.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.example.ForDay.domain.app.dto.request.DeleteS3ImageReqDto;
import com.example.ForDay.domain.app.dto.request.GeneratePresignedReqDto;
import com.example.ForDay.domain.app.dto.response.AppMetaDataResDto;
import com.example.ForDay.domain.app.dto.response.GeneratePresignedUrlResDto;
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

    @Transactional(readOnly = true)
    public AppMetaDataResDto getMetaData() {
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

        return new AppMetaDataResDto(
                "1.0.0", // 앱 버전 관리는 나중에 구현
                hobbyCardDtos
        );
    }

    @Transactional
    public List<GeneratePresignedUrlResDto> generatePresignedUrls(@Valid GeneratePresignedReqDto reqDto) {
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
        if (!StringUtils.hasText(imageUrl)) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_URL);
        }

        String originalKey = s3Service.extractKeyFromFileUrl(imageUrl);
        List<String> keysToDelete = new ArrayList<>();
        keysToDelete.add(originalKey);

        if (originalKey.contains("/activity_record")) {
            keysToDelete.add(s3Service.extractKeyFromFileUrl(s3Util.toFeedThumbResizedUrl(imageUrl)));

        } else if (originalKey.contains("/profile_image")) {
            keysToDelete.add(s3Service.extractKeyFromFileUrl(s3Util.toProfileMainResizedUrl(imageUrl))); // 메인
            keysToDelete.add(s3Service.extractKeyFromFileUrl(s3Util.toProfileListResizedUrl(imageUrl))); // 리스트

        } else if (originalKey.contains("/cover_image")) {
            keysToDelete.add(s3Service.extractKeyFromFileUrl(s3Util.toCoverMainResizedUrl(imageUrl)));
        } else {
            throw new CustomException(ErrorCode.INVALID_IMAGE_SOURCE);
        }

        // 2. 일괄 삭제 수행 (에러 방지를 위해 try-catch 권장)
        try {
            for (String key : keysToDelete) {
                if (key != null) {
                    s3Service.deleteByKey(key);
                }
            }
        } catch (Exception e) {
            log.error("S3 이미지 삭제 중 오류 발생: {}", imageUrl, e);
            throw new CustomException(ErrorCode.S3_DELETE_ERROR);
        }

        return new MessageResDto("이미지가 성공적으로 삭제되었습니다.");
    }
}
