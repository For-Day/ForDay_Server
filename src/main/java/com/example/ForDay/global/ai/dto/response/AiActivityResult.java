package com.example.ForDay.global.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiActivityResult {
    private List<ActivityCard> activities;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ActivityCard {
        private String topic;
        private String content;
        private String description;
    }
}
