package com.example.ForDay.domain.hobby.dto;

/**
 * 기록 기반 커버 변경(Tx1)에서 계산한 결과를 트랜잭션 밖(외부 I/O)과 Tx2로 넘기기 위한 DTO.
 *
 * <p>{@code hasSourceImage}가 true면 기록에 실제 이미지가 있어 S3 copy + Lambda 리사이즈 생성이
 * 필요한 경우이고(이때만 {@code srcKey}/{@code newCoverKey}/{@code resizedCoverKey}가 채워진다),
 * false면 스티커 기본 커버로 대체되는 경우라 외부 I/O 없이 바로 Tx2로 넘어간다.
 */
public record CoverPreparation(
        Long hobbyId,
        String oldCoverUrl,
        String newCoverUrl,
        String srcKey,
        String newCoverKey,
        String resizedCoverKey,
        boolean hasSourceImage
) {

    public static CoverPreparation fromRecordImage(
            Long hobbyId, String oldCoverUrl, String newCoverUrl,
            String srcKey, String newCoverKey, String resizedCoverKey
    ) {
        return new CoverPreparation(hobbyId, oldCoverUrl, newCoverUrl, srcKey, newCoverKey, resizedCoverKey, true);
    }

    public static CoverPreparation fromStickerDefault(Long hobbyId, String oldCoverUrl, String stickerCoverUrl) {
        return new CoverPreparation(hobbyId, oldCoverUrl, stickerCoverUrl, null, null, null, false);
    }
}
