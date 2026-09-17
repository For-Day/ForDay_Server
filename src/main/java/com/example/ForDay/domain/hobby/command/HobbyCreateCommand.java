package com.example.ForDay.domain.hobby.command;

/**
 * 취미 생성에 필요한 값 묶음.
 *
 * <p>엔티티가 웹 계층 DTO를 알지 않도록 사이에 두는 도메인 타입이다.
 * 요청 DTO → 커맨드 변환은 서비스가 맡는다.
 *
 * @param goalDays 기간을 지정하지 않았으면 null
 */
public record HobbyCreateCommand(
        Long hobbyInfoId,
        String hobbyName,
        String hobbyPurpose,
        Integer hobbyTimeMinutes,
        Integer executionCount,
        Integer goalDays
) {
}
