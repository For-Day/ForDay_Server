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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        String key = s3Service.extractKeyFromFileUrl(imageUrl);

        if(!s3Service.existsByKey(key))
            throw new CustomException(ErrorCode.S3_IMAGE_NOT_FOUND);
        s3Service.deleteByKey(key);

        if(key.contains("/activity_record")) {
            // 썸네일용 (/resized/thumb/)
            String feedThumbResizedUrl = s3Util.toFeedThumbResizedUrl(imageUrl);
            String feedThumbResizedKey = s3Service.extractKeyFromFileUrl(feedThumbResizedUrl);
            if(s3Service.existsByKey(feedThumbResizedKey)) s3Service.deleteByKey(feedThumbResizedKey);

        } else if (key.contains("profile_image")) {
            // 프로필 조회용 (/resized/main/)
            String profileMainResizedUrl = s3Util.toFeedThumbResizedUrl(imageUrl);
            String profileMainResizedKey = s3Service.extractKeyFromFileUrl(profileMainResizedUrl);
            if(s3Service.existsByKey(profileMainResizedKey)) s3Service.deleteByKey(profileMainResizedKey);

            // 리스트 썸네일용 (/resized/list/)
            String profileListResizedUrl = s3Util.toFeedThumbResizedUrl(imageUrl);
            String profileListResizedKey = s3Service.extractKeyFromFileUrl(profileListResizedUrl);
            if(s3Service.existsByKey(profileListResizedKey)) s3Service.deleteByKey(profileListResizedKey);

        } else if (key.contains("cover_image")) {
            // 썸네일용 (/resized/thumb/)
            String coverThumbResizedUrl = s3Util.toFeedThumbResizedUrl(imageUrl);
            String coverThumbResizedKey = s3Service.extractKeyFromFileUrl(coverThumbResizedUrl);
            if(s3Service.existsByKey(coverThumbResizedKey)) s3Service.deleteByKey(coverThumbResizedKey);
        } else {
            throw new CustomException(ErrorCode.INVALID_IMAGE_SOURCE);
        }
        return new MessageResDto("이미지가 성공적으로 삭제되었습니다.");
    }
}
