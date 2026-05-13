package com.example.ForDay.domain.hobby.service.v1;

import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.hobby.utils.HobbyUtil;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HobbyServiceTest {

    @InjectMocks
    private HobbyService hobbyService;

    @Mock
    private HobbyRepository hobbyRepository;

    @Mock
    private HobbyUtil hobbyUtil;

    @Mock
    private ActivityRecordRepository activityRecordRepository;

    @Test
    @DisplayName("진행 중인 취미가 1개 이하일 때 삭제를 시도하면 예외가 발생한다")
    void deleteHobby_ThrowsException_WhenLastProgressHobby() {
        // given
        Long hobbyId = 1L;
        User user = mock(User.class);
        Hobby hobby = mock(Hobby.class);

        given(hobbyUtil.getHobbyByUserId(hobbyId, user)).willReturn(hobby);

        given(hobby.isProgressed()).willReturn(true);
        given(hobbyRepository.countByUserAndStatus(user, HobbyStatus.IN_PROGRESS)).willReturn(1L);

        // when & then
        assertThatThrownBy(() -> hobbyService.deleteHobby(hobbyId, user))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.MINIMUM_PROGRESS_HOBBY_REQUIRED);

        verify(hobby, never()).deleteHobby();
        verify(hobbyRepository, never()).saveAndFlush(any());
        verify(activityRecordRepository, never()).bulkDeleteByHobby(any());
    }
}