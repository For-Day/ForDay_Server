package com.example.ForDay.domain.hobby.service;

import com.example.ForDay.domain.hobby.dto.CoverChangeResult;
import com.example.ForDay.domain.hobby.dto.CoverPreparation;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.StickerCover;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.port.ImageLifecyclePort;
import com.example.ForDay.global.port.ImageUrlPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

import static com.example.ForDay.global.common.constants.FileStorageConstants.*;

/**
 * 기록 기반 커버 변경(Tx1: 권한 검증/계산, Tx2: 실제 반영)을 담당한다.
 * <p>
 * {@link HobbyCoverService}가 Tx1 → (트랜잭션 밖 외부 I/O) → Tx2 순서로 호출하는데,
 * 같은 클래스 안에서 {@code @Transactional} 메서드를 호출하면 Spring AOP 프록시를
 * 거치지 않아(self-invocation) 트랜잭션이 실제로 분리되지 않는다. 그래서 두 트랜잭션을
 * 별도 빈으로 분리해, HobbyCoverService(다른 빈)가 호출할 때만 프록시를 타고
 * 각각 독립된 트랜잭션으로 실행되도록 한다.
 */
@Component
@RequiredArgsConstructor
class HobbyCoverTransactionSupport {

    private final ActivityRecordRepository activityRecordRepository;
    private final HobbyRepository hobbyRepository;
    private final ImageUrlPort imageUrlPort;
    private final ImageLifecyclePort imageLifecyclePort;

    /**
     * Tx1 (readOnly): 권한 검증 + 외부 I/O에 필요한 키/URL을 계산하고 커넥션을 즉시 반납한다.
     */
    @Transactional(readOnly = true)
    public CoverPreparation prepare(Long recordId, User currentUser) {
        ActivityRecord record = activityRecordRepository.findByIdWithHobby(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        if (!Objects.equals(record.getUser(), currentUser)) {
            throw new CustomException(ErrorCode.NOT_ACTIVITY_RECORD_OWNER);
        }

        Hobby hobby = record.getHobby();
        String oldCoverUrl = hobby.getCoverImageUrl();
        String recordImageUrl = record.getImageUrl();

        if (StringUtils.hasText(recordImageUrl)) {
            String srcKey = imageUrlPort.extractKeyFromFileUrl(recordImageUrl);
            String newCoverKey = srcKey.replace(TEMP_ACTIVITY_PATH, TEMP_COVER_PATH);
            String resizedCoverKey = newCoverKey.replace(TEMP_DIR, THUMB_DIR);
            String newCoverUrl = imageUrlPort.createFileUrl(newCoverKey);

            return CoverPreparation.fromRecordImage(
                    hobby.getId(), oldCoverUrl, newCoverUrl, srcKey, newCoverKey, resizedCoverKey);
        }

        String stickerCoverUrl = StickerCover.getUrlBySticker(record.getSticker());
        return CoverPreparation.fromStickerDefault(hobby.getId(), oldCoverUrl, stickerCoverUrl);
    }

    /**
     * Tx2 (write): 외부 I/O가 끝난 뒤에만 호출된다. 상태 반영 + 구 커버 삭제를 커밋 후로 예약한다.
     * <p>
     * Tx1에서 조회한 {@code Hobby}는 Tx1 종료와 함께 detach되므로, 반드시 여기서 다시 조회해야
     * 변경 감지(dirty checking)가 이 트랜잭션의 영속성 컨텍스트에서 정상 동작한다.
     */
    @Transactional
    public CoverChangeResult applyChange(Long hobbyId, String oldCoverUrl, String newCoverUrl) {
        Hobby hobby = hobbyRepository.findById(hobbyId)
                .orElseThrow(() -> new CustomException(ErrorCode.HOBBY_NOT_FOUND));

        imageLifecyclePort.deleteAfterCommit(oldCoverUrl);
        hobby.updateCoverImage(newCoverUrl);

        return CoverChangeResult.changed(hobby.getId(), newCoverUrl);
    }
}
