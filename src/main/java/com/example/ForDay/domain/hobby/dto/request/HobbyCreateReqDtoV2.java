package com.example.ForDay.domain.hobby.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HobbyCreateReqDtoV2 {
    @NotEmpty(message = "취미 리스트는 비어있을 수 없습니다.")
    @Valid
    private List<HobbyInfo> hobbyList;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HobbyInfo {
        private Long hobbyInfoId;

        @NotBlank(message = "취미 이름은 필수 입력 값입니다.")
        @Size(max = 20, message = "취미 이름은 20자 이내여야 합니다.")
        private String hobbyName;
    }
}
