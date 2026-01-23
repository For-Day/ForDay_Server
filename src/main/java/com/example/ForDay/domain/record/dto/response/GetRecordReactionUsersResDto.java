package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.record.type.RecordReactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetRecordReactionUsersResDto {
    private RecordReactionType reactionType;
    private List<ReactionUserInfo> reactionUsers;

    @Data
    @AllArgsConstructor
    public static class ReactionUserInfo {
        private String userId;
        private String nickname;
        private String profileImageUrl;
        private LocalDateTime reactedAt;
    }
}
