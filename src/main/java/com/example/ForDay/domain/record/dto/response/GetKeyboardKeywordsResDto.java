package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.record.entity.KeyboardKeyword;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "키보드 키워드 목록 응답 DTO")
public class GetKeyboardKeywordsResDto {

    @Schema(description = "취미 정보 ID", example = "1")
    private Long hobbyInfoId;

    @Schema(description = "키워드 목록")
    private List<KeywordInfo> keywords;

    public static GetKeyboardKeywordsResDto from(Long hobbyInfoId, List<KeyboardKeyword> keywords) {
        return GetKeyboardKeywordsResDto.builder()
                .hobbyInfoId(hobbyInfoId)
                .keywords(keywords.stream().map(KeywordInfo::from).toList())
                .build();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "키워드 정보")
    public static class KeywordInfo {

        @Schema(description = "키워드 ID", example = "1")
        private Long keywordId;

        @Schema(description = "키워드 내용", example = "오늘도 한 챕터")
        private String keyword;

        public static KeywordInfo from(KeyboardKeyword keyboardKeyword) {
            return KeywordInfo.builder()
                    .keywordId(keyboardKeyword.getId())
                    .keyword(keyboardKeyword.getKeyword())
                    .build();
        }
    }
}
