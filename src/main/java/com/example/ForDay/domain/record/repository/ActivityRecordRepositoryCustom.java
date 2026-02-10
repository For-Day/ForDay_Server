package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.hobby.dto.response.GetStickerInfoResDto;
import com.example.ForDay.domain.record.dto.ActivityRecordWithUserDto;
import com.example.ForDay.domain.record.dto.RecordDetailQueryDto;
import com.example.ForDay.domain.record.dto.ReportActivityRecordDto;
import com.example.ForDay.domain.record.dto.response.GetActivityRecordByStoryResDto;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.record.type.StoryFilterType;
import com.example.ForDay.domain.user.dto.response.GetUserFeedListResDto;
import com.example.ForDay.domain.user.entity.User;

import java.util.List;
import java.util.Optional;

public interface ActivityRecordRepositoryCustom {
    List<GetStickerInfoResDto.StickerDto> getStickerInfo(Long hobbyId, Integer page, Integer size, User currentUser);

    List<GetUserFeedListResDto.FeedDto> findUserFeedList(List<Long> hobbyIds, Long lastRecordId, Integer feedSize, String userId, List<RecordVisibility> visibilities, List<Long> reportedRecordIds, String currentUserId);

    Optional<RecordDetailQueryDto> findDetailDtoById(Long recordId);

    Long countRecordByHobbyIds(List<Long> hobbyIds, String userId);

    Optional<ActivityRecordWithUserDto>  getActivityRecordWithUser(Long recordId);

    Optional<ReportActivityRecordDto> getReportActivityRecord(Long recordId);

    List<GetActivityRecordByStoryResDto.RecordDto> getActivityRecordByStory(Long hobbyInfoId, Long lastRecordId, Integer size, String keyword, String currentUserId, List<String> myFriendIds, List<String> blockFriendIds, List<Long> reportedRecordIds, StoryFilterType storyFilterType);
}
