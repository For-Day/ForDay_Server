package com.example.ForDay.domain.record.dto.request;

import com.example.ForDay.domain.record.type.ContextType;
import jakarta.validation.constraints.NotNull;

public record RecordSearchConditionReqDto(
        @NotNull ContextType context,
        String userId,
        String keyword
) {
}