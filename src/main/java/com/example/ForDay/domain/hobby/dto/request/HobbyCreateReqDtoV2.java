package com.example.ForDay.domain.hobby.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HobbyCreateReqDtoV2 {
    private List<HobbyInfo> hobbyList;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HobbyInfo {
        private Long hobbyInfoId;
        private String hobbyName;
    }
}
