package com.example.ForDay.domain.hobby.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetHobbyListByChipResDto {
    private List<HobbyInfoByChip> hobbyInfoList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HobbyInfoByChip {
        private Long hobbyId;
        private String hobbyName;
        private boolean todayRecorded; // 오늘 기록이 되었는지 여부
    }

}
