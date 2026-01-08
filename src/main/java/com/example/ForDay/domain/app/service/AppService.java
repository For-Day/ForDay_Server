package com.example.ForDay.domain.app.service;

import com.example.ForDay.domain.app.dto.response.AppMetaDataResDto;
import com.example.ForDay.domain.hobby.repository.HobbyCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppService {
    private final HobbyCardRepository hobbyCardRepository;

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
}
