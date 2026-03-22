package com.example.ForDay.domain.reaction.service;

import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionCountRepository;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionRepository;
import com.example.ForDay.domain.record.dto.response.ReactionTabScrollResDto;
import com.example.ForDay.domain.record.dto.response.ReactionSummaryResDto;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.infra.s3.util.S3Util;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecordReactionService {
    private final ActivityRecordRepository activityRecordRepository;
    private final ActivityRecordReactionRepository activityRecordReactionRepository;
    private final ActivityRecordReactionCountRepository activityRecordReactionCountRepository;
    private final UserUtil userUtil;
    private final S3Util s3Util;

    @Transactional(readOnly = true)
    public ReactionSummaryResDto getReactionSummary(Long recordId, int size, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        ActivityRecord record = getRecord(recordId); // 조회하고자하는 기록 엔티티 조회

        // 감정 갯수 요약 조회 (무한 스크롤 상관없이 전체 조회해야함) -> 이걸 별도의 ReactionCount 테이블 만들어서 관리 그래서 반응 남길 때 lock 거는 로직 필요
        ReactionSummaryResDto.ReactionCountDto reactionCountDto = getReactionCountSummary(record.getId());

        // size에 따른 전체 유저 목록 조회
        Map<String, ReactionSummaryResDto.ReactionSliceDto> tabs = activityRecordReactionRepository.getReactionSummary(recordId, size, currentUser.getId());
        processReactionProfileUrls(tabs);

        return new ReactionSummaryResDto(record.getId(), reactionCountDto, tabs);
    }

    @Transactional(readOnly = true)
    public ReactionTabScrollResDto getReactionTabScroll(Long recordId, RecordReactionType type, Long lastReactionId, int size, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        ActivityRecord record = getRecord(recordId);

        return activityRecordReactionRepository.getReactionTabScroll(
                record.getId(), type, lastReactionId, size, currentUser.getId()
        );
    }

    private void processReactionProfileUrls(Map<String, ReactionSummaryResDto.ReactionSliceDto> tabs) {
        tabs.values().forEach(sliceDto -> {
            if (sliceDto != null && sliceDto.getUsers() != null) {
                sliceDto.getUsers().forEach(userDto -> {
                    userDto.setProfileImageUrl(
                            s3Util.toProfileListResizedUrl(userDto.getProfileImageUrl())
                    );
                });
            }
        });
    }

    private ReactionSummaryResDto.ReactionCountDto getReactionCountSummary(Long recordId) {
        // recordId에 해당하는 ActivityRecordReactionCount 조회
        return activityRecordReactionCountRepository.findById(recordId)
                .map(ReactionSummaryResDto::from)
                .orElseGet(ReactionSummaryResDto::empty);
    }

    private ActivityRecord getRecord(Long recordId) {
        return activityRecordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));
    }
}
