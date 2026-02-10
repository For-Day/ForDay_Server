package com.example.ForDay.domain.hobby.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReCheckHobbyInfoResDto {
    private List<HobbyInfoDto> hobbyInfo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HobbyInfoDto {
        private Long hobbyInfoId;
        private String hobbyName;
        private String hobbyDescription;
        private String imageCode;
    }
}
