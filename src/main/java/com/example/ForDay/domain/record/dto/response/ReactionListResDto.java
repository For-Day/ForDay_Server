package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.record.type.RecordReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionListResDto {

    private List<ReactionUserDto> users;

    private String nextCursor;

    private boolean hasNext;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public class ReactionUserDto {

        private String userId;

        private String nickname;

        private String profileImageUrl;

        private RecordReactionType reactionType;
    }
}
