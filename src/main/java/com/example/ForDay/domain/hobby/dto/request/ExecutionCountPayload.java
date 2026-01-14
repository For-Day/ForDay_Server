package com.example.ForDay.domain.hobby.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExecutionCountPayload implements HobbyUpdatePayload{

    @Min(value = 1, message = "실행횟수는 최소 1 이상이어야 합니다.")
    @Max(value = 7, message = "실행횟수는 최대 7 이하만 가능합니다.")
    private Integer executionCount;
}
