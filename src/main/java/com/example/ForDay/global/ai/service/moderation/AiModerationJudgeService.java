package com.example.ForDay.global.ai.service.moderation;

import com.example.ForDay.global.ai.dto.moderation.ModerationJudgeInput;
import com.example.ForDay.global.ai.dto.moderation.ModerationJudgment;
import com.example.ForDay.global.ai.dto.moderation.ModerationLlmOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 이슈 #423 - 신고된 기록을 운영 정책 기준으로 1차 판정한다.
 *
 * <p><b>내부 테스트 전용.</b> 운영 개인정보처리방침에 외부 위탁·국외 이전 조항이 없어 운영 서비스에는
 * 적용하지 않는다. {@code moderation.enabled=true}일 때만 빈으로 등록되며, 운영 설정에는 이 값을 넣지
 * 않는다. 오프라인 평가 러너는 빈 대신 생성자로 직접 만든다.
 *
 * <p>판정 실패(호출 오류·형식 오류)는 예외로 올리지 않고 UNCERTAIN(사람 검토)으로 돌려준다 -
 * 실패가 자동 조치나 신고 처리 중단으로 이어지지 않게 하기 위해서다.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "moderation.enabled", havingValue = "true")
public class AiModerationJudgeService {

    private final ChatClient chatClient;
    private final String promptVersion;

    @Autowired
    public AiModerationJudgeService(ChatClient chatClient) {
        this(chatClient, ModerationPromptBuilder.DEFAULT_VERSION);
    }

    public AiModerationJudgeService(ChatClient chatClient, String promptVersion) {
        this.chatClient = chatClient;
        this.promptVersion = promptVersion;
    }

    public ModerationJudgment judge(ModerationJudgeInput input) {
        ModerationPromptBuilder.Prompt prompt = ModerationPromptBuilder.build(input, promptVersion);
        try {
            ModerationLlmOutput out = chatClient.prompt()
                    .system(prompt.system())
                    .user(prompt.user())
                    // 전역 ChatClient 기본 temperature(0.6)는 문구 생성용이다. 판정은 재현성이 우선이라 0.
                    .options(OpenAiChatOptions.builder().temperature(0.0).build())
                    .call()
                    .entity(ModerationLlmOutput.class);
            return ModerationResultNormalizer.normalize(out, prompt.personalInfoDetected(), prompt.version());
        } catch (Exception e) {
            log.warn("[moderation] 판정 실패 - UNCERTAIN으로 처리: {}", e.getMessage());
            return ModerationResultNormalizer.failed(e.getClass().getSimpleName(), prompt.personalInfoDetected(),
                    prompt.version());
        }
    }
}
