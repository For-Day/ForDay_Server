package com.example.ForDay.domain.record.service;

import com.example.ForDay.domain.activity.utils.ActivityUtil;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.notification.repository.NotificationRepository;
import com.example.ForDay.domain.notification.service.NotificationService;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionRepository;
import com.example.ForDay.domain.recent.service.RecentRedisService;
import com.example.ForDay.domain.record.dto.ReportActivityRecordDto;
import com.example.ForDay.domain.record.dto.request.ReportActivityRecordReqDto;
import com.example.ForDay.domain.record.dto.response.ReportActivityRecordResDto;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.entity.ActivityRecordReport;
import com.example.ForDay.domain.record.repository.ActivityRecordReportRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordScrapRepository;
import com.example.ForDay.domain.record.utils.ActivityRecordUtil;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.global.ai.service.TodayRecordRedisService;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.infra.s3.service.S3Service;
import com.example.ForDay.infra.s3.util.S3Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordReportService {

    private final ActivityRecordRepository activityRecordRepository;
    private final UserUtil userUtil;
    private final ActivityRecordUtil activityRecordUtil;
    private final ActivityRecordReportRepository activityRecordReportRepository;
     private final UserRepository userRepository;

    @Transactional
    public ReportActivityRecordResDto reportActivityRecord(Long recordId, ReportActivityRecordReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        log.info("[reportActivityRecord] 신고 요청 - recordId={}, reporter={}", recordId, currentUser.getId());

        // 기록 조회 + 접근 권한 검증
        ReportActivityRecordDto record = getAccessibleReportRecord(recordId, currentUser);

        // 중복 신고 방지
        validateDuplicateReport(record.getRecordId(), currentUser.getId());

        // 신고 저장
        saveReport(record, currentUser, reqDto.getReason());

        log.info("[reportActivityRecord] 신고 완료 - recordId={}", recordId);

        return ReportActivityRecordResDto.from(record);
    }

    private void validateDuplicateReport(Long recordId, String userId) {
        if (activityRecordReportRepository.existsByReportedRecordIdAndReporterId(recordId, userId)) {
            throw new CustomException(ErrorCode.ALREADY_RECORD_REPORTED);
        }
    }

    private void saveReport(ReportActivityRecordDto record, User reporter, String reason) {
        ActivityRecord recordProxy = activityRecordRepository.getReferenceById(record.getRecordId());
        User reportedUserProxy = userRepository.getReferenceById(record.getWriterId());
        ActivityRecordReport report = ActivityRecordReport.of(reporter, reportedUserProxy, recordProxy, reason);
        activityRecordReportRepository.save(report);
    }

    private ReportActivityRecordDto getAccessibleReportRecord(Long recordId, User user) {
        ReportActivityRecordDto record = activityRecordRepository.getReportActivityRecord(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        activityRecordUtil.validateAccess(user.getId(), record.getWriterId(), record.isWriterDeleted(), record.getVisibility());
        return record;
    }
}
