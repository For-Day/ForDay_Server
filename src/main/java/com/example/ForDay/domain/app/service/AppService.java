package com.example.ForDay.domain.app.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.example.ForDay.domain.app.dto.request.GeneratePresignedReqDto;
import com.example.ForDay.domain.app.dto.response.AppMetaDataResDto;
import com.example.ForDay.domain.app.dto.response.PresignedUrlResDto;
import com.example.ForDay.domain.hobby.repository.HobbyCardRepository;
import com.example.ForDay.infra.s3.S3Properties;
import com.example.ForDay.infra.s3.S3Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppService {
    private final HobbyCardRepository hobbyCardRepository;
    private final S3Service s3Service;
    private final AmazonS3 amazonS3;
    private final S3Properties s3Properties;

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

    public List<PresignedUrlResDto> generatePresignedUrls(@Valid GeneratePresignedReqDto reqDto) {
        return reqDto.getImages().stream()
                .map(img -> {
                    String key = s3Service.generateKey(
                            img.getUsage(),
                            img.getOriginalFilename()
                    );

                    // s3에 presigned url 만들어달라고 s3에 요청
                    GeneratePresignedUrlRequest request =
                            s3Service.createPresignedPutRequest(
                                    s3Properties.getBucket(),
                                    key,
                                    img.getContentType()
                            );

                    String uploadUrl =
                            amazonS3.generatePresignedUrl(request).toString();

                    String fileUrl = s3Service.createFileUrl(key);

                    return new PresignedUrlResDto(
                            uploadUrl,
                            fileUrl,
                            img.getOrder()
                    );
                })
                .toList();
    }
}
