package com.example.ForDay.global.port;

/**
 * 이미지 저장소의 키 ↔ URL 변환.
 *
 * <p>여러 도메인이 공유하므로 global에 둔다 (ADR-0001 §7 포트 위치 규칙).
 */
public interface ImageUrlPort {

    String createFileUrl(String key);

    String extractKeyFromFileUrl(String fileUrl);
}
