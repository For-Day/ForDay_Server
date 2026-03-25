package com.example.ForDay.domain.record.dto.response;

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
public class ReactionTabScrollResDto {
    private Map<String, ReactionSliceDto> tabs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReactionSliceDto {
        private List<ReactionSummaryResDto.ReactionUserDto> users;
        private Long lastReactionId;
        private boolean hasNext;
    }
}
