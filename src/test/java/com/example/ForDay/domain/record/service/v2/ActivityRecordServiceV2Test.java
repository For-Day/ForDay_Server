package com.example.ForDay.domain.record.service.v2;

import com.example.ForDay.domain.notification.service.NotificationService;
import com.example.ForDay.domain.record.dto.RecordDetailQueryDto;
import com.example.ForDay.domain.record.dto.request.RecordSearchConditionReqDto;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.type.ContextType;
import com.example.ForDay.domain.record.utils.ActivityRecordUtil;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.util.UserUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityRecordServiceV2Test {

    @InjectMocks
    private ActivityRecordServiceV2 recordService; // 테스트 대상 서비스

    @Mock
    private ActivityRecordRepository activityRecordRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserUtil userUtil;

    @Mock
    private ActivityRecordUtil activityRecordUtil;

    @Test
    @DisplayName("삭제된 기록 조회 시, 예외가 발생하더라도 본인이라면 알림 읽음 처리가 수행되어야 한다")
    void markAsRead_WhenRecordIsDeleted_AndUserIsOwner() {
        // Given
        Long recordId = 1L;
        Long notificationId = 100L;
        String currentUserId = "userId";

        User mockUser = mock(User.class);
        given(mockUser.getId()).willReturn(currentUserId);
        given(mockUser.getRole()).willReturn(Role.USER);
        given(userUtil.getCurrentUser(any())).willReturn(mockUser);

        RecordDetailQueryDto deletedDetail = mock(RecordDetailQueryDto.class);
        given(deletedDetail.writerId()).willReturn(currentUserId);
        given(deletedDetail.recordDeleted()).willReturn(true); // 기록 삭제됨

        given(activityRecordRepository.findDetailDtoById(recordId))
                .willReturn(Optional.of(deletedDetail));

        given(activityRecordUtil.isRecordOwner(currentUserId, currentUserId))
                .willReturn(true);

        RecordSearchConditionReqDto condition = new RecordSearchConditionReqDto(
                ContextType.STORY_ALL,
                null,
                null
        );

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> {
            recordService.getRecordDetailV2(recordId, condition, null, null, notificationId);
        });

        assertEquals(ErrorCode.ACTIVITY_RECORD_NOT_FOUND, exception.getErrorCode());

        // Verify
        verify(notificationService, times(1)).markAsReadIfUnread(notificationId);
    }

}