package com.example.ForDay.domain.recent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.example.ForDay.global.common.response.message.RecentSuccessMessage.DELETE_KEYWORD_SUCCESS;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteRecentKeywordResDto {
    private String message;
    private Long recentId;

    public static DeleteRecentKeywordResDto of(Long recentId) {
        return new DeleteRecentKeywordResDto(
                DELETE_KEYWORD_SUCCESS,
                recentId
        );
    }
}
