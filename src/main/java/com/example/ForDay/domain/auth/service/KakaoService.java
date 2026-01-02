package com.example.ForDay.domain.auth.service;

import com.example.ForDay.domain.auth.dto.KakaoProfileDto;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class KakaoService {

    public KakaoProfileDto getKakaoProfile(String token) {
        try {
            RestClient restClient = RestClient.create();

            ResponseEntity<KakaoProfileDto> response = restClient.get()
                    .uri("https://kapi.kakao.com/v2/user/me") // token_uri
                    .header("Authorization","Bearer " + token)
                    .retrieve()
                    .toEntity(KakaoProfileDto.class);
            System.out.println("profile JSON: " + response.getBody());
            return response.getBody();
        } catch (Exception e) {
            throw new CustomException(ErrorCode.KAKAO_PROFILE_REQUEST_FAILED);
        }
    }
}
