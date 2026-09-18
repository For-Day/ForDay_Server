package com.example.ForDay.global.ai.service;

import com.example.ForDay.global.ai.document.AiCallLog;
import com.example.ForDay.global.ai.repository.AiCallLogRepository;
import com.example.ForDay.global.ai.type.AiCallType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiCallLogService - AI 호출 이력 저장")
class AiCallLogServiceTest {

    @Mock private AiCallLogRepository aiCallLogRepository;

    private AiCallLogService sut;

    private AiCallLog sampleLog() {
        return AiCallLog.builder()
                .userId("user-1")
                .hobbyId(10L)
                .callType(AiCallType.HOBBY_CARD)
                .model("gpt-4o-mini")
                .temperature(0.6)
                .success(true)
                .build();
    }

    @Nested
    @DisplayName("저장이 정상적으로 되면")
    class WhenSaveSucceeds {

        @Test
        @DisplayName("리포지토리에 그대로 저장한다")
        void 저장한다() {
            sut = new AiCallLogService(aiCallLogRepository);
            AiCallLog log = sampleLog();

            sut.record(log);

            verify(aiCallLogRepository).save(log);
        }
    }

    @Nested
    @DisplayName("MongoDB 저장이 실패하면")
    class WhenSaveFails {

        @Test
        @DisplayName("예외를 삼키고 호출자에게 전파하지 않는다")
        void 예외를_전파하지_않는다() {
            sut = new AiCallLogService(aiCallLogRepository);
            AiCallLog log = sampleLog();
            willThrow(new RuntimeException("Mongo 접속 실패")).given(aiCallLogRepository).save(log);

            assertThatCode(() -> sut.record(log)).doesNotThrowAnyException();
        }
    }
}
