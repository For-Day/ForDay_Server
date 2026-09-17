package com.example.ForDay.domain.hobby.service;

import com.example.ForDay.domain.hobby.dto.CoverChangeResult;
import com.example.ForDay.domain.hobby.dto.CoverPreparation;
import com.example.ForDay.domain.hobby.dto.request.SetHobbyCoverImageReqDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.port.CoverGeneratorPort;
import com.example.ForDay.domain.hobby.utils.HobbyUtil;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.port.ImageLifecyclePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class HobbyCoverServiceTest {

    @Mock
    private HobbyUtil hobbyUtil;

    @Mock
    private ImageLifecyclePort imageLifecyclePort;

    @Mock
    private CoverGeneratorPort coverGeneratorPort;

    @Mock
    private HobbyCoverTransactionSupport transactionSupport;

    private HobbyCoverService hobbyCoverService;

    private final User currentUser = User.builder().id("user-1").build();

    @Nested
    @DisplayName("changeCover 분기")
    class ChangeCoverTest {

        @Test
        @DisplayName("hobbyId와 coverImageUrl이 있으면 직접 업로드 케이스로 위임한다")
        void changeCover_delegatesToDirectUpload() {
            hobbyCoverService = new HobbyCoverService(hobbyUtil, imageLifecyclePort, coverGeneratorPort, transactionSupport);
            SetHobbyCoverImageReqDto reqDto = new SetHobbyCoverImageReqDto(1L, null, "https://example.com/new.jpg");
            Hobby hobby = Hobby.builder().id(1L).user(currentUser).coverImageUrl("https://example.com/old.jpg").build();
            given(hobbyUtil.getHobby(1L)).willReturn(hobby);

            // WHEN
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> hobbyCoverService.changeCover(reqDto, currentUser));

            // THEN: 직접 업로드 경로(hobbyUtil.getHobby)를 탔는지 확인 - record 경로였다면 호출되지 않는다
            verify(hobbyUtil).getHobby(1L);
            verifyNoInteractions(transactionSupport);
        }

        @Test
        @DisplayName("recordId만 있으면 기록 기반 케이스로 위임한다")
        void changeCover_delegatesToFromRecord() throws Exception {
            hobbyCoverService = new HobbyCoverService(hobbyUtil, imageLifecyclePort, coverGeneratorPort, transactionSupport);
            SetHobbyCoverImageReqDto reqDto = new SetHobbyCoverImageReqDto(null, 10L, null);
            CoverPreparation prep = CoverPreparation.fromStickerDefault(1L, "old-url", "sticker-url");
            given(transactionSupport.prepare(10L, currentUser)).willReturn(prep);
            given(transactionSupport.applyChange(1L, "old-url", "sticker-url"))
                    .willReturn(CoverChangeResult.changed(1L, "sticker-url"));

            hobbyCoverService.changeCover(reqDto, currentUser);

            verify(transactionSupport).prepare(10L, currentUser);
            verify(hobbyUtil, never()).getHobby(anyLong());
        }

        @Test
        @DisplayName("hobbyId/coverImageUrl도 recordId도 없으면 예외를 던진다")
        void changeCover_throwsWhenNeitherCaseMatches() {
            hobbyCoverService = new HobbyCoverService(hobbyUtil, imageLifecyclePort, coverGeneratorPort, transactionSupport);
            SetHobbyCoverImageReqDto reqDto = new SetHobbyCoverImageReqDto(null, null, null);

            assertThatThrownBy(() -> hobbyCoverService.changeCover(reqDto, currentUser))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Nested
    @DisplayName("changeFromDirectUpload")
    class ChangeFromDirectUploadTest {

        @Test
        @DisplayName("기존 커버와 동일한 URL이면 아무 것도 바꾸지 않고 unchanged를 반환한다")
        void unchangedWhenSameUrl() {
            hobbyCoverService = new HobbyCoverService(hobbyUtil, imageLifecyclePort, coverGeneratorPort, transactionSupport);
            Hobby hobby = Hobby.builder().id(1L).user(currentUser).coverImageUrl("same-url").build();
            given(hobbyUtil.getHobby(1L)).willReturn(hobby);

            CoverChangeResult result = hobbyCoverService.changeFromDirectUpload(1L, "same-url", currentUser);

            assertThat(result.unchanged()).isTrue();
            verify(imageLifecyclePort, never()).validateExists(anyString());
            verify(imageLifecyclePort, never()).deleteAfterCommit(anyString());
        }

        @Test
        @DisplayName("새 URL이면 존재를 확인하고 구 커버 삭제를 예약한 뒤 커버를 갱신한다")
        void changesWhenDifferentUrl() {
            hobbyCoverService = new HobbyCoverService(hobbyUtil, imageLifecyclePort, coverGeneratorPort, transactionSupport);
            Hobby hobby = Hobby.builder().id(1L).user(currentUser).coverImageUrl("old-url").build();
            given(hobbyUtil.getHobby(1L)).willReturn(hobby);

            CoverChangeResult result = hobbyCoverService.changeFromDirectUpload(1L, "new-url", currentUser);

            assertThat(result.unchanged()).isFalse();
            assertThat(result.updatedCoverUrl()).isEqualTo("new-url");
            assertThat(hobby.getCoverImageUrl()).isEqualTo("new-url");
            verify(imageLifecyclePort).validateExists("new-url");
            verify(imageLifecyclePort).deleteAfterCommit("old-url");
        }
    }

    @Nested
    @DisplayName("changeFromRecord — 외부 I/O를 Tx1과 Tx2 사이(트랜잭션 밖)에서 실행한다")
    class ChangeFromRecordTest {

        @Test
        @DisplayName("기록에 이미지가 있으면 prepare(Tx1) -> S3 copy -> Lambda invoke -> applyChange(Tx2) 순서로 실행한다")
        void callsExternalIoBetweenTx1AndTx2_whenSourceImageExists() throws Exception {
            hobbyCoverService = new HobbyCoverService(hobbyUtil, imageLifecyclePort, coverGeneratorPort, transactionSupport);
            CoverPreparation prep = CoverPreparation.fromRecordImage(
                    1L, "old-url", "new-url", "src-key", "new-cover-key", "resized-key");
            given(transactionSupport.prepare(10L, currentUser)).willReturn(prep);
            given(transactionSupport.applyChange(1L, "old-url", "new-url"))
                    .willReturn(CoverChangeResult.changed(1L, "new-url"));

            hobbyCoverService.changeFromRecord(10L, currentUser);

            InOrder order = inOrder(transactionSupport, imageLifecyclePort, coverGeneratorPort);
            order.verify(transactionSupport).prepare(10L, currentUser);
            order.verify(imageLifecyclePort).copy("src-key", "new-cover-key");
            order.verify(coverGeneratorPort).generateCover("new-cover-key", "resized-key");
            order.verify(transactionSupport).applyChange(1L, "old-url", "new-url");
        }

        @Test
        @DisplayName("기록에 이미지가 없으면(스티커 기본값) 외부 I/O 없이 바로 applyChange(Tx2)만 호출한다")
        void skipsExternalIo_whenNoSourceImage() throws Exception {
            hobbyCoverService = new HobbyCoverService(hobbyUtil, imageLifecyclePort, coverGeneratorPort, transactionSupport);
            CoverPreparation prep = CoverPreparation.fromStickerDefault(1L, "old-url", "sticker-url");
            given(transactionSupport.prepare(10L, currentUser)).willReturn(prep);
            given(transactionSupport.applyChange(1L, "old-url", "sticker-url"))
                    .willReturn(CoverChangeResult.changed(1L, "sticker-url"));

            hobbyCoverService.changeFromRecord(10L, currentUser);

            verify(imageLifecyclePort, never()).copy(anyString(), anyString());
            verify(coverGeneratorPort, never()).generateCover(anyString(), anyString());
            verify(transactionSupport).applyChange(1L, "old-url", "sticker-url");
        }

        @Test
        @DisplayName("Lambda 리사이즈 생성이 실패하면 applyChange(Tx2)는 호출되지 않고, 원본 사본만 보상 삭제한다 (#359)")
        void doesNotApplyChange_whenLambdaGenerationFails() throws Exception {
            hobbyCoverService = new HobbyCoverService(hobbyUtil, imageLifecyclePort, coverGeneratorPort, transactionSupport);
            CoverPreparation prep = CoverPreparation.fromRecordImage(
                    1L, "old-url", "new-url", "src-key", "new-cover-key", "resized-key");
            given(transactionSupport.prepare(10L, currentUser)).willReturn(prep);
            willThrow(new RuntimeException("Lambda timeout"))
                    .given(coverGeneratorPort).generateCover(anyString(), anyString());

            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                    () -> hobbyCoverService.changeFromRecord(10L, currentUser));

            // THEN: Tx2(applyChange)에 진입하지 않으므로 DB의 커버는 그대로 유지된다
            verify(transactionSupport, never()).applyChange(anyLong(), any(), any());
            // THEN: Lambda가 리사이즈본을 만들기 전에 실패했으므로 원본 사본만 고아로 남아 그것만 지운다
            verify(imageLifecyclePort).deleteOrphanCopy("new-cover-key");
            verify(imageLifecyclePort, never()).deleteOrphanCopy("resized-key");
        }

        @Test
        @DisplayName("applyChange(Tx2)가 실패하면 원본 사본과 리사이즈본을 둘 다 보상 삭제한다 (#359)")
        void deletesBothCopies_whenApplyChangeFails() throws Exception {
            hobbyCoverService = new HobbyCoverService(hobbyUtil, imageLifecyclePort, coverGeneratorPort, transactionSupport);
            CoverPreparation prep = CoverPreparation.fromRecordImage(
                    1L, "old-url", "new-url", "src-key", "new-cover-key", "resized-key");
            given(transactionSupport.prepare(10L, currentUser)).willReturn(prep);
            willThrow(new CustomException(ErrorCode.HOBBY_NOT_FOUND))
                    .given(transactionSupport).applyChange(1L, "old-url", "new-url");

            assertThatThrownBy(() -> hobbyCoverService.changeFromRecord(10L, currentUser))
                    .isInstanceOf(CustomException.class);

            // THEN: 이 시점엔 S3 copy와 Lambda 리사이즈 생성 모두 성공한 뒤였으므로 둘 다 고아로 남아 둘 다 지운다
            verify(imageLifecyclePort).deleteOrphanCopy("new-cover-key");
            verify(imageLifecyclePort).deleteOrphanCopy("resized-key");
        }
    }
}
