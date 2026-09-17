package com.example.ForDay.domain.record.command;

import com.example.ForDay.domain.record.type.RecordVisibility;

/**
 * 활동 기록 생성에 필요한 값 묶음.
 *
 * <p>엔티티가 웹 계층 DTO를 알지 않도록 사이에 두는 도메인 타입이다.
 * 요청 DTO → 커맨드 변환은 서비스가 맡는다.
 *
 * <p>V1은 단일 이미지, V2는 이미지 목록의 첫 장을 대표 이미지로 넘긴다.
 */
public record RecordCreateCommand(
        String sticker,
        String memo,
        RecordVisibility visibility,
        String imageUrl
) {
}
