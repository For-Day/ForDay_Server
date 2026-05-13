package com.example.ForDay.domain.hobby.dto.request;

import com.example.ForDay.domain.hobby.type.HobbyStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateMyHobbySettingReqDtoV2 {
    @Valid
    @NotEmpty(message = "진행 중인 취미는 최소 1개 이상 설정해야 합니다.")
    private List<ProgressUpdateInfo> progressHobbyList;

    @Valid
    private List<HiddenUpdateInfo> hiddenHobbyList;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProgressUpdateInfo {
        @NotNull
        private Long hobbyId;
        @NotNull
        private Integer sequence;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HiddenUpdateInfo {
        @NotNull
        private Long hobbyId;
        @NotNull
        private Integer sequence;
    }
}
