package com.example.ForDay.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "취미 카드 리스트 응답 DTO")
public class GetUserHobbyCardListResDto {
    @Schema(description = "취미 카드 목록")
    private Long lastHobbyCardId;
    private List<HobbyCardDto> hobbyCardList;
    private boolean hasNext;

    public static GetUserHobbyCardListResDto of(List<HobbyCardDto> cardList, int size) {
        boolean hasNext = cardList.size() > size;
        if (hasNext) cardList.remove(size);

        Long lastId = cardList.isEmpty()
                ? null
                : cardList.get(cardList.size() - 1).getHobbyCardId();

        return new GetUserHobbyCardListResDto(lastId, cardList, hasNext);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HobbyCardDto {
        private Long hobbyCardId;
        private String hobbyCardContent;
        private String imageUrl;
        private LocalDateTime createdAt;
    }
}
