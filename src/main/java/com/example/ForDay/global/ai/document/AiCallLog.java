package com.example.ForDay.global.ai.document;

import com.example.ForDay.global.ai.type.AiCallType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * 이슈 #406/#407 - AI 호출(프롬프트/원본 응답) 이력을 남기는 MongoDB document.
 *
 * <p>호출 종류({@link AiCallType})마다 {@code prompt}/{@code rawResponse}의 구조가
 * 전혀 다르다(평문 문자열 vs 구조화된 객체, 변수 개수도 제각각). RDB처럼 컬럼을
 * 미리 정의하지 않고 그대로 저장하기 위해 스키마리스 컬렉션으로 둔다.
 *
 * <p>{@code userId}/{@code hobbyId}는 호출 지점에 따라 없을 수 있다
 * (예: {@code SIMPLE_RECOMMEND}는 취미 생성 전 상태 없는 호출이라 둘 다 null).
 *
 * <p>이 이력 저장이 실패해도 AI 응답 흐름 자체는 깨지면 안 된다 - 예외 격리는
 * {@link com.example.ForDay.global.ai.service.AiCallLogService}가 담당한다.
 */
@Document(collection = "ai_call_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiCallLog {

    @Id
    private String id;

    private String userId;
    private Long hobbyId;

    private AiCallType callType;
    private String model;
    private Double temperature;

    private Map<String, Object> prompt;
    private Object rawResponse;

    private boolean success;
    private String errorCode;

    private LocalDateTime createdAt;

    @Builder
    private AiCallLog(String userId, Long hobbyId, AiCallType callType, String model, Double temperature,
                       Map<String, Object> prompt, Object rawResponse, boolean success, String errorCode) {
        this.userId = userId;
        this.hobbyId = hobbyId;
        this.callType = callType;
        this.model = model;
        this.temperature = temperature;
        this.prompt = prompt;
        this.rawResponse = rawResponse;
        this.success = success;
        this.errorCode = errorCode;
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}
