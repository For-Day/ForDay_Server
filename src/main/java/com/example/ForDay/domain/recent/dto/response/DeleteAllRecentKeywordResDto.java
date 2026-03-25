package com.example.ForDay.domain.recent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import static com.example.ForDay.global.common.response.message.RecentSuccessMessage.DELETE_ALL_KEYWORD_SUCCESS;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteAllRecentKeywordResDto {
    private String message;

    public static DeleteAllRecentKeywordResDto of() {
        return new DeleteAllRecentKeywordResDto(
                DELETE_ALL_KEYWORD_SUCCESS
        );
    }
}
