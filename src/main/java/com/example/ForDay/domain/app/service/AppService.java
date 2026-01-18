package com.example.ForDay.domain.app.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.example.ForDay.domain.app.dto.request.DeleteS3ImageReqDto;
import com.example.ForDay.domain.app.dto.request.GeneratePresignedReqDto;
import com.example.ForDay.domain.app.dto.response.AppMetaDataResDto;
import com.example.ForDay.domain.app.dto.response.GeneratePresignedUrlResDto;
import com.example.ForDay.domain.hobby.repository.HobbyCardRepository;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.common.response.dto.MessageResDto;
import com.example.ForDay.infra.s3.S3Properties;
import com.example.ForDay.infra.s3.S3Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppService {
    private final HobbyCardRepository hobbyCardRepository;
    private final S3Service s3Service;
    private final AmazonS3 amazonS3;
    private final S3Properties s3Properties;

    @Transactional(readOnly = true)
    public AppMetaDataResDto getMetaData() {
        List<AppMetaDataResDto.HobbyCardDto> hobbyCardDtos =
                hobbyCardRepository.findAll()
                        .stream()
                        .map(hobbyCard -> new AppMetaDataResDto.HobbyCardDto(
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
        String key = s3Service.extractKeyFromFileUrl(reqDto.getImageUrl());

        // 이미지가 존재하는지 먼저 확인
        if(!s3Service.existsByKey(key)) throw new CustomException(ErrorCode.S3_IMAGE_NOT_FOUND);
        s3Service.deleteByKey(key);
        return new MessageResDto("이미지가 성공적으로 삭제되었습니다.");
    }
}
