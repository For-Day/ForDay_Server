package com.example.ForDay.global.measure.service;

import com.example.ForDay.domain.notification.repository.NotificationOutboxRepository;
import com.example.ForDay.domain.notification.type.OutboxStatus;
import com.example.ForDay.global.measure.MeasurementRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 단계별 측정 수치를 모아 한 화면으로 만든다. {@code measure} 프로파일 전용.
 *
 * <p>컨트롤러가 리포지토리를 직접 쓰지 않도록 이 서비스를 둔다(ArchUnit S2).
 */
@Service
@Profile("measure")
@RequiredArgsConstructor
public class MeasurementStatsService {

    private final MeasurementRecorder recorder;
    private final NotificationOutboxRepository outboxRepository;

    public Map<String, Object> stats() {
        long pending = outboxRepository.countByStatus(OutboxStatus.PENDING);
        long published = outboxRepository.countByStatus(OutboxStatus.PUBLISHED);

        // 1·2단계는 발송기가 직접 센 값이, 3·4단계는 이번 실행에서 새로 생긴 outbox 행 수가
        // "보내기로 확정된 건수"다. 한 실행은 둘 중 한 경로만 쓰므로 더해도 섞이지 않는다.
        long acceptedDirect = recorder.read(MeasurementRecorder.ACCEPTED_KEY);
        long outboxCreated = (pending + published) - recorder.read(MeasurementRecorder.OUTBOX_BASELINE_KEY);
        long accepted = acceptedDirect + outboxCreated;
        long sent = recorder.read(MeasurementRecorder.SENT_KEY);

        // 중복이 섞이면 총 발송 수로는 유실을 판정할 수 없다. 고유 알림 수를 기준으로 센다.
        long distinctSent = recorder.distinctSentCount();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("발송확정_건수", accepted);
        result.put("실제발송_건수", sent);
        result.put("발송된_고유알림수", distinctSent);
        result.put("유실_건수", accepted - distinctSent);
        result.put("중복발송_건수", recorder.read(MeasurementRecorder.DUPLICATE_KEY));
        result.put("중복_알림ID_샘플", recorder.duplicateIds().stream().limit(20).toList());
        result.put("이번실행_아웃박스_생성행수", outboxCreated);
        result.put("아웃박스_PENDING_전체", pending);
        result.put("아웃박스_PUBLISHED_전체", published);
        return result;
    }

    /** 단계를 넘어갈 때마다 호출한다. 이전 단계 카운터가 남아 있으면 유실·중복이 섞인다. */
    public Map<String, Object> reset() {
        long currentRows = outboxRepository.countByStatus(OutboxStatus.PENDING)
                + outboxRepository.countByStatus(OutboxStatus.PUBLISHED);
        return Map.of("삭제된_키", recorder.reset(currentRows), "아웃박스_기준선", currentRows);
    }
}
