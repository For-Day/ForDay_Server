package com.example.ForDay.domain.activity.dto.response;

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


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ItemDto {
        private Long itemId;
        private String content;
        private String description;
    }
}
