package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.reaction.entity.ActivityRecordReactionCount;
import com.example.ForDay.domain.record.type.RecordReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionSummaryResDto {
    private Long recordId;

    // 감정 개수 요약
    private ReactionCountDto reactionSummary;

    private Map<String, ReactionSliceDto> tabs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReactionCountDto {

        private Long totalCount;
        private Long awesome;
        private Long great;
        private Long amazing;
        private Long fighting;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReactionSliceDto {
        private List<ReactionUserDto> users;
        private String lastUserId;
        private boolean hasNext;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReactionUserDto {
        private String userId;
        private String nickname;
        private String profileImageUrl;
        private RecordReactionType reactionType;
    }

    public static ReactionCountDto from(ActivityRecordReactionCount entity) {
        return new ReactionCountDto(
                entity.getTotalCount(),
                entity.getAwesomeCount(),
                entity.getGreatCount(),
                entity.getAmazingCount(),
                entity.getFightingCount()
        );
    }

    // 빈 객체를 만드는 정적 메서드도 추가하면 더 좋습니다.
    public static ReactionCountDto empty() {
        return new ReactionCountDto(0L, 0L, 0L, 0L, 0L);
    }
}
