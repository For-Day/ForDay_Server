package com.example.ForDay.infra.apple.adapter;

import com.example.ForDay.domain.auth.dto.response.ApplePublicKeyDto;
import com.example.ForDay.domain.auth.port.AppleIdentityPort;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Apple 공개키 엔드포인트를 호출한다.
 *
 * <p>이전에는 AppleService가 메서드 안에서 RestTemplate을 직접 생성해
 * 스텁을 끼워 넣을 지점 자체가 없었다. HTTP 호출을 이 어댑터로 옮겨
 * 도메인은 포트만 알게 한다.
 */
@Component
public class AppleIdentityRestAdapter implements AppleIdentityPort {

    private static final String APPLE_PUBLIC_KEY_URL = "https://appleid.apple.com/auth/keys";

    private final RestTemplate restTemplate;

    public AppleIdentityRestAdapter() {
        this(new RestTemplate());
    }

    AppleIdentityRestAdapter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public ApplePublicKeyDto fetchPublicKeys() {
        return restTemplate.getForObject(APPLE_PUBLIC_KEY_URL, ApplePublicKeyDto.class);
    }
}
