package com.example.ForDay.domain.hobby.service.v2;

import com.example.ForDay.domain.hobby.dto.response.MyHobbySettingResDtoV2;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HobbyServiceV2Test {

    @InjectMocks
    private HobbyServiceV2 hobbyServiceV2;

    @Mock
    private HobbyRepository hobbyRepository;

    @Nested
    @DisplayName("내 취미 설정 조회 시 deletable 필드 검증")
    class MyHobbySettingDeletableTest {

        @Test
        @DisplayName("진행 중인 취미가 '1개'라면 progressHobbyList의 deletable은 false여야 한다")
        void myHobbySetting_DeletableIsFalse_WhenProgressHobbyIsOne() {
            // given
            User user = mock(User.class);
            Hobby progressHobby = createMockHobby(1L, "축구", HobbyStatus.IN_PROGRESS, 1);
            Hobby hiddenHobby = createMockHobby(2L, "독서", HobbyStatus.ARCHIVED, null);

            given(hobbyRepository.findAllByUser(user)).willReturn(List.of(progressHobby, hiddenHobby));

            // when
            MyHobbySettingResDtoV2 result = hobbyServiceV2.myHobbySetting(user);

            // then
            assertThat(result.getProgressHobbyList()).hasSize(1);
            assertThat(result.getProgressHobbyList().get(0).isDeletable()).isFalse();

            assertThat(result.getHiddenHobbyList()).hasSize(1);
            assertThat(result.getHiddenHobbyList().get(0).isDeletable()).isTrue();
        }

        @Test
        @DisplayName("진행 중인 취미가 '여러 개(2개 이상)'라면 progressHobbyList의 deletable은 true여야 한다")
        void myHobbySetting_DeletableIsTrue_WhenProgressHobbyIsMultiple() {
            // given
            User user = mock(User.class);

            Hobby progressHobby1 = createMockHobby(1L, "축구", HobbyStatus.IN_PROGRESS, 1);
            Hobby progressHobby2 = createMockHobby(2L, "농구", HobbyStatus.IN_PROGRESS, 2);

            given(hobbyRepository.findAllByUser(user)).willReturn(List.of(progressHobby1, progressHobby2));

            // when
            MyHobbySettingResDtoV2 result = hobbyServiceV2.myHobbySetting(user);

            // then
            assertThat(result.getProgressHobbyList()).hasSize(2);
            assertThat(result.getProgressHobbyList().get(0).isDeletable()).isTrue();
            assertThat(result.getProgressHobbyList().get(1).isDeletable()).isTrue();
        }
    }

    private Hobby createMockHobby(Long id, String name, HobbyStatus status, Integer sequence) {
        Hobby hobby = mock(Hobby.class);
        given(hobby.getId()).willReturn(id);
        given(hobby.getHobbyName()).willReturn(name);
        given(hobby.getStatus()).willReturn(status);
        given(hobby.getSequence()).willReturn(sequence);
        given(hobby.getHobbyInfoId()).willReturn(1L);
        given(hobby.getCreatedAt()).willReturn(LocalDateTime.now());
        return hobby;
    }
}