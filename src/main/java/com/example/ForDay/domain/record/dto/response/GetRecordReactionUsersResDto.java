package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.record.type.RecordReactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "리액션 유저 목록 응답")
public class GetRecordReactionUsersResDto {
    @Schema(description = "리액션 종류", example = "AWESOME")
    private RecordReactionType reactionType;

    @Schema(description = "리액션을 남긴 유저 리스트")
    private List<ReactionUserInfo> reactionUsers;

    @Data
    @AllArgsConstructor
    @Schema(description = "리액션 유저 상세 정보")
    public static class ReactionUserInfo {
        @Schema(description = "유저 고유 ID", example = "75e5f503-667a-40e8-8f90-f592a2022a5d")
        private String userId;

        @Schema(description = "유저 닉네임", example = "유지")
        private String nickname;

        @Schema(description = "프로필 이미지 URL (없으면 null)", example = "https://forday-s3.amazonaws.com/profiles/yuji.png")
        private String profileImageUrl;

        @Schema(description = "리액션 생성 일시 (ISO-8601)", example = "2026-01-23T17:34:53")
        private LocalDateTime reactedAt;
    }
}
