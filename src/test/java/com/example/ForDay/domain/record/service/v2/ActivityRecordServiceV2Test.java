package com.example.ForDay.domain.record.service.v2;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.notification.service.NotificationService;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionRepository;
import com.example.ForDay.domain.reaction.service.ReactionRankingService;
import com.example.ForDay.domain.reaction.service.ReactionRedisLockService;
import com.example.ForDay.domain.record.dto.RecordDetailQueryDto;
import com.example.ForDay.domain.record.dto.request.ActivityRecordReqDtoV2;
import com.example.ForDay.domain.record.dto.request.RecordSearchConditionReqDto;
import com.example.ForDay.domain.record.dto.response.ActivityRecordResDto;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.entity.RecordImage;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordScrapRepository;
import com.example.ForDay.domain.record.repository.RecordImageRepository;
import com.example.ForDay.domain.record.type.ContextType;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.record.utils.ActivityRecordUtil;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.infra.s3.util.S3Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActivityRecordServiceV2Test {

    @InjectMocks
    private ActivityRecordServiceV2 recordService;

    @Mock private ActivityRecordRepository activityRecordRepository;
    @Mock private ActivityRecordReactionRepository recordReactionRepository;
    @Mock private ActivityRecordScrapRepository activityRecordScrapRepository;
    @Mock private NotificationService notificationService;
    @Mock private UserUtil userUtil;
    @Mock private ActivityRecordUtil activityRecordUtil;
    @Mock private HobbyRepository hobbyRepository;
    @Mock private ActivityRepository activityRepository;
    @Mock private RecordImageRepository recordImageRepository;
    @Mock private S3Util s3Util;
    @Mock private ReactionRankingService reactionRankingService;
    @Mock private ReactionRedisLockService reactionRedisLockService;

    @Test
    @DisplayName("삭제된 기록 조회 시, 예외가 발생하더라도 본인이라면 알림 읽음 처리가 수행되어야 한다")
    void markAsRead_WhenRecordIsDeleted_AndUserIsOwner() {
        Long recordId = 1L;
        Long notificationId = 100L;
        String currentUserId = "userId";

        User mockUser = mock(User.class);
        given(mockUser.getId()).willReturn(currentUserId);
        given(mockUser.getRole()).willReturn(Role.USER);
        given(userUtil.getCurrentUser(any())).willReturn(mockUser);

        RecordDetailQueryDto deletedDetail = mock(RecordDetailQueryDto.class);
        given(deletedDetail.writerId()).willReturn(currentUserId);
        given(deletedDetail.recordDeleted()).willReturn(true);

        given(activityRecordRepository.findDetailDtoById(recordId))
                .willReturn(Optional.of(deletedDetail));

        given(activityRecordUtil.isRecordOwner(currentUserId, currentUserId))
                .willReturn(true);

        RecordSearchConditionReqDto condition = new RecordSearchConditionReqDto(
                ContextType.STORY_ALL, null, null
        );

        CustomException exception = assertThrows(CustomException.class, () -> {
            recordService.getRecordDetailV2(recordId, condition, null, null, notificationId);
        });

        assertEquals(ErrorCode.ACTIVITY_RECORD_NOT_FOUND, exception.getErrorCode());
        verify(notificationService, times(1)).markAsReadIfUnread(notificationId);
    }

    @Nested
    @DisplayName("activityId가 존재하는 경우 - 기존 Activity로 ActivityRecord 생성")
    class WithExistingActivity {

        private final Long hobbyId = 1L;
        private final Long activityId = 10L;
        private final String userId = "user-1";

        private User mockUser;
        private Hobby mockHobby;
        private Activity mockActivity;
        private ActivityRecordReqDtoV2 reqDto;

        @BeforeEach
        void setUp() {
            mockUser = mock(User.class);
            given(mockUser.getId()).willReturn(userId);
            given(userUtil.getCurrentUser(any())).willReturn(mockUser);

            mockHobby = mock(Hobby.class);
            given(mockHobby.getHobbyName()).willReturn("독서");
            given(hobbyRepository.findByIdAndUserId(hobbyId, userId)).willReturn(Optional.of(mockHobby));

            mockActivity = mock(Activity.class);
            given(mockActivity.getId()).willReturn(activityId);
            given(mockActivity.getContent()).willReturn("책 읽기");
            given(activityRepository.findByIdAndUserId(activityId, userId)).willReturn(Optional.of(mockActivity));

            reqDto = new ActivityRecordReqDtoV2();
            reqDto.setHobbyId(hobbyId);
            reqDto.setActivityId(activityId);
            reqDto.setActivityContent(null);
            reqDto.setVisibility(RecordVisibility.PUBLIC);
            reqDto.setImages(null);
        }

        @Test
        @DisplayName("ActivityRecord에 연결된 activity가 요청한 activityId와 일치한다")
        void recordActivity_SetsCorrectActivity() {
            recordService.recordActivity(reqDto, null);

            ArgumentCaptor<ActivityRecord> captor = ArgumentCaptor.forClass(ActivityRecord.class);
            verify(activityRecordRepository).save(captor.capture());

            assertThat(captor.getValue().getActivity().getId()).isEqualTo(activityId);
        }

        @Test
        @DisplayName("ActivityRecord 저장 시 취미, 유저 정보가 올바르게 설정된다")
        void recordActivity_SetsCorrectHobbyAndUser() {
            recordService.recordActivity(reqDto, null);

            ArgumentCaptor<ActivityRecord> captor = ArgumentCaptor.forClass(ActivityRecord.class);
            verify(activityRecordRepository).save(captor.capture());

            ActivityRecord saved = captor.getValue();
            assertThat(saved.getHobby()).isEqualTo(mockHobby);
            assertThat(saved.getUser()).isEqualTo(mockUser);
            assertThat(saved.getVisibility()).isEqualTo(RecordVisibility.PUBLIC);
        }

        @Test
        @DisplayName("activity.record()가 1회 호출된다 (스티커 카운트 증가)")
        void recordActivity_CallsActivityRecord() {
            recordService.recordActivity(reqDto, null);

            verify(mockActivity, times(1)).record();
        }

        @Test
        @DisplayName("새 Activity를 저장하지 않는다 (기존 Activity 사용)")
        void recordActivity_DoesNotSaveNewActivity() {
            recordService.recordActivity(reqDto, null);

            verify(activityRepository, never()).save(any(Activity.class));
        }

        @Test
        @DisplayName("이미지가 있을 때 RecordImage가 저장되고, imageOrder=1인 이미지가 썸네일이다")
        void recordActivity_WithImages_SavesImagesWithThumbnail() {
            List<ActivityRecordReqDtoV2.ActivityImageReqDto> images = List.of(
                    new ActivityRecordReqDtoV2.ActivityImageReqDto("https://image1.url", 1, 800L, 600L),
                    new ActivityRecordReqDtoV2.ActivityImageReqDto("https://image2.url", 2, 800L, 600L)
            );
            reqDto.setImages(images);

            recordService.recordActivity(reqDto, null);

            ArgumentCaptor<List<RecordImage>> captor = ArgumentCaptor.forClass(List.class);
            verify(recordImageRepository).saveAll(captor.capture());

            List<RecordImage> savedImages = captor.getValue();
            assertThat(savedImages).hasSize(2);
            assertThat(savedImages.get(0).isThumbnail()).isTrue();   // imageOrder=1
            assertThat(savedImages.get(1).isThumbnail()).isFalse();  // imageOrder=2
        }

        @Test
        @DisplayName("이미지가 없을 때 빈 리스트로 saveAll이 호출된다")
        void recordActivity_WithoutImages_SavesEmptyList() {
            recordService.recordActivity(reqDto, null);

            verify(recordImageRepository).saveAll(Collections.emptyList());
        }

        @Test
        @DisplayName("존재하지 않는 activityId이면 ACTIVITY_NOT_FOUND 예외가 발생한다")
        void recordActivity_ActivityNotFound_ThrowsException() {
            given(activityRepository.findByIdAndUserId(activityId, userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> recordService.recordActivity(reqDto, null))
                    .isInstanceOf(CustomException.class)
                    .extracting(ex -> ((CustomException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ACTIVITY_NOT_FOUND);
        }

        @Test
        @DisplayName("존재하지 않는 hobbyId이면 HOBBY_NOT_FOUND 예외가 발생한다")
        void recordActivity_HobbyNotFound_ThrowsException() {
            given(hobbyRepository.findByIdAndUserId(hobbyId, userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> recordService.recordActivity(reqDto, null))
                    .isInstanceOf(CustomException.class)
                    .extracting(ex -> ((CustomException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.HOBBY_NOT_FOUND);
        }

        @Test
        @DisplayName("취미 상태가 기록 불가일 때 hobby.validateCanRecord()에서 예외가 전파된다")
        void recordActivity_InvalidHobbyStatus_ThrowsException() {
            doThrow(new CustomException(ErrorCode.INVALID_HOBBY_STATUS))
                    .when(mockHobby).validateCanRecord();

            assertThatThrownBy(() -> recordService.recordActivity(reqDto, null))
                    .isInstanceOf(CustomException.class)
                    .extracting(ex -> ((CustomException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_HOBBY_STATUS);
        }
    }

    @Nested
    @DisplayName("activityContent가 존재하는 경우 - 새 Activity 생성 후 ActivityRecord 생성")
    class WithNewActivity {

        private final Long hobbyId = 1L;
        private final String activityContent = "새로운 활동";
        private final String userId = "user-1";

        private User mockUser;
        private Hobby mockHobby;
        private Activity savedActivity;
        private ActivityRecordReqDtoV2 reqDto;

        @BeforeEach
        void setUp() {
            mockUser = mock(User.class);
            given(mockUser.getId()).willReturn(userId);
            given(userUtil.getCurrentUser(any())).willReturn(mockUser);

            mockHobby = mock(Hobby.class);
            given(mockHobby.getHobbyName()).willReturn("독서");
            given(hobbyRepository.findByIdAndUserId(hobbyId, userId)).willReturn(Optional.of(mockHobby));

            savedActivity = mock(Activity.class);
            given(savedActivity.getContent()).willReturn(activityContent);
            given(activityRepository.save(any(Activity.class))).willReturn(savedActivity);

            reqDto = new ActivityRecordReqDtoV2();
            reqDto.setHobbyId(hobbyId);
            reqDto.setActivityId(null);
            reqDto.setActivityContent(activityContent);
            reqDto.setVisibility(RecordVisibility.PUBLIC);
            reqDto.setImages(null);
        }

        @Test
        @DisplayName("activityRepository.save()가 1회 호출된다 (새 Activity 생성)")
        void recordActivity_SavesNewActivity() {
            recordService.recordActivity(reqDto, null);

            verify(activityRepository, times(1)).save(any(Activity.class));
        }

        @Test
        @DisplayName("저장되는 Activity의 content가 요청한 activityContent와 일치한다")
        void recordActivity_NewActivity_HasCorrectContent() {
            recordService.recordActivity(reqDto, null);

            ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
            verify(activityRepository).save(captor.capture());

            assertThat(captor.getValue().getContent()).isEqualTo(activityContent);
        }

        @Test
        @DisplayName("저장되는 Activity의 aiRecommended는 false이다")
        void recordActivity_NewActivity_IsNotAiRecommended() {
            recordService.recordActivity(reqDto, null);

            ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
            verify(activityRepository).save(captor.capture());

            assertThat(captor.getValue().isAiRecommended()).isFalse();
        }

        @Test
        @DisplayName("ActivityRecord에 연결된 activity가 새로 저장된 Activity이다")
        void recordActivity_RecordLinkedToNewActivity() {
            recordService.recordActivity(reqDto, null);

            ArgumentCaptor<ActivityRecord> captor = ArgumentCaptor.forClass(ActivityRecord.class);
            verify(activityRecordRepository).save(captor.capture());

            assertThat(captor.getValue().getActivity()).isEqualTo(savedActivity);
        }

        @Test
        @DisplayName("새로 생성된 Activity에 record()가 호출된다")
        void recordActivity_CallsRecordOnNewActivity() {
            recordService.recordActivity(reqDto, null);

            verify(savedActivity, times(1)).record();
        }

        @Test
        @DisplayName("응답 DTO의 activityContent가 요청한 activityContent와 일치한다")
        void recordActivity_ResponseDto_HasCorrectContent() {
            ActivityRecordResDto result = recordService.recordActivity(reqDto, null);

            assertThat(result.getActivityContent()).isEqualTo(activityContent);
        }
    }
}
