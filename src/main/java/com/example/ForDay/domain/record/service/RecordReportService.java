package com.example.ForDay.domain.record.service;

import com.example.ForDay.domain.record.dto.ReportActivityRecordDto;
import com.example.ForDay.domain.record.dto.request.ReportActivityRecordReqDto;
import com.example.ForDay.domain.record.dto.response.ReportActivityRecordResDto;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.entity.ActivityRecordReport;
import com.example.ForDay.domain.record.repository.ActivityRecordReportRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.utils.ActivityRecordUtil;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.UserUtil;
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
        ReportActivityRecordDto record = getAccessibleReportRecord(recordId, currentUser);
        validateDuplicateReport(record.getRecordId(), currentUser.getId());
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
