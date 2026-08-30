package com.example.ForDay.domain.auth.port;

import com.example.ForDay.domain.auth.dto.response.ApplePublicKeyDto;

/**
 * Apple 신원 서버 조회.
 *
 * <p>auth 도메인만 쓰므로 도메인 안에 둔다 (ADR-0001 §7 포트 위치 규칙).
 */
public interface AppleIdentityPort {

    /** id_token 서명 검증에 쓸 Apple 공개키 목록을 가져온다. */
    ApplePublicKeyDto fetchPublicKeys();
}
