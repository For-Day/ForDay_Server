package com.example.ForDay.domain.record.service;

import com.example.ForDay.domain.friend.FriendRelationRepository;
import com.example.ForDay.domain.record.dto.response.GetRecordDetailResDto;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordReactionRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.TimeUtil;
import com.example.ForDay.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ActivityRecordService {
    private final ActivityRecordRepository activityRecordRepository;
    private final UserUtil userUtil;
    private final FriendRelationRepository friendRelationRepository;
    private final ActivityRecordReactionRepository recordReactionRepository;

    @Transactional(readOnly = true)
    public GetRecordDetailResDto getRecordDetail(Long activityRecordId, CustomUserDetails user) {
        ActivityRecord activityRecord = getActivityRecord(activityRecordId);
        User currentUser = userUtil.getCurrentUser(user);
        User writer = activityRecord.getUser();
        String currentUserId = currentUser.getId();

        // 내가 이 글에 누른 리액션 정보 (UserReactionDto)
        List<RecordReactionType> myReactions = recordReactionRepository.findAllMyReactions(activityRecordId, currentUserId);
        GetRecordDetailResDto.UserReactionDto userReaction = new GetRecordDetailResDto.UserReactionDto(
                myReactions.contains(RecordReactionType.AWESOME),
                myReactions.contains(RecordReactionType.GREAT),
                myReactions.contains(RecordReactionType.AMAZING),
                myReactions.contains(RecordReactionType.FIGHTING)
        );

        GetRecordDetailResDto.NewReactionDto newReaction;

        // 권한 판별 및 New 알림 처리
        if (Objects.equals(currentUserId, writer.getId())) {
            //  읽지 않은 리액션이 있는지 확인
            List<RecordReactionType> unreadTypes = recordReactionRepository.findAllUnreadReactions(activityRecordId);
            newReaction = new GetRecordDetailResDto.NewReactionDto(
                    unreadTypes.contains(RecordReactionType.AWESOME),
                    unreadTypes.contains(RecordReactionType.GREAT),
                    unreadTypes.contains(RecordReactionType.AMAZING),
                    unreadTypes.contains(RecordReactionType.FIGHTING)
            );

            // 확인한 리액션들은 모두 읽음 처리 (Dirty Checking)
            recordReactionRepository.markAsReadByRecordId(activityRecordId);
        } else {
            //  권한 체크
            validateVisibility(activityRecord, writer, currentUser);
            // 타인 조회 시 new 정보는 항상 false
            newReaction = new GetRecordDetailResDto.NewReactionDto(false, false, false, false);
        }

        return GetRecordDetailResDto.builder()
                .activityId(activityRecord.getActivity().getId())
                .activityContent(activityRecord.getActivity().getContent())
                .activityRecordId(activityRecord.getId())
                .imageUrl(activityRecord.getImageUrl())
                .sticker(activityRecord.getSticker())
                .createdAt(TimeUtil.formatLocalDateTime(activityRecord.getCreatedAt()))
                .memo(activityRecord.getMemo())
                .visibility(activityRecord.getVisibility())
                .newReaction(newReaction)
                .userReaction(userReaction)
                .build();
    }

    private void validateVisibility(ActivityRecord record, User writer, User currentUser) {
        switch (record.getVisibility()) {
            case FRIEND -> {
                if (!checkFriendship(writer, currentUser)) {
                    throw new CustomException(ErrorCode.FRIEND_ONLY_ACCESS);
                }
            }
            case PRIVATE -> throw new CustomException(ErrorCode.PRIVATE_RECORD);
            case PUBLIC -> {} // 통과
        }
    }

    private boolean checkFriendship(User writer, User currentUser) {
        if (writer.getId().equals(currentUser.getId())) {
            return true;
        }

        return friendRelationRepository.existsAcceptedFriendship(
                writer.getId(),
                currentUser.getId()
        );
    }

    private ActivityRecord getActivityRecord(Long activityRecordId) {
        return activityRecordRepository.findById(activityRecordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));
    }

}
