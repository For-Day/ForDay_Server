package com.example.ForDay.global.ai.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiPromptRequest {
    private String systemPrompt;
    private String userPrompt;
}