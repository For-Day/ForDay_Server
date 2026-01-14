package com.example.ForDay.domain.hobby.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HobbyTimePayload implements HobbyUpdatePayload {

    @NotNull(message = "시간(minutes)은 필수입니다.")
    private Integer minutes;
}
