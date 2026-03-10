package com.example.ForDay.domain.activity.dto.response;

import com.example.ForDay.domain.activity.entity.ActivityRecommendItem;
import com.example.ForDay.domain.hobby.entity.Hobby;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetAiRecommendItemsResDto {
    private String message;
    private Long hobbyId;
    private String hobbyName;
    private List<ItemDto> activityItems;

    // 정적 팩토리 메서드 구현
    public static GetAiRecommendItemsResDto of(Hobby hobby, List<ActivityRecommendItem> items, String userSummaryText) {
        List<ItemDto> itemDtos = items.stream()
                .map(item -> new ItemDto(
                        item.getId(),
                        item.getContent(),
                        item.getDescription()
                ))
                .toList();

        return new GetAiRecommendItemsResDto(
                userSummaryText,
                hobby.getId(),
                hobby.getHobbyName(),
                itemDtos
        );
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ItemDto {
        private Long itemId;
        private String content;
        private String description;
    }
}