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

    /**
     * {@link #copy}로 만든 임시 사본을, 그 이후 단계(리사이즈 생성·DB 반영 등)가 실패했을 때
     * 트랜잭션과 무관하게 즉시 삭제한다. 실패해도 예외를 던지지 않고 로그만 남긴다 -
     * 보상 삭제 자체의 실패가 원래 실패의 후처리(예외 전파)를 가로막으면 안 되기 때문이다.
     */
    void deleteOrphanCopy(String key);
}
