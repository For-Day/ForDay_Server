package com.example.ForDay.domain.hobby.port;

/**
 * 취미 커버 이미지의 리사이즈본 생성을 외부에 요청한다.
 *
 * <p>hobby 도메인만 쓰므로 도메인 안에 둔다 (ADR-0001 §7 포트 위치 규칙).
 */
public interface CoverGeneratorPort {

    /**
     * @param sourceKey      원본 커버 키
     * @param destinationKey 생성할 리사이즈본 키
     * @throws Exception 생성 실패. 기존 동작(호출부까지 전파)을 그대로 유지하기 위해 그대로 둔다.
     */
    void generateCover(String sourceKey, String destinationKey) throws Exception;
}
