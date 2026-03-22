package com.example.ForDay.global.util;

import com.example.ForDay.domain.friend.entity.FriendRelation;
import com.example.ForDay.domain.friend.repository.FriendRelationRepository;
import com.example.ForDay.domain.friend.type.FriendRelationStatus;
import com.example.ForDay.domain.record.dto.RecordDetailQueryDto;
import com.example.ForDay.domain.record.dto.ReportActivityRecordDto;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ActivityRecordUtil {
    private final ActivityRecordRepository activityRecordRepository;
    private final FriendRelationRepository friendRelationRepository;

    public ActivityRecord getRecord(Long recordId) {
        return activityRecordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));
    }

    public ActivityRecord getRecordByUserId(Long recordId, User user) {
        return activityRecordRepository.findByIdAndUserId(recordId, user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));
    }

    public void validateAccess(String currentUserId, String writerId, boolean writerDeleted, RecordVisibility visibility) {
        List<FriendRelation> relations = getRelations(currentUserId, writerId);
        checkBlockedAndDeletedUser(relations, writerDeleted);
        validateRecordAuthority(relations, visibility, writerId, currentUserId);
    }

    public List<FriendRelation> getRelations(String currentUserId, String targetUserId) {
        return friendRelationRepository.findAllRelationsBetween(currentUserId, targetUserId);
    }

    public void validateRecordAuthority(List<FriendRelation> relations,
                                         RecordVisibility visibility, String writerId, String me) {
        if (writerId.equals(me)) return;

        switch (visibility) {
            case FRIEND -> {
                boolean isFollowing = relations.stream()
                        .anyMatch(f -> f.getRequester().getId().equals(me)
                                && f.getTargetUser().getId().equals(writerId)
                                && f.getRelationStatus() == FriendRelationStatus.FOLLOW);
                if (!isFollowing) throw new CustomException(ErrorCode.FRIEND_ONLY_ACCESS);
            }
            case PRIVATE -> throw new CustomException(ErrorCode.PRIVATE_RECORD);
            default -> {} // PUBLIC
        }
    }

    public void checkBlockedAndDeletedUser(List<FriendRelation> relations, boolean deleted) {
        boolean isBlocked = relations.stream()
                .anyMatch(f -> f.getRelationStatus() == FriendRelationStatus.BLOCK);
        if (isBlocked || deleted) {
            throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);
        }
    }

    public boolean isRecordOwner(String currentUserId, String writerId) {
        return Objects.equals(currentUserId, writerId);
    }

    public ReportActivityRecordDto getValidRecord(Long recordId) {
        ReportActivityRecordDto record = activityRecordRepository.getReportActivityRecord(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        if (record.isRecordDeleted()) {
            throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);
        }

        return record;
    }
}
