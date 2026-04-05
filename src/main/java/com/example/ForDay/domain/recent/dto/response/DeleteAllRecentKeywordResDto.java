package com.example.ForDay.domain.recent.dto.response;

import com.example.ForDay.global.common.response.message.RecentSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.example.ForDay.global.common.response.message.RecentSuccessCode.DELETE_ALL_KEYWORD_SUCCESS;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteAllRecentKeywordResDto {
    private String message;

    public static DeleteAllRecentKeywordResDto of() {
        return new DeleteAllRecentKeywordResDto(
                RecentSuccessCode.DELETE_ALL_KEYWORD_SUCCESS.getMessage()
        );
    }
}
