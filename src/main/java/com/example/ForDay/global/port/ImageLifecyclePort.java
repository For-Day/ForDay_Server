package com.example.ForDay.global.port;

/**
 * 이미지 존재 확인 · 복사 · 삭제.
 */
public interface ImageLifecyclePort {

    /**
     * 이미지가 저장소에 실제로 있는지 확인한다. 없으면 예외를 던진다.
     * 빈 URL은 검사 대상이 아니다.
     */
    void validateExists(String imageUrl);

    void copy(String sourceKey, String destinationKey);

    /**
     * 트랜잭션 커밋 후에 삭제한다. 리사이즈 파생본도 함께 지운다.
     * 롤백되면 삭제하지 않는다.
     */
    void deleteAfterCommit(String imageUrl);
}
