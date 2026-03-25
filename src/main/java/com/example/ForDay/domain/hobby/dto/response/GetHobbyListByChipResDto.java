package com.example.ForDay.domain.hobby.dto.response;

import com.example.ForDay.domain.hobby.entity.Hobby;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetHobbyListByChipResDto {
    private List<HobbyInfoByChip> hobbyInfoList;

    public static GetHobbyListByChipResDto from(List<HobbyInfoByChip> hobbyInfoList) {
        return new GetHobbyListByChipResDto(hobbyInfoList);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HobbyInfoByChip {
        private Long hobbyId;
        private String hobbyName;
        private boolean todayRecorded; // 오늘 기록이 되었는지 여부

        public static HobbyInfoByChip of(Hobby hobby, boolean todayRecorded) {
            return new HobbyInfoByChip(
                    hobby.getId(),
                    hobby.getHobbyName(),
                    todayRecorded
            );
        }
    }

}
