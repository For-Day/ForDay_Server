package com.example.ForDay.domain.auth.service;

import com.example.ForDay.domain.auth.dto.AccessTokenDto;
import com.example.ForDay.domain.auth.dto.KakaoProfileDto;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class KakaoService {

    @Value("${oauth.kakao.client-id}")
    private String kakaoClientId;


    @Value("${oauth.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    public AccessTokenDto getAccessToken(String code) {
        if (code == null || code.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_KAKAO_CODE);
        }

        try {
            RestClient restClient = RestClient.create(); // RestClient 객체 생성

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code", code);
            params.add("client_id", kakaoClientId);
            params.add("redirect_uri", kakaoRedirectUri);
            params.add("grant_type", "authorization_code");

            ResponseEntity<AccessTokenDto > response = restClient.post()
                    .uri("https://kauth.kakao.com/oauth/token") // token_uri
                    .header("Content-Type","application/x-www-form-urlencoded")
                    .body(params)
                    .retrieve()
                    .toEntity(AccessTokenDto .class);
            System.out.println("응답 accesstoken JSON " + response.getBody());
            return response.getBody();
        } catch (Exception e) {
            throw new CustomException(ErrorCode.KAKAO_TOKEN_REQUEST_FAILED);
        }
    }

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
