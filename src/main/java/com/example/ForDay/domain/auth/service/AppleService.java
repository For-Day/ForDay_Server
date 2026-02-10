package com.example.ForDay.domain.auth.service;

import com.example.ForDay.domain.auth.dto.response.AppleTokenResDto;
import com.example.ForDay.domain.auth.dto.response.ApplePublicKeyDto;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppleService {
    @Value("${apple.client_id}")
    private String clientId;

    @Value("${apple.team_id}")
    private String teamId;

    @Value("${apple.key}")
    private String appleSecret;

    @Value("${apple.private_key}")
    private String privateKey;

    private static final long THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000;

    public AppleTokenResDto getAppleToken(String code) {
        // code와 애플 설정값을 이용하여 직접 JWT 토큰 생성후 apple api에 유저 정보 요청을 보낸다. -> 응답으로 idToken과 accessToken을 받는다.
        WebClient webClient = WebClient.builder()
                .baseUrl("https://appleid.apple.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=utf-8")
                .build();

        try { // webClient를 사용하여 애플 api에 아래와 같이 파라미터를 넣어 요청을 보낸다.
            return webClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/auth/token")
                            .queryParam("grant_type", "authorization_code")
                            .queryParam("client_id", clientId)
                            .queryParam("client_secret", makeClientSecretToken()) // JWT 토큰 직접 생성 메서드
                            .queryParam("code", code)
                            .build()
                    )
                    .retrieve()
                    .bodyToMono(AppleTokenResDto.class)
                    .block();

        } catch (WebClientResponseException e) {
            log.error("[LOGIN] Apple login failed");
            throw new CustomException(ErrorCode.APPLE_PROFILE_REQUEST_FAILED);
        }
    }

    private String makeClientSecretToken() {
        String token = Jwts.builder()
                .subject(clientId) // sub
                .issuer(teamId) // iss
                .issuedAt(new Date()) // iat
                .expiration(new Date(System.currentTimeMillis() + THIRTY_DAYS_MS)) // exp
                .audience() // aud
                .add("https://appleid.apple.com")
                .and()
                .header()
                .keyId(appleSecret)
                .and()
                .signWith(getPrivateKey(), Jwts.SIG.ES256)
                .compact();
        log.info("[LOGIN] APPLE LOGIN REQUEST AUTHORIZATION TOKEN: " + token);
        return token;
    }

    private PrivateKey getPrivateKey() {
        try {
            byte[] privateKeyBytes = Decoders.BASE64.decode(privateKey);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            log.info("[LOGIN] Apple Login PrivateKey Generate Failed", e);
            throw new CustomException(ErrorCode.APPLE_LOGIN_FAILED);
        }
    }

    public Claims verifyAndParseAppleIdToken(AppleTokenResDto resDto) {
        ApplePublicKeyDto applePublicKeys = getPublicKeys();

        MyKeyLocator myKeyLocator =
                new MyKeyLocator(applePublicKeys.getKeys());

        Claims claims = Jwts.parser()
                .keyLocator(myKeyLocator)
                .build()
                .parseSignedClaims(resDto.getIdToken())
                .getPayload();

        log.info("[LOGIN] Apple Login IdToken Validation Complete: ", claims.toString());

        return claims;
    }

    private ApplePublicKeyDto getPublicKeys() {
        RestTemplate restTemplate = new RestTemplate();

        String applePublicKeyUrl = "https://appleid.apple.com/auth/keys";

        return restTemplate.getForObject(
                applePublicKeyUrl,
                ApplePublicKeyDto.class
        );
    }
}
