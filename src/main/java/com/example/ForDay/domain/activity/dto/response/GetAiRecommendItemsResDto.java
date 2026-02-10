package com.example.ForDay.domain.activity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetAiRecommendItemsResDto {
    private MessageDto message;
    private List<ItemDto> activityItems;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MessageDto {

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ItemDto {
        private Long itemId;
        private Long hobbyId;
        private String hobbyName;
        private String content;
        private String description;
    }
}
