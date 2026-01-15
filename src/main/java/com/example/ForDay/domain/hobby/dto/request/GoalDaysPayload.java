package com.example.ForDay.domain.hobby.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalDaysPayload {

    private Boolean isDurationSet;
}
