package com.example.ForDay.domain.record.service.v3;

import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionRepository;
import com.example.ForDay.domain.record.dto.ReactionSummary;
import com.example.ForDay.domain.record.dto.RecordDetailQueryDto;
import com.example.ForDay.domain.record.dto.response.GetRecordDetailResDtoV3;
import com.example.ForDay.domain.record.entity.RecordImage;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordScrapRepository;
import com.example.ForDay.domain.record.repository.RecordImageRepository;
import com.example.ForDay.domain.record.utils.ActivityRecordUtil;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.infra.s3.util.S3Util;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityRecordServiceV3 {

    private final ActivityRecordRepository activityRecordRepository;
    private final ActivityRecordReactionRepository recordReactionRepository;
    private final ActivityRecordScrapRepository activityRecordScrapRepository;
    private final RecordImageRepository recordImageRepository;
    private final ActivityRecordUtil activityRecordUtil;
    private final UserUtil userUtil;
    private final S3Util s3Util;

    @Transactional(readOnly = true)
    public GetRecordDetailResDtoV3 getRecordDetail(Long recordId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        RecordDetailQueryDto detail = activityRecordRepository.findDetailDtoById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        if (detail.recordDeleted()) throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);

        boolean isRecordOwner = activityRecordUtil.isRecordOwner(currentUser.getId(), detail.writerId());

        if (!isRecordOwner) {
            activityRecordUtil.validateAccess(currentUser.getId(), detail.writerId(), detail.writerDeleted(), detail.visibility());
        }

        List<ReactionSummary> summaries = recordReactionRepository.findOptimizedSummaries(recordId, currentUser.getId());
        boolean scraped = activityRecordScrapRepository.existsByScrap(detail.recordId(), currentUser.getId());
        List<RecordImage> images = recordImageRepository.findAllByActivityRecordIdOrderByImageOrderAsc(recordId);

        return GetRecordDetailResDtoV3.of(
                detail,
                isRecordOwner,
                scraped,
                summaries,
                images,
                s3Util.toProfileMainResizedUrl(detail.writerProfileImageUrl()),
                currentUser.getId()
        );
    }
}
