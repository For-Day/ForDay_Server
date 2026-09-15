package com.example.ForDay.domain.hobby.service;

import com.example.ForDay.domain.hobby.dto.CoverChangeResult;
import com.example.ForDay.domain.hobby.dto.CoverPreparation;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.port.ImageLifecyclePort;
import com.example.ForDay.global.port.ImageUrlPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HobbyCoverTransactionSupportTest {

    @Mock
    private ActivityRecordRepository activityRecordRepository;

    @Mock
    private HobbyRepository hobbyRepository;

    @Mock
    private ImageUrlPort imageUrlPort;

    @Mock
    private ImageLifecyclePort imageLifecyclePort;

    @InjectMocks
    private HobbyCoverTransactionSupport transactionSupport;

    private final User owner = User.builder().id("owner-1").build();
    private final User stranger = User.builder().id("stranger-1").build();

    @Nested
    @DisplayName("prepare (Tx1) — 권한 검증 + 키 계산")
    class PrepareTest {

        @Test
        @DisplayName("기록이 없으면 ACTIVITY_RECORD_NOT_FOUND 예외를 던진다")
        void throwsWhenRecordNotFound() {
            given(activityRecordRepository.findByIdWithHobby(10L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> transactionSupport.prepare(10L, owner))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACTIVITY_RECORD_NOT_FOUND);
        }

        @Test
        @DisplayName("기록 작성자가 아니면 NOT_ACTIVITY_RECORD_OWNER 예외를 던진다")
        void throwsWhenNotOwner() {
            Hobby hobby = Hobby.builder().id(1L).user(owner).coverImageUrl("old-url").build();
            ActivityRecord record = ActivityRecord.builder().id(10L).hobby(hobby).user(owner).imageUrl(null).build();
            given(activityRecordRepository.findByIdWithHobby(10L)).willReturn(Optional.of(record));

            assertThatThrownBy(() -> transactionSupport.prepare(10L, stranger))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ACTIVITY_RECORD_OWNER);
        }

        @Test
        @DisplayName("기록에 이미지가 있으면 activity_record/temp -> cover_image/temp, temp -> resized/thumb 경로로 키를 계산한다")
        void computesKeysFromRecordImage() {
            Hobby hobby = Hobby.builder().id(1L).user(owner).coverImageUrl("old-cover-url").build();
            ActivityRecord record = ActivityRecord.builder()
                    .id(10L).hobby(hobby).user(owner)
                    .imageUrl("https://cdn.example.com/activity_record/temp/abc.jpg")
                    .build();
            given(activityRecordRepository.findByIdWithHobby(10L)).willReturn(Optional.of(record));
            given(imageUrlPort.extractKeyFromFileUrl("https://cdn.example.com/activity_record/temp/abc.jpg"))
                    .willReturn("activity_record/temp/abc.jpg");
            given(imageUrlPort.createFileUrl("cover_image/temp/abc.jpg"))
                    .willReturn("https://cdn.example.com/cover_image/temp/abc.jpg");

            CoverPreparation prep = transactionSupport.prepare(10L, owner);

            assertThat(prep.hasSourceImage()).isTrue();
            assertThat(prep.hobbyId()).isEqualTo(1L);
            assertThat(prep.oldCoverUrl()).isEqualTo("old-cover-url");
            assertThat(prep.srcKey()).isEqualTo("activity_record/temp/abc.jpg");
            assertThat(prep.newCoverKey()).isEqualTo("cover_image/temp/abc.jpg");
            assertThat(prep.resizedCoverKey()).isEqualTo("cover_image/resized/thumb/abc.jpg");
            assertThat(prep.newCoverUrl()).isEqualTo("https://cdn.example.com/cover_image/temp/abc.jpg");
        }

        @Test
        @DisplayName("기록에 이미지가 없으면 외부 I/O 없이 스티커 기본 커버로 대체한다")
        void fallsBackToStickerDefault_whenNoRecordImage() {
            Hobby hobby = Hobby.builder().id(1L).user(owner).coverImageUrl("old-cover-url").build();
            ActivityRecord record = ActivityRecord.builder()
                    .id(10L).hobby(hobby).user(owner).imageUrl(null).sticker("sports_sticker").build();
            given(activityRecordRepository.findByIdWithHobby(10L)).willReturn(Optional.of(record));

            CoverPreparation prep = transactionSupport.prepare(10L, owner);

            assertThat(prep.hasSourceImage()).isFalse();
            assertThat(prep.srcKey()).isNull();
            assertThat(prep.newCoverKey()).isNull();
            assertThat(prep.resizedCoverKey()).isNull();
        }
    }

    @Nested
    @DisplayName("applyChange (Tx2) — 실제 반영 + 구 커버 삭제 예약")
    class ApplyChangeTest {

        @Test
        @DisplayName("취미가 없으면 HOBBY_NOT_FOUND 예외를 던진다")
        void throwsWhenHobbyNotFound() {
            given(hobbyRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> transactionSupport.applyChange(1L, "old-url", "new-url"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.HOBBY_NOT_FOUND);
        }

        @Test
        @DisplayName("Tx1이 아니라 Tx2 시점에 구 커버 삭제를 예약하고, 커버를 갱신한다")
        void deletesOldCoverAfterCommit_andUpdatesCover() {
            Hobby hobby = Hobby.builder().id(1L).user(owner).coverImageUrl("old-url").build();
            given(hobbyRepository.findById(1L)).willReturn(Optional.of(hobby));

            CoverChangeResult result = transactionSupport.applyChange(1L, "old-url", "new-url");

            // deleteAfterCommit은 applyChange(Tx2) 안에서 호출돼야, 그 안의 TransactionSynchronization이
            // Tx2 커밋을 기준으로 등록된다 (Tx1에서 미리 호출하면 Tx1 커밋 시점에 지워져 버린다)
            verify(imageLifecyclePort).deleteAfterCommit("old-url");
            assertThat(hobby.getCoverImageUrl()).isEqualTo("new-url");
            assertThat(result.unchanged()).isFalse();
            assertThat(result.updatedCoverUrl()).isEqualTo("new-url");
        }
    }
}
