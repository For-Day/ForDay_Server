package com.example.ForDay.global.ai.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiOthersActivityResult {
    private List<OtherActivityDto> otherActivities;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtherActivityDto {
        private Long id;
        private String content;
    }
}
