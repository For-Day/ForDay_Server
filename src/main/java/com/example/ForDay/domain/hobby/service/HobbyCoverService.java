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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 취미 커버 이미지 변경을 담당한다. {@link com.example.ForDay.domain.hobby.service.v1.HobbyService}에서 분리했다.
 * <p>
 * 기록 기반 변경({@link #changeFromRecord})은 S3 copy + Lambda 동기 invoke라는 느린 외부 I/O를
 * 거치므로, 이 외부 I/O를 트랜잭션 경계 밖으로 빼기 위해 3단계로 나눈다.
 *
 * <pre>
 * [Tx1 readOnly] 권한 검증 + srcKey/dstKey 계산   → 커넥션 즉시 반납
 *       ↓
 * [트랜잭션 밖]  S3 copy → Lambda Invoke
 *       ↓
 * [Tx2 write]   updateCoverImage + 구 커버 삭제 예약
 * </pre>
 * <p>
 * 이 메서드 자체는 {@code @Transactional}이 아니다. Tx1/Tx2는 {@link HobbyCoverTransactionSupport}
 * (다른 빈)에 있으므로, 그 경계 밖에서 이 메서드가 외부 I/O를 실행하는 동안 DB 커넥션을 점유하지 않는다.
 * Lambda 호출이 실패하면 예외가 그대로 전파되어 Tx2에 진입하지 않으므로, 실패 시 동작은 기존과
 * 동일하게 "커버 미변경"이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HobbyCoverService {

    private final HobbyUtil hobbyUtil;
    private final ImageLifecyclePort imageLifecyclePort;
    private final CoverGeneratorPort coverGeneratorPort;
    private final HobbyCoverTransactionSupport transactionSupport;

    public CoverChangeResult changeCover(SetHobbyCoverImageReqDto reqDto, User currentUser) throws Exception {
        if (isDirectUploadCase(reqDto)) {
            return changeFromDirectUpload(reqDto.getHobbyId(), reqDto.getCoverImageUrl(), currentUser);
        } else if (isRecordCase(reqDto)) {
            return changeFromRecord(reqDto.getRecordId(), currentUser);
        }
        throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }

    /**
     * Case 1: 직접 업로드된 이미지 URL로 설정. 외부 I/O가 존재 확인(HEAD 조회) 정도로 가벼워
     * 트랜잭션을 나누지 않는다.
     */
    @Transactional
    public CoverChangeResult changeFromDirectUpload(Long hobbyId, String newUrl, User currentUser) {
        Hobby hobby = hobbyUtil.getHobby(hobbyId);
        hobbyUtil.verifyHobbyOwner(hobby, currentUser);

        String oldUrl = hobby.getCoverImageUrl();
        if (Objects.equals(oldUrl, newUrl)) {
            return CoverChangeResult.unchanged(hobby.getId(), oldUrl);
        }

        imageLifecyclePort.validateExists(newUrl);
        imageLifecyclePort.deleteAfterCommit(oldUrl);
        hobby.updateCoverImage(newUrl);

        return CoverChangeResult.changed(hobby.getId(), newUrl);
    }

    /**
     * Case 2: 기존 활동 기록의 사진(또는 스티커 기본 이미지)으로 설정.
     * <p>
     * copy()로 만든 사본은 트랜잭션 밖에 있어 DB 롤백에 연동되지 않는다(#359). 그래서 이후
     * 단계(Lambda 리사이즈 생성, Tx2 DB 반영)가 실패하면 그 자리에서 직접 보상 삭제한다.
     */
    public CoverChangeResult changeFromRecord(Long recordId, User currentUser) throws Exception {
        CoverPreparation prep = transactionSupport.prepare(recordId, currentUser);

        if (prep.hasSourceImage()) {
            imageLifecyclePort.copy(prep.srcKey(), prep.newCoverKey());
            try {
                coverGeneratorPort.generateCover(prep.newCoverKey(), prep.resizedCoverKey());
            } catch (Exception e) {
                // Lambda 실패 - 리사이즈본은 아직 없으므로 원본 사본만 고아로 남는다.
                imageLifecyclePort.deleteOrphanCopy(prep.newCoverKey());
                throw e;
            }
        }

        try {
            return transactionSupport.applyChange(prep.hobbyId(), prep.oldCoverUrl(), prep.newCoverUrl());
        } catch (RuntimeException e) {
            // Tx2 실패 - 여기까지 왔다면 원본 사본과 리사이즈본이 둘 다 만들어진 상태라 둘 다 지운다.
            if (prep.hasSourceImage()) {
                imageLifecyclePort.deleteOrphanCopy(prep.newCoverKey());
                imageLifecyclePort.deleteOrphanCopy(prep.resizedCoverKey());
            }
            throw e;
        }
    }

    private boolean isDirectUploadCase(SetHobbyCoverImageReqDto reqDto) {
        return reqDto.getHobbyId() != null && StringUtils.hasText(reqDto.getCoverImageUrl());
    }

    private boolean isRecordCase(SetHobbyCoverImageReqDto reqDto) {
        return reqDto.getRecordId() != null;
    }
}
