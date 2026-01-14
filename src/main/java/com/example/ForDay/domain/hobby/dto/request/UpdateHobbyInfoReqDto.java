package com.example.ForDay.domain.hobby.dto.request;

import com.example.ForDay.domain.hobby.type.HobbyUpdateType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateHobbyInfoReqDto {

    @NotNull(message = "수정 타입(type)은 필수입니다.")
    private HobbyUpdateType type;

    @NotNull(message = "수정 데이터(payload)는 필수입니다.")
    @Valid
    private HobbyUpdatePayload payload;
}
