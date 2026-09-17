package com.example.ForDay.domain.record.controller.v1;

import com.example.ForDay.domain.reaction.service.QueueOnlyReactionMeasurementService;
import com.example.ForDay.domain.reaction.service.ReactionService;
import com.example.ForDay.domain.record.dto.request.ReactToRecordReqDto;
import com.example.ForDay.domain.record.dto.response.ReactToRecordResDto;
import com.example.ForDay.global.oauth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code measure} 프로파일 전용 반응(reaction) 측정 엔드포인트 모음. 빈이 이 프로파일에서만
 * 등록되므로, 프로덕션(`blue`/`green`)과 로컬·테스트에서는 이 경로들 자체가 존재하지 않아
 * 404가 난다.
 *
 * <p>이름이 {@code Test}로 시작해 ArchUnit S3(컨트롤러는 Docs 인터페이스를 구현한다) 대상에서
 * 영구 제외된다({@code docs/architecture-rules.md} §4 참고) — 디버그·측정용 컨트롤러는
 * Swagger 문서화 대상이 아니다.
 */
@RestController
@Profile("measure")
@RequiredArgsConstructor
@RequestMapping("/records")
public class TestReactionMeasurementController {
    private final ReactionService reactionService;
    private final QueueOnlyReactionMeasurementService queueOnlyReactionMeasurementService;

    /**
     * 동기 알림 발송({@link ReactionService#testReactToRecord})을 호출하기 위한 측정 전용
     * 엔드포인트. #370/#371 응답시간 비교 실험에서 쓰인다.
     */
    @PostMapping("/{recordId}/reaction/test")
    public ReactToRecordResDto testReactToRecord(@PathVariable(name = "recordId") Long recordId,
                                                   @RequestBody ReactToRecordReqDto reqDto,
                                                   @AuthenticationPrincipal CustomUserDetails user) {
        return reactionService.testReactToRecord(recordId, reqDto.getReactionType(), user);
    }

    /**
     * #375 4단계 재측정의 3단계(Redis Write-Back 큐만, 분산 락 없음) 전용 엔드포인트.
     * {@link QueueOnlyReactionMeasurementService} 참고.
     */
    @PostMapping("/{recordId}/reaction/measure/queue-only")
    public ReactToRecordResDto reactToRecordQueueOnly(@PathVariable(name = "recordId") Long recordId,
                                                        @RequestBody ReactToRecordReqDto reqDto,
                                                        @AuthenticationPrincipal CustomUserDetails user) {
        return queueOnlyReactionMeasurementService.reactToRecordQueueOnly(recordId, reqDto.getReactionType(), user);
    }
}
