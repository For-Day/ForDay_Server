package com.example.ForDay.domain.record.dto.request;

import com.example.ForDay.domain.record.type.ContextType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record RecordSearchConditionReqDto(

        @Schema(
                description = "조회 컨텍스트 (필수)\n" +
                        "- `STORY_ALL`: 소식 전체 조회\n" +
                        "- `STORY_HOBBY`: 소식 취미별 조회 (hobbyIds 필수)\n" +
                        "- `USER_FEED`: 유저 피드 조회 (userId, hobbyIds 선택)\n" +
                        "- `USER_SCRAP`: 유저 스크랩 목록 조회 (userId 선택)",
                example = "STORY_ALL",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull ContextType context,

        @Schema(
                description = "피드 또는 스크랩 조회 시 대상 유저의 ID (미입력 시 본인 기준)\n" +
                        "* 사용 가능 컨텍스트: `USER_FEED`, `USER_SCRAP`",
                example = "user123"
        )
        String userId,

        @Schema(
                description = "검색 키워드\n" +
                        "* 사용 가능 컨텍스트: `STORY_ALL`, `STORY_HOBBY`",
                example = "러닝"
        )
        String keyword
) {
}