package com.example.ForDay.domain.hobby.dto.response;


import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.user.type.HobbyInfoImageIcon;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyHobbySettingResDto {
    private HobbyStatus currentHobbyStatus;
    private Long inProgressHobbyCount;
    private Long archivedHobbyCount;
    private List<HobbyDto> hobbies;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HobbyDto {
        private Long hobbyId;
        private String hobbyName;
        private Integer hobbyTimeMinutes;
        private Integer executionCount;
        private Integer goalDays;
        private Long hobbyInfoId;
        private HobbyInfoImageIcon imageCode;

        public HobbyDto(Long hobbyId, String hobbyName, Integer hobbyTimeMinutes, Integer executionCount, Integer goalDays, Long hobbyInfoId) {
            this.hobbyId = hobbyId;
            this.hobbyName = hobbyName;
            this.hobbyTimeMinutes = hobbyTimeMinutes;
            this.executionCount = executionCount;
            this.goalDays = goalDays;
            this.hobbyInfoId = hobbyInfoId;
            this.imageCode = mapImageCode(hobbyInfoId);
        }


        private HobbyInfoImageIcon mapImageCode(Long hobbyInfoId) {
            if (hobbyInfoId == null) return HobbyInfoImageIcon.DEFAULT_ICON;

            return switch (hobbyInfoId.intValue()) {
                case 1 -> HobbyInfoImageIcon.DRAWING_ICON;
                case 2 -> HobbyInfoImageIcon.GYM_ICON;
                case 3 -> HobbyInfoImageIcon.READING_ICON;
                case 4 -> HobbyInfoImageIcon.MUSIC_ICON;
                case 5 -> HobbyInfoImageIcon.RUNNING_ICON;
                case 6 -> HobbyInfoImageIcon.COOKING_ICON;
                case 7 -> HobbyInfoImageIcon.CAFE_ICON;
                case 8 -> HobbyInfoImageIcon.MOVIE_ICON;
                case 9 -> HobbyInfoImageIcon.PHOTO_ICON;
                case 10 -> HobbyInfoImageIcon.WRITING_ICON;

                default -> HobbyInfoImageIcon.DEFAULT_ICON;
            };
        }
    }


}
