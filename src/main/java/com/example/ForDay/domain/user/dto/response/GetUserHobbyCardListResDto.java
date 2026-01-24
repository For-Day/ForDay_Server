package com.example.ForDay.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetUserHobbyCardListResDto {
    private Long lastHobbyCardId;
    private List<HobbyCardDto> hobbyCardList;

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
